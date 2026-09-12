package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/system/dicts", "/api/v1/admin/system/dict"})
@PreAuthorize("hasRole('ADMIN')")

// 字典管理接口 负责系统字典数据的维护
public class SysDictController {
    // JDBC模板
    private final JdbcTemplate jdbc;

    public SysDictController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 查询字典数据。
    @GetMapping
    public AjaxResult<?> list(@RequestParam(required = false) String type,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        PageQuery page = PageSupport.normalize(new PageQuery(pageNo, pageSize));
        String where = type == null || type.isBlank() ? "" : " WHERE dict_type = ?";
        Object[] filterArgs = type == null || type.isBlank() ? new Object[0] : new Object[]{type.trim()};
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dict_data" + where, Long.class, filterArgs);
        List<Object> args = new java.util.ArrayList<>();
        if (filterArgs.length > 0) args.add(filterArgs[0]);
        args.add(page.pageSizeValue());
        args.add(page.offset());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, dict_type, dict_code, dict_label, sort_no FROM sys_dict_data"
                + where + " ORDER BY dict_type, sort_no, id LIMIT ? OFFSET ?", args.toArray());
        return AjaxResult.success(new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue()));
    }

    // 保存字典数据。
    @PostMapping
    public AjaxResult<?> save(@RequestBody DictRequest request) {
        if (request == null || blank(request.type()) || blank(request.code()) || blank(request.label())) {
            throw new BusinessException("字典类型、编码和名称不能为空");
        }
        String type = request.type().trim();
        String code = request.code().trim();
        String label = request.label().trim();
        int sortNo = request.sortNo() == null ? 0 : request.sortNo();
        jdbc.update("INSERT INTO sys_dict_data (dict_type, dict_code, dict_label, sort_no) VALUES (?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE dict_label=VALUES(dict_label), sort_no=VALUES(sort_no)",
                type, code, label, sortNo);
        return AjaxResult.success(Map.of("type", type, "code", code, "label", label, "sortNo", sortNo));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record DictRequest(
        // 字典类型
        String type,
        // 字典编码
        String code,
        // 字典标签
        String label,
        // 排序号
        Integer sortNo
    ) {

    }
}
