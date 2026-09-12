package com.nso.web.controller.system.tool;

import java.util.Map;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.PageSupport;
import com.nso.scaffold.service.CodePreviewService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/admin/tool")
@PreAuthorize("hasRole('ADMIN')")

// 代码生成接口 负责代码生成器的操作
public class GenController {
    // 编码Preview服务
    private final CodePreviewService codePreviewService;

    public GenController(CodePreviewService codePreviewService) {
        this.codePreviewService = codePreviewService;
    }

    // 查询可生成代码的数据表。
    @GetMapping("/gen/tables")
    public AjaxResult<?> tables(@RequestParam(required = false) Integer pageNo,
                                @RequestParam(required = false) Integer pageSize) {
        var tables = codePreviewService.tables();
        return AjaxResult.success(PageSupport.slice(tables, new PageQuery(pageNo, pageSize)));
    }

    // 导入数据表生成配置。
    @PostMapping("/gen/import")
    public AjaxResult<?> importTable(@RequestParam String tableName) {
        return AjaxResult.success(codePreviewService.importTable(tableName));
    }

    // 预览生成代码。
    @GetMapping("/gen/{id}/preview")
    public AjaxResult<?> preview(@PathVariable Long id) {
        return AjaxResult.success(codePreviewService.preview(id));
    }

    // 下载生成代码。
    @GetMapping("/gen/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] content = codePreviewService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nso-scaffold-" + id + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(content);
    }
}
