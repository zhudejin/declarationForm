package com.zving.declarationform.processingtrade.schema;

import java.util.List;

import com.zving.declarationform.dto.ResponseDTO;
import com.zving.declarationform.model.DeclarationForm;
import com.zving.declarationform.processingtrade.model.ProcessingTrade;

/**
 * @author zhaochangjin
 * @mail zhaochangjin@zving.com
 * @date 2017年10月20日
 */
public interface ProcessingTradeService {

	String add(ProcessingTrade processingTrade);

	String update(ProcessingTrade processingTrade);

	String delete(String ids);

	ProcessingTrade get(long id);

	List<ProcessingTrade> list();

	String check(DeclarationForm form);

	ResponseDTO tccTry(DeclarationForm form);

	ResponseDTO tccConfirm(DeclarationForm form);

	ResponseDTO tccCancel(DeclarationForm form);

}
