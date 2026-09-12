package com.nso.scaffold.service;

import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// 代码生成预览服务。
@Service
public class CodePreviewService {
    private static final Map<Long, TableDefinition> ALLOWED_TABLES = Map.of(
            1L, new TableDefinition(1L, "nso_project", "非标订单项目", "business", "project", "NsoProject"),
            2L, new TableDefinition(2L, "nso_change_order", "变更单", "business", "change", "NsoChange"),
            3L, new TableDefinition(3L, "nso_sample", "样品单", "business", "sample", "NsoSample"));

    // 查询允许生成代码的业务表。
    public List<TableDefinition> tables() {
        return ALLOWED_TABLES.values().stream()
                .sorted(java.util.Comparator.comparing(TableDefinition::id))
                .toList();
    }

    // 按表名导入已授权的业务表配置。
    public TableDefinition importTable(String tableName) {
        return ALLOWED_TABLES.values().stream()
                .filter(table -> table.tableName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> BusinessException.accessDenied(
                        "GEN_TABLE_DENIED",
                        "代码生成仅允许访问授权业务表",
                        tableName,
                        "nso_project,nso_change_order,nso_sample",
                        "请从代码生成器列表选择授权业务表"));
    }

    // 生成指定业务表的代码预览。
    public Map<String, String> preview(Long id) {
        TableDefinition table = requireTable(id);
        String packageName = "com.nso.generated." + table.moduleName() + "." + table.businessName();
        Map<String, String> files = new LinkedHashMap<>();
        files.put("entity/" + table.className() + ".java",
                "package " + packageName + ";\n\npublic class " + table.className()
                        + " {\n    private Long id;\n}\n");
        files.put("mapper/" + table.className() + "Mapper.java",
                "package " + packageName + ";\n\n/**\n * " + table.tableComment()
                        + "数据访问接口。\n */\npublic interface "
                        + table.className() + "Mapper {\n}\n");
        files.put("service/I" + table.className() + "Service.java",
                "package " + packageName + ";\n\n/**\n * " + table.tableComment()
                        + "服务接口。\n */\npublic interface I"
                        + table.className() + "Service {\n}\n");
        files.put("controller/" + table.className() + "Controller.java",
                "package " + packageName + ";\n\npublic class " + table.className() + "Controller {\n}\n");
        files.put("vue/" + table.businessName() + "/index.vue",
                "<template><main>" + table.tableComment() + "</main></template>\n");
        return files;
    }

    // 将代码预览打包为 ZIP 文件。
    public byte[] download(Long id) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> file : preview(id).entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("cannot package generated code", ex);
        }
    }

    private TableDefinition requireTable(Long id) {
        TableDefinition table = ALLOWED_TABLES.get(id);
        if (table == null) {
            throw BusinessException.accessDenied(
                    "GEN_TABLE_DENIED",
                    "代码生成配置不存在或未授权",
                    String.valueOf(id),
                    "授权业务表配置",
                    "请从生成器列表重新选择");
        }
        return table;
    }

    public record TableDefinition(
        // 配置编号
        Long id,
        // 数据库表名
        String tableName,
        // 表说明
        String tableComment,
        // 模块名
        String moduleName,
        // 业务名
        String businessName,
        // Java 类名
        String className
    ) {
    }
}
