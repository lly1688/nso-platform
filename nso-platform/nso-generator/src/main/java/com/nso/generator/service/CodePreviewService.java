package com.nso.generator.service;

import com.nso.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CodePreviewService {
    private static final Map<Long, TableDefinition> ALLOWED_TABLES = Map.of(
            1L, new TableDefinition(1L, "nso_project", "非标订单项目", "business", "project", "NsoProject"),
            2L, new TableDefinition(2L, "nso_change_order", "变更单", "business", "change", "NsoChange"),
            3L, new TableDefinition(3L, "nso_sample", "样品单", "business", "sample", "NsoSample"));

    public List<TableDefinition> tables() {
        return ALLOWED_TABLES.values().stream().sorted(java.util.Comparator.comparing(TableDefinition::id)).toList();
    }

    public TableDefinition importTable(String tableName) {
        return ALLOWED_TABLES.values().stream()
                .filter(table -> table.tableName().equals(tableName))
                .findFirst()
                .orElseThrow(() -> BusinessException.accessDenied("GEN_TABLE_DENIED", "代码生成仅允许访问授权业务表", tableName,
                        "nso_project,nso_change_order,nso_sample", "选择生成器列表中的业务表"));
    }

    public Map<String, String> preview(Long id) {
        TableDefinition table = requireTable(id);
        String packageName = "com.nso.generated." + table.moduleName() + "." + table.businessName();
        Map<String, String> files = new LinkedHashMap<>();
        files.put("entity/" + table.className() + ".java", "package " + packageName + ";\n\npublic class " + table.className() + " {\n    private Long id;\n}\n");
        files.put("mapper/" + table.className() + "Mapper.java", "package " + packageName + ";\n\npublic interface " + table.className() + "Mapper {\n}\n");
        files.put("service/I" + table.className() + "Service.java", "package " + packageName + ";\n\npublic interface I" + table.className() + "Service {\n}\n");
        files.put("controller/" + table.className() + "Controller.java", "package " + packageName + ";\n\npublic class " + table.className() + "Controller {\n}\n");
        files.put("vue/" + table.businessName() + "/index.vue", "<template><main>" + table.tableComment() + "</main></template>\n");
        return files;
    }

    public byte[] download(Long id) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
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
            throw BusinessException.accessDenied("GEN_TABLE_DENIED", "代码生成配置不存在或未授权", String.valueOf(id),
                    "授权业务表配置", "从生成器列表重新选择表");
        }
        return table;
    }

    public record TableDefinition(Long id, String tableName, String tableComment, String moduleName, String businessName,
                                  String className) { }
}
