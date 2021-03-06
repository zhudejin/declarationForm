package com.zving.declarationform.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 将数据存储到Mysql
 * 
 * @author 王育春
 * @mail wyuch@zving.com
 * @date 2017年10月19日
 */
public class MysqlStorage implements IStorage {
	ReentrantLock lock = new ReentrantLock();
	static final String DriverName = "com.mysql.jdbc.Driver";
	Connection conn;

	public MysqlStorage() {
		String address = System.getenv("mysql_address");
		String user = System.getenv("mysql_user");
		String password = System.getenv("mysql_password");

		if (address == null) {
			address = System.getProperty("mysql_address");
		}
		if (user == null) {
			user = System.getProperty("mysql_user");
		}
		if (password == null) {
			password = System.getProperty("mysql_password");
		}

		try {
			Class.forName(DriverName);
			Connection conn = DriverManager.getConnection("jdbc:mysql://" + address + "?useUnicode=true&characterEncoding=utf8", user,
					password);
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.execute("SET NAMES 'utf8'");
			stmt.close();

			// 初始化schema
			String sql = "select * from  information_schema.schemata where schema_name='declaration_form'";
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			if (!rs.next()) {
				stmt.close();
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				stmt.execute("create database declaration_form charset utf8;");
			}
			stmt.close();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.execute("use declaration_form;");
			stmt.close();

			sql = "select * from  information_schema.tables where table_schema='declaration_form' and table_name='classdata'";
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery(sql);
			if (!rs.next()) {
				stmt.close();
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				stmt.execute("create table classdata(classtype varchar(100),jsondata mediumtext)");
			}
			stmt.close();
			this.conn = conn;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	<T> List<T> getList(Class<T> clazz) {
		ObjectMapper om = new ObjectMapper();
		JavaType type = om.getTypeFactory().constructParametricType(ArrayList.class, clazz);
		try {
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select jsondata from classdata where classtype='" + clazz.getName() + "'");
			String json = null;
			if (rs.next()) {
				json = rs.getString(1);
			}
			stmt.close();

			if (json != null) {
				List<T> list = om.readValue(json, type);
				return list;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<T> list = new ArrayList<T>();
		return list;
	}

	<T> void write(Class<T> clazz, List<T> list) {
		ObjectMapper om = new ObjectMapper();
		try {
			Statement stmt1 = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt1.execute("delete from classdata where classtype='" + clazz.getName() + "'");
			stmt1.close();

			String json = om.writeValueAsString(list);
			PreparedStatement stmt = conn.prepareStatement("insert into classdata values (?,?)", ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, clazz.getName());
			stmt.setString(2, json);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public <T> T get(Class<T> clazz, T bean) {
		List<T> list = find(clazz, bean);
		return get(clazz, list, bean);
	}

	public <T> T get(Class<T> clazz, List<T> list, T bean) {
		list = find(clazz, list, bean);
		if (list != null && list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	@Override
	public <T> List<T> find(Class<T> clazz, T bean) {
		List<T> list = getList(clazz);
		return find(clazz, list, bean);
	}

	public <T> List<T> find(Class<T> clazz, List<T> list, T bean) {
		if (bean == null) {
			return list;
		}
		BeanMap map1 = new BeanMap(bean);
		bean.getClass().getDeclaredMethods();
		return list.stream().filter(item -> {
			BeanMap map2 = new BeanMap(item);
			boolean flag = true;
			for (Entry<?, ?> e : map1.entrySet()) {
				if (e.getValue() == null || !StorageUtil.isBasicType(e.getValue().getClass())) {
					continue;
				} else {
					if (e.getValue() instanceof Long || e.getValue() instanceof Integer || e.getValue() instanceof Float
							|| e.getValue() instanceof Double) {
						if (((Number) e.getValue()).doubleValue() == 0) {
							continue;
						}
					}
					if (!e.getValue().equals(map2.get(e.getKey()))) {
						flag = false;
						break;
					}
				}
			}
			return flag;
		}).collect(Collectors.toList());
	}

	@Override
	public <T> void add(Class<T> clazz, T bean) {
		List<T> list = getList(clazz);
		list.add(bean);
		write(clazz, list);
	}

	@Override
	public <T> void update(Class<T> clazz, T bean, T newBean) {
		List<T> list = getList(clazz);
		T dest = get(clazz, list, bean);
		try {
			BeanUtils.copyProperties(dest, newBean);
		} catch (Exception e) {
			e.printStackTrace();
		}
		write(clazz, list);
	}

	@Override
	public <T> void delete(Class<T> clazz, T bean) {
		List<T> list = getList(clazz);
		List<T> list2 = find(clazz, list, bean);
		list.removeAll(list2);
		write(clazz, list);
	}
}
