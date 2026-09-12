package com.nso.framework.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.nso.business.core.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

// 多租户 MyBatis 配置。
@Configuration
public class TenantMybatisConfiguration {

    // 不参与租户条件拼接的全局表
    private static final Set<String> GLOBAL_TABLES = Set.of("nso_tenant", "flyway_schema_history");

    // 注册租户隔离、乐观锁和分页拦截器。
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.tenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return GLOBAL_TABLES.contains(tableName) || tableName.startsWith("sys_") || tableName.startsWith("gen_");
            }
        }));
        // 可变业务聚合使用 @Version，更新时必须注册乐观锁拦截器。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页必须位于租户过滤之后，确保 COUNT 和 LIMIT 基于租户查询生成。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
