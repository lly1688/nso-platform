package com.nso.web.controller.system.tool;

import java.util.Map;

import com.nso.common.core.domain.AjaxResult;
import com.nso.generator.service.CodePreviewService;

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
public class GenController {
    private final CodePreviewService codePreviewService;

    public GenController(CodePreviewService codePreviewService) {
        this.codePreviewService = codePreviewService;
    }

    @GetMapping("/gen/tables")
    public AjaxResult<?> tables() {
        var tables = codePreviewService.tables();
        return AjaxResult.success(Map.of("list", tables, "total", tables.size(), "pageNo", 1, "pageSize", tables.size()));
    }

    @PostMapping("/gen/import")
    public AjaxResult<?> importTable(@RequestParam String tableName) {
        return AjaxResult.success(codePreviewService.importTable(tableName));
    }

    @GetMapping("/gen/{id}/preview")
    public AjaxResult<?> preview(@PathVariable Long id) {
        return AjaxResult.success(codePreviewService.preview(id));
    }

    @GetMapping("/gen/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] content = codePreviewService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nso-generator-" + id + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(content);
    }
}
