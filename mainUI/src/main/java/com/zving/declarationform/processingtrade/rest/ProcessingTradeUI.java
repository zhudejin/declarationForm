package com.zving.declarationform.processingtrade.rest;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.zving.declarationform.base.BaseUI;
import com.zving.declarationform.dto.ResponseDTO;
import com.zving.declarationform.model.ProcessingTrade;

import io.servicecomb.provider.rest.common.RestSchema;
import io.servicecomb.provider.springmvc.reference.RestTemplateBuilder;

/**
 * @author zhaochangjin
 * @mail zhaochangjin@zving.com
 * @date 2017年10月20日
 */
@Controller
@RestSchema(schemaId = "processingTradeUI")
@RequestMapping(path = "api/", produces = MediaType.APPLICATION_JSON)
public class ProcessingTradeUI extends BaseUI {

	@RequestMapping(path = "processingtrade", method = RequestMethod.POST)
	public @ResponseBody ResponseDTO add(@RequestBody ProcessingTrade processingTrade) {
		RestTemplate restTemplate = RestTemplateBuilder.create();
		restTemplate.postForObject("cse://processingTrade/api/processingtrade", processingTrade, ProcessingTrade.class);
		return success("添加成功");
	}

	@RequestMapping(path = "processingtrade", method = RequestMethod.PUT)
	public @ResponseBody ResponseDTO update(@RequestBody ProcessingTrade processingTrade) {
		RestTemplate restTemplate = RestTemplateBuilder.create();
		restTemplate.put("cse://processingTrade/api/processingtrade", processingTrade);
		return success("更新成功");
	}

	@RequestMapping(path = "processingtrade/{number}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseDTO delete(@PathVariable String number) {
		RestTemplate restTemplate = RestTemplateBuilder.create();
		restTemplate.delete("cse://processingTrade/api/processingtrade/{number}", number);
		return success("删除成功");
	}

	@RequestMapping(path = "processingtrade/{number}", method = RequestMethod.GET)
	public @ResponseBody ResponseDTO get(@PathVariable String number) {
		RestTemplate restTemplate = RestTemplateBuilder.create();
		ProcessingTrade processingTrade = restTemplate.getForObject("cse://processingTrade/api/processingtrade/{number}",
				ProcessingTrade.class, number);
		return success("执行成功", processingTrade);
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(path = "processingtrade/{pageIndex}/{pageSize}", method = RequestMethod.GET)
	public @ResponseBody ResponseDTO list(@PathVariable String pageIndex, @PathVariable String pageSize) {
		RestTemplate restTemplate = RestTemplateBuilder.create();
		List<ProcessingTrade> list = restTemplate.getForObject("cse://processingTrade/api/processingtrade/{pageIndex}/{pageSize}",
				List.class, pageIndex, pageSize);
		return success("执行成功", list);
	}

}