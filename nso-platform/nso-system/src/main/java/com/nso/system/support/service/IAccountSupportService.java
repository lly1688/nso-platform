package com.nso.system.support.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.file.service.FileDownloadPayload;
import com.nso.business.file.service.FileUploadPayload;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号恢复与技术支持服务接口。
 */
public interface IAccountSupportService {

    /**
     * 提交密码恢复申请。
     *
     * @param request 恢复申请
     * @param clientIp 客户端地址
     */
    void requestPasswordRecovery(PasswordRecoverySubmission request, String clientIp);

    /**
     * 创建访客支持工单。
     *
     * @param request 工单内容
     * @param clientIp 客户端地址
     * @return 新建的工单
     */
    SupportTicketView createGuestSupportTicket(GuestSupportTicketSubmission request, String clientIp);

    /**
     * 创建当前用户支持工单。
     *
     * @param request 工单内容
     * @return 新建的工单
     */
    SupportTicketView createCurrentUserSupportTicket(CurrentUserSupportTicketSubmission request);

    /**
     * 查询当前用户支持工单。
     *
     * @return 工单分页结果
     */
    PageResult<SupportTicketView> listMySupportTickets();

    /**
     * 分页查询当前用户支持工单。
     *
     * @param pageQuery 分页参数
     * @return 工单分页结果
     */
    PageResult<SupportTicketView> listMySupportTickets(PageQuery pageQuery);

    /**
     * 上传支持工单图片。
     *
     * @param ticketId 工单编号
     * @param file 图片文件
     * @return 附件信息
     */
    SupportTicketAttachmentView uploadSupportTicketImage(Long ticketId, FileUploadPayload file);

    /**
     * 下载支持工单图片。
     *
     * @param ticketId 工单编号
     * @param attachmentId 附件编号
     * @return 附件内容
     */
    AttachmentContent downloadSupportTicketImage(Long ticketId, Long attachmentId);

    /**
     * 查询密码恢复申请。
     *
     * @param status 申请状态
     * @return 恢复申请分页结果
     */
    PageResult<PasswordRecoveryView> listPasswordRecoveryRequests(String status);

    /**
     * 分页查询密码恢复申请。
     *
     * @param status 申请状态
     * @param pageQuery 分页参数
     * @return 恢复申请分页结果
     */
    PageResult<PasswordRecoveryView> listPasswordRecoveryRequests(String status, PageQuery pageQuery);

    /**
     * 审核密码恢复申请。
     *
     * @param requestId 申请编号
     * @param request 审核内容
     * @return 更新后的恢复申请
     */
    PasswordRecoveryView reviewPasswordRecovery(Long requestId, RecoveryReviewSubmission request);

    /**
     * 为已核验申请重置密码。
     *
     * @param requestId 申请编号
     * @param request 重置内容
     * @return 密码重置结果
     */
    PasswordResetResult resetRecoveredPassword(Long requestId, RecoveryResetSubmission request);

    /**
     * 查询全部支持工单。
     *
     * @param status 工单状态
     * @return 工单分页结果
     */
    PageResult<SupportTicketView> listSupportTickets(String status);

    /**
     * 分页查询全部支持工单。
     *
     * @param status 工单状态
     * @param pageQuery 分页参数
     * @return 工单分页结果
     */
    PageResult<SupportTicketView> listSupportTickets(String status, PageQuery pageQuery);

    /**
     * 更新支持工单处理状态。
     *
     * @param ticketId 工单编号
     * @param request 处理内容
     * @return 更新后的工单
     */
    SupportTicketView updateSupportTicket(Long ticketId, SupportTicketUpdateSubmission request);

    /**
     * 密码恢复申请。
     *
     * @param username 用户名
     * @param contactName 联系人
     * @param contactValue 联系方式
     * @param requesterNote 申请说明
     */
    record PasswordRecoverySubmission(String username, String contactName, String contactValue, String requesterNote) {
    }

