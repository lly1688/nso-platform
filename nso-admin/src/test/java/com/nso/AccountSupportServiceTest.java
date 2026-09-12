package com.nso;

import com.nso.business.core.TenantContext;
import com.nso.business.file.ObjectStoragePort;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.message.service.IMessageService;
import com.nso.business.support.BusinessNumberService;
import com.nso.framework.security.FrameworkPasswordCredentialAdapter;
import com.nso.system.security.PasswordCredentialPort;
import com.nso.system.security.PublicRequestRateLimitPort;
import com.nso.system.service.ISysUserService;
import com.nso.system.support.domain.PasswordRecoveryRequest;
import com.nso.system.support.domain.SupportTicket;
import com.nso.system.support.mapper.PasswordRecoveryRequestMapper;
import com.nso.system.support.mapper.SupportTicketAttachmentMapper;
import com.nso.system.support.mapper.SupportTicketMapper;
import com.nso.system.support.service.IAccountSupportService;
import com.nso.system.support.service.impl.AccountSupportServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSupportServiceTest {
    @Mock private PasswordRecoveryRequestMapper recoveryRequests;
    @Mock private SupportTicketMapper supportTickets;
    @Mock private SupportTicketAttachmentMapper attachments;
    @Mock private FileObjectMapper fileObjects;
    @Mock private ISysUserService users;
    @Mock private PasswordCredentialPort credentials;
    @Mock private PublicRequestRateLimitPort rateLimits;
    @Mock private BusinessNumberService numbers;
    @Mock private IMessageService messages;
    @Mock private ObjectStoragePort storage;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void generatedTemporaryPasswordIsStrongAndOnlyHashIsPersistable() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        FrameworkPasswordCredentialAdapter adapter = new FrameworkPasswordCredentialAdapter(encoder);

        PasswordCredentialPort.TemporaryPassword credential = adapter.issueTemporaryPassword();

        assertEquals(14, credential.plainText().length());
        assertTrue(credential.plainText().matches(".*[A-Z].*"));
        assertTrue(credential.plainText().matches(".*[a-z].*"));
        assertTrue(credential.plainText().matches(".*[0-9].*"));
        assertTrue(credential.plainText().matches(".*[!@#$%].*"));
        assertTrue(encoder.matches(credential.plainText(), credential.passwordHash()));
    }

    @Test
    void unknownAccountRecoveryReturnsWithoutCreatingRequest() {
        AccountSupportServiceImpl service = service();
        when(rateLimits.allowPasswordRecovery(anyString(), anyString())).thenReturn(true);
        when(users.findByUsername("missing-user")).thenReturn(Optional.empty());

        service.requestPasswordRecovery(new IAccountSupportService.PasswordRecoverySubmission(
                "missing-user", "测试用户", "13800000000", null), "127.0.0.1");

        verify(recoveryRequests, never()).insert(any(PasswordRecoveryRequest.class));
        verify(messages, never()).notifyUsers(any(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void verifiedRecoveryResetsPasswordAndClosesRequestAtomically() {
        TenantContext.set(new TenantContext.Actor(1L, 9L, "system-admin", List.of("system_admin")));
        AccountSupportServiceImpl service = service();
        PasswordRecoveryRequest request = new PasswordRecoveryRequest();
        request.setId(7L);
        request.setTenantId(1L);
        request.setUserId(18L);
        request.setUsername("demo-quality");
        request.setStatus("IN_REVIEW");
        request.setVersion(0);
        request.setActiveKey("PASSWORD_RECOVERY:1:18");
        when(recoveryRequests.selectById(7L)).thenReturn(request);
        when(credentials.issueTemporaryPassword()).thenReturn(new PasswordCredentialPort.TemporaryPassword("Aa1!temporary", "bcrypt-hash"));
        when(recoveryRequests.updateById(any(PasswordRecoveryRequest.class))).thenReturn(1);

        IAccountSupportService.PasswordResetResult result = service.resetRecoveredPassword(7L,
                new IAccountSupportService.RecoveryResetSubmission("已完成线下身份核验", 0));

        verify(users).resetPassword(18L, "bcrypt-hash");
        assertEquals("Aa1!temporary", result.temporaryPassword());
        assertEquals("RESET", request.getStatus());
        assertEquals(null, request.getActiveKey());
    }

    @Test
    void openTicketCanOnlyAdvanceToProcessingWithAnOperatorNote() {
        TenantContext.set(new TenantContext.Actor(1L, 9L, "system-admin", List.of("system_admin")));
        AccountSupportServiceImpl service = service();
        SupportTicket ticket = new SupportTicket();
        ticket.setId(11L);
        ticket.setTenantId(1L);
        ticket.setTicketNo("SUP-2026-000001");
        ticket.setRequesterUserId(18L);
        ticket.setStatus("OPEN");
        ticket.setVersion(0);
        when(supportTickets.selectById(11L)).thenReturn(ticket);
        when(supportTickets.updateById(any(SupportTicket.class))).thenReturn(1);
        when(attachments.selectList(any())).thenReturn(List.of());

        service.updateSupportTicket(11L, new IAccountSupportService.SupportTicketUpdateSubmission("PROCESSING", "已受理并开始排查", 0));

        ArgumentCaptor<List<Long>> recipients = ArgumentCaptor.forClass(List.class);
        verify(messages).notifyUsers(recipients.capture(), anyString(), anyString(), anyString(), anyString(), anyLong());
        assertEquals(List.of(18L), recipients.getValue());
        assertEquals("PROCESSING", ticket.getStatus());
        assertEquals(9L, ticket.getHandlerId());
    }

    private AccountSupportServiceImpl service() {
        return new AccountSupportServiceImpl(recoveryRequests, supportTickets, attachments, fileObjects, users,
                credentials, rateLimits, numbers, messages, storage);
    }
}
