package com.nso.framework.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.nso.business.core.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class TenantMybatisConfiguration {
    private static final Set<String> GLOBAL_TABLES = Set.of("nso_tenant", "flyway_schema_history");

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new LongValue(TenantContext.tenantId()); }
            @Override public String getTenantIdColumn() { return "tenant_id"; }
            @Override public boolean ignoreTable(String tableName) {
                return GLOBAL_TABLES.contains(tableName) || tableName.startsWith("sys_") || tableName.startsWith("gen_");
            }
        }));
        // Every mutable business aggregate carries @Version.  Without this
        // interceptor MyBatis-Plus emits an unresolved version parameter on
        // updateById, preventing project, document, sample, change and task
        // state transitions from completing.
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