    /**
     * 访客支持工单申请。
     *
     * @param username 用户名
     * @param contactName 联系人
     * @param contactValue 联系方式
     * @param category 问题分类
     * @param priority 紧急程度
     * @param description 问题描述
     */
    record GuestSupportTicketSubmission(String username, String contactName, String contactValue,
                                        String category, String priority, String description) {
    }

    /**
     * 当前用户支持工单申请。
     *
     * @param category 问题分类
     * @param priority 紧急程度
     * @param description 问题描述
     * @param pageContext 页面上下文
     */
    record CurrentUserSupportTicketSubmission(String category, String priority, String description, String pageContext) {
    }

    /**
     * 密码恢复审核内容。
     *
     * @param status 目标状态
     * @param handlingNote 处理说明
     * @param version 数据版本号
     */
    record RecoveryReviewSubmission(String status, String handlingNote, Integer version) {
    }

    /**
     * 密码恢复重置内容。
     *
     * @param handlingNote 核验说明
     * @param version 数据版本号
     */
    record RecoveryResetSubmission(String handlingNote, Integer version) {
    }

    /**
     * 支持工单处理内容。
     *
     * @param status 目标状态
     * @param handlingNote 处理说明
     * @param version 数据版本号
     */
    record SupportTicketUpdateSubmission(String status, String handlingNote, Integer version) {
    }

    /**
     * 密码恢复申请视图。
     *
     * @param id 申请编号
     * @param username 用户名
     * @param contactName 联系人
     * @param contactValue 联系方式
     * @param requesterNote 申请说明
     * @param status 申请状态
     * @param handlerId 处理人编号
     * @param handlingNote 处理说明
     * @param reviewedAt 审核时间
     * @param resetAt 重置时间
     * @param version 数据版本号
     * @param createdAt 创建时间
     */
    record PasswordRecoveryView(Long id, String username, String contactName, String contactValue,
                                String requesterNote, String status, Long handlerId, String handlingNote,
                                LocalDateTime reviewedAt, LocalDateTime resetAt, Integer version,
                                LocalDateTime createdAt) {
    }

    /**
     * 支持工单视图。
     *
     * @param id 工单编号
     * @param ticketNo 工单编号文本
     * @param requesterUserId 申请用户编号
     * @param requesterUsername 申请用户名
     * @param contactName 联系人
     * @param contactValue 联系方式
     * @param source 工单来源
     * @param category 问题分类
     * @param priority 紧急程度
     * @param pageContext 页面上下文
     * @param description 问题描述
     * @param status 工单状态
     * @param handlerId 处理人编号
     * @param handlingNote 处理说明
     * @param resolvedAt 解决时间
     * @param version 数据版本号
     * @param createdAt 创建时间
     * @param attachments 附件列表
     */
    record SupportTicketView(Long id, String ticketNo, Long requesterUserId, String requesterUsername,
                             String contactName, String contactValue, String source, String category,
                             String priority, String pageContext, String description, String status,
                             Long handlerId, String handlingNote, LocalDateTime resolvedAt, Integer version,
                             LocalDateTime createdAt, List<SupportTicketAttachmentView> attachments) {
    }

    /**
     * 支持工单附件视图。
     *
     * @param id 附件编号
     * @param fileName 文件名
     * @param contentType 内容类型
     * @param fileSize 文件大小
     * @param downloadUrl 下载地址
     */
    record SupportTicketAttachmentView(Long id, String fileName, String contentType, Long fileSize,
                                       String downloadUrl) {
    }

    /**
     * 密码重置结果。
     *
     * @param request 恢复申请
     * @param temporaryPassword 临时密码
     */
    record PasswordResetResult(PasswordRecoveryView request, String temporaryPassword) {
    }

    /**
     * 工单附件内容。
     *
     * @param content 文件下载载荷
     */
    record AttachmentContent(FileDownloadPayload content) {
    }
}
