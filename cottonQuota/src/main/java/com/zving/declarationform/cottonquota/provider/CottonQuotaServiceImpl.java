package com.zving.declarationform.cottonquota.provider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.zving.declarationform.PATCH;
import com.zving.declarationform.cottonquota.model.CottonQuota;
import com.zving.declarationform.cottonquota.schema.CottonQuotaService;
import com.zving.declarationform.dto.ResponseDTO;
import com.zving.declarationform.model.DeclarationForm;
import com.zving.declarationform.model.PackingItem;
import com.zving.declarationform.model.TCCLock;
import com.zving.declarationform.storage.IStorage;
import com.zving.declarationform.storage.StorageUtil;

import io.servicecomb.provider.rest.common.RestSchema;

@RestSchema(schemaId = "cottonQuota")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)

public class CottonQuotaServiceImpl implements CottonQuotaService {

	IStorage storage = StorageUtil.getInstance();

	private long getId() {
		return (long) ((Math.random() * 100000) + 1);
	}

	@Override
	@Path("cottonQuota")
	@POST

	public String add(CottonQuota cottonQuota) {
		cottonQuota.setNumber(String.valueOf(new Date().getTime()));
		cottonQuota.setAuditStatus("");
		cottonQuota.setId(getId());
		cottonQuota.setAddTime(new Date());
		cottonQuota.setAddUser("demo");
		storage.add(CottonQuota.class, cottonQuota);
		return "添加成功";
	}

	@Override
	@Path("cottonQuota")
	@PUT

