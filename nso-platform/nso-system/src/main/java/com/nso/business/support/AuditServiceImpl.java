package com.nso.business.support;
import com.nso.business.core.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service public class AuditServiceImpl implements IAuditService { private final JdbcTemplate jdbc; public AuditServiceImpl(JdbcTemplate jdbc){this.jdbc=jdbc;} @Override public void record(String userName,String clientType,String module,String operation,String requestUri,String requestMethod,String result,String summary,String traceId){jdbc.update("INSERT INTO nso_audit_log (tenant_id,user_id,user_name,client_type,module_name,operation_type,after_summary,request_uri,request_method,trace_id,result) VALUES (?,?,?,?,?,?,?,?,?,?,?)",TenantContext.tenantId(),TenantContext.userId(),userName,clientType,module,operation,summary,requestUri,requestMethod,traceId,result);} }
