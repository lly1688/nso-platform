package com.nso.business.message.service;

import com.nso.business.core.NsoDtos.MessageDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.List;

/**
 * 站内消息与通知服务接口。
 */
public interface IMessageService {

    /**
     * 查询当前用户消息。
     *
     * @param status 消息状态
     * @return 消息分页结果
     */
    PageResult<MessageDto> list(String status);

    /**
     * 分页查询当前用户消息。
     *
     * @param status 消息状态
     * @param pageQuery 分页参数
     * @return 消息分页结果
     */
    PageResult<MessageDto> list(String status, PageQuery pageQuery);

    /**
     * 将消息标记为已读。
     *
     * @param messageId 消息编号
     * @return 更新后的消息
     */
    MessageDto markRead(Long messageId);

    /**
     * 向当前用户发送消息。
     *
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 新建的消息
     */
    MessageDto notify(String title, String content, String type, String businessType, Long businessId);

    /**
     * 向指定用户批量发送消息。
     *
     * @param receiverIds 接收用户编号
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 新增消息数
     */
    int notifyUsers(List<Long> receiverIds, String title, String content, String type, String businessType, Long businessId);

    /**
     * 按业务维度幂等发送消息。
     *
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 已存在或新建的消息
     */
    MessageDto notifyOnce(String title, String content, String type, String businessType, Long businessId);

    /**
     * 向项目成员发送事件通知。
     *
     * @param projectId 项目编号
     * @param eventType 事件类型
     * @param title 消息标题
     * @param content 消息内容
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 新增消息数
     */
    int notifyProject(Long projectId, String eventType, String title, String content, String businessType, Long businessId);

    /**
     * 重试待发送通知。
     *
     * @return 成功处理数量
     */
    int retryPendingNotifications();
}
