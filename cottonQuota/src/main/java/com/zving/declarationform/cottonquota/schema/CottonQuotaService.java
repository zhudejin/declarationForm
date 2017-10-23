package com.zving.declarationform.cottonquota.schema;

import java.util.List;

import com.zving.declarationform.cottonquota.model.CottonQuota;

/**
 * @author 王育春
 * @mail wyuch@zving.com
 * @date 2017年10月18日
 */
public interface CottonQuotaService {
    List<CottonQuota> list();

    CottonQuota get(long id);

    String delete(String ids);

    String add(CottonQuota cottonQuota);

    String update(CottonQuota cottonQuota);

    String audit(String ids, String status);

    String check();

}
