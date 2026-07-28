package com.nso.web.controller.system;

import com.nso.common.core.domain.AjaxResult;
import com.nso.common.exception.BusinessException;
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
public class SysDictController {
    private final JdbcTemplate jdbc;

    public SysDictController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public AjaxResult<?> list(@RequestParam(required = false) String type) {
        String sql = "SELECT id, dict_type, dict_code, dict_label, sort_no FROM sys_dict_data "
                + (type == null || type.isBlank() ? "" : "WHERE dict_type = ? ")
                + "ORDER BY dict_type, sort_no, id";
        List<Map<String, Object>> rows = type == null || type.isBlank()
                ? jdbc.queryForList(sql)
                : jdbc.queryForList(sql, type.trim());
        return AjaxResult.success(rows);
    }

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

    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record DictRequest(String type, String code, String label, Integer sortNo) { }
}
