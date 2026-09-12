package com.nso.business.project.service;

import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// 全局系统角色与单个项目内职责之间的规范映射。
@Component

// 项目职责目录 定义项目成员职责的标准目录
public class ProjectResponsibilityCatalog {
    private static final Map<String, String> REQUIRED_ROLE = Map.ofEntries(
            Map.entry("SALES", "sales"),
            Map.entry("PROJECT_MANAGER", "project_manager"),
            Map.entry("PROJECT_DEPUTY", "project_manager"),
            Map.entry("TECHNICAL", "technical"),
            Map.entry("TECH_OWNER", "technical"),
            Map.entry("TECH_MEMBER", "technical"),
            Map.entry("PROCESS", "process"),
            Map.entry("PROCESS_OWNER", "process"),
            Map.entry("PROCESS_MEMBER", "process"),
            Map.entry("PURCHASER", "purchaser"),
            Map.entry("PURCHASE_OWNER", "purchaser"),
            Map.entry("PURCHASE_MEMBER", "purchaser"),
            Map.entry("PRODUCTION", "production"),
            Map.entry("PRODUCTION_OWNER", "production"),
            Map.entry("PRODUCTION_MEMBER", "production"),
            Map.entry("QUALITY", "quality"),
            Map.entry("QUALITY_OWNER", "quality"),
            Map.entry("QUALITY_MEMBER", "quality"),
            Map.entry("FIELD_USER", "field_user"),
            Map.entry("COLLABORATOR", "field_user"),
            Map.entry("VIEWER", "field_user"),
            Map.entry("CUSTOMER_CONFIRM", "customer_confirm")
    );

    public List<String> normalize(List<String> requested, String primary) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (requested != null) {
            requested.stream().filter(item -> item != null && !item.isBlank())
                    .map(this::normalizeCode).forEach(values::add);
        }
        if (values.isEmpty() && primary != null && !primary.isBlank()) values.add(normalizeCode(primary));
        if (values.isEmpty()) throw new BusinessException("至少需要指定一个项目职责");
        return List.copyOf(values);
    }

    public String primary(List<String> responsibilities, String requestedPrimary) {
        String primary = requestedPrimary == null || requestedPrimary.isBlank() ? responsibilities.get(0) : normalizeCode(requestedPrimary);
        if (!responsibilities.contains(primary)) throw new BusinessException("主职责必须包含在项目职责中");
        return primary;
    }

    public String normalizeCode(String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        if ("CUSTOMER".equals(code)) code = "CUSTOMER_CONFIRM";
        if (!REQUIRED_ROLE.containsKey(code)) throw new BusinessException("不支持的项目职责：" + value);
        return code;
    }

    public boolean isAllowedForRoles(String responsibility, List<String> roleCodes, boolean administrator) {
        if (administrator) return true;
        String required = REQUIRED_ROLE.get(normalizeCode(responsibility));
        return roleCodes != null && roleCodes.stream().anyMatch(role -> required.equalsIgnoreCase(role));
    }

    public List<String> allowedResponsibilities(List<String> roleCodes, boolean administrator, boolean external) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : REQUIRED_ROLE.entrySet()) {
            if (external && !"CUSTOMER_CONFIRM".equals(entry.getKey())) continue;
            if (!external && "CUSTOMER_CONFIRM".equals(entry.getKey())) continue;
            if (administrator || (roleCodes != null && roleCodes.stream().anyMatch(role -> entry.getValue().equalsIgnoreCase(role)))) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        return List.copyOf(values.keySet());
    }

    public String requiredSystemRole(String responsibility) {
        return REQUIRED_ROLE.get(normalizeCode(responsibility));
    }

    public boolean isOwner(String responsibility) {
        return Set.of("PROJECT_MANAGER", "TECH_OWNER", "PROCESS_OWNER", "PURCHASE_OWNER", "PRODUCTION_OWNER", "QUALITY_OWNER")
                .contains(normalizeCode(responsibility));
    }

    public Set<String> supported() {
        return REQUIRED_ROLE.keySet();
    }
}