	public String update(CottonQuota cottonQuota) {
		try {
			CottonQuota cottonQuotaOrigin = getCottonQuota(cottonQuota.getId());
			CottonQuota cottonQuotaNew = new CottonQuota();
			BeanUtils.copyProperties(cottonQuotaNew, cottonQuotaOrigin);
			cottonQuotaNew.setQuota(cottonQuota.getQuota());
			cottonQuotaNew.setUsed(cottonQuota.getUsed());
			cottonQuotaNew.setApplication(cottonQuota.getApplication());
			cottonQuotaNew.setModifyTime(new Date());
			cottonQuotaNew.setModifyUser("demo");
			storage.update(CottonQuota.class, cottonQuotaOrigin, cottonQuotaNew);
			return "更新成功";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "更新失败";
	}

	@Override
	@Path("cottonQuota/{ids}")
	@DELETE

	public String delete(@PathParam("ids") String ids) {
		String[] idArray = ids.split(",");
		for (String id : idArray) {
			if (NumberUtils.isNumber(id)) {
				CottonQuota cottonQuota = getCottonQuota(Long.valueOf(id));
				storage.delete(CottonQuota.class, cottonQuota);
			}
		}
		return "删除成功";
	}

	@Override
	@Path("cottonQuota/{id}")
	@GET

	public CottonQuota get(@PathParam("id") long id) {
		return getCottonQuota(id);
	}

	@Override
	@Path("cottonQuota")
	@GET

	public List<CottonQuota> list() {
		return storage.find(CottonQuota.class, null);
	}

	@Override
	@Path("cottonQuota/audit/{ids}/{status}")
	@PATCH

	public String audit(@PathParam("ids") String ids, @PathParam("status") String status) {
		String[] idArray = ids.split(",");
		try {
			for (String id : idArray) {
				if (NumberUtils.isNumber(id)) {
					CottonQuota cottonQuotaOrigin = getCottonQuota(Long.valueOf(id));
					CottonQuota cottonQuota = new CottonQuota();
					BeanUtils.copyProperties(cottonQuota, cottonQuotaOrigin);
					cottonQuota.setAuditStatus(status);
					cottonQuota.setModifyTime(new Date());
					cottonQuota.setModifyUser("demo");
					storage.update(CottonQuota.class, cottonQuotaOrigin, cottonQuota);
				}
			}
			return "审核成功";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "审核失败";
	}

	@Override
	@Path("cottonQuota/check")
	@POST

	public String check(DeclarationForm form) {
		try {
			for (PackingItem item : form.getPackingList()) {
				if (item.getName().contains("棉花")) {
					CottonQuota quota = new CottonQuota();
					quota.setCompanyId(form.getCompanyId());
					quota = StorageUtil.getInstance().get(CottonQuota.class, quota);
					if (item.getAmount() > quota.getQuota() - quota.getUsed()) {
						return InetAddress.getLocalHost().getHostName() + ":棉花配额检查失败：可用额度不足";
					}
				}
			}
			return InetAddress.getLocalHost().getHostName() + ":棉花配额检查通过";
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	private CottonQuota getCottonQuota(long id) {
		List<CottonQuota> list = storage.find(CottonQuota.class, null);
		for (CottonQuota cottonQuota : list) {
			if (cottonQuota.getId() == id) {
				return cottonQuota;
			}
		}
		return null;
	}

	@Override
	@Path("try")
	@POST

	public ResponseDTO tccTry(DeclarationForm form) {
		for (PackingItem item : form.getPackingList()) {
			if (item.getName().contains("棉花")) {
				CottonQuota old = new CottonQuota();
				old.setCompanyId(form.getCompanyId());
				CottonQuota quota = StorageUtil.getInstance().get(CottonQuota.class, old);
				if (quota == null) {
					return new ResponseDTO("棉花配额锁定失败，没有棉花配额");
				}
				if (item.getAmount() > quota.getQuota() - quota.getUsed()) {
					return new ResponseDTO("棉花配额锁定失败，棉花配额不足");
				}
				quota.setUsed(quota.getUsed() + item.getAmount());
				StorageUtil.getInstance().update(CottonQuota.class, old, quota);

				// 记录资源锁定
				TCCLock lock = new TCCLock();
				lock.setType("CottonQuota");
				lock.setRelaId(item.getId() + "");
				lock.setLockedValue(item.getAmount() + "");
				lock.setFormId(form.getId());
				StorageUtil.getInstance().add(TCCLock.class, lock);
			}
		}
		return new ResponseDTO("");
	}

	@Override
	@Path("confirm")
	@POST

	public ResponseDTO tccConfirm(DeclarationForm form) {
		for (PackingItem item : form.getPackingList()) {
			if (item.getName().contains("棉花")) {
				// 删除资源锁定
				TCCLock lock = new TCCLock();
				lock.setType("CottonQuota");
				lock.setRelaId(item.getId() + "");
				lock.setFormId(form.getId());
				lock = StorageUtil.getInstance().get(TCCLock.class, lock);
				if (lock == null) {
					continue;
				}
				StorageUtil.getInstance().delete(TCCLock.class, lock);
			}
		}
		return new ResponseDTO("");
	}

	@Override
	@Path("cancel")
	@POST

	public ResponseDTO tccCancel(DeclarationForm form) {
		for (PackingItem item : form.getPackingList()) {
			if (item.getName().contains("棉花")) {
				CottonQuota old = new CottonQuota();
				old.setCompanyId(form.getCompanyId());
				CottonQuota quota = StorageUtil.getInstance().get(CottonQuota.class, old);

				if (quota == null) {
					continue;
				}

				// 记录资源锁定
				TCCLock lock = new TCCLock();
				lock.setType("CottonQuota");
				lock.setRelaId(item.getId() + "");
				lock.setFormId(form.getId());
				lock = StorageUtil.getInstance().get(TCCLock.class, lock);
				if (lock == null) {
					continue;
				}

				quota.setUsed(quota.getUsed() - Double.parseDouble(lock.getLockedValue()));
				StorageUtil.getInstance().update(CottonQuota.class, old, quota);
				StorageUtil.getInstance().delete(TCCLock.class, lock);
			}
		}
		return new ResponseDTO("");
	}
}
