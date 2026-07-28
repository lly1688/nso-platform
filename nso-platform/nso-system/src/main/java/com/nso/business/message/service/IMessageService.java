package com.nso.business.message.service;
import com.nso.business.core.NsoDtos.MessageDto;
import com.nso.business.core.NsoDtos.PageResult;
public interface IMessageService { PageResult<MessageDto> list(String status); MessageDto markRead(Long messageId); MessageDto notify(String title, String content, String type, String businessType, Long businessId); MessageDto notifyOnce(String title, String content, String type, String businessType, Long businessId); int retryPendingNotifications(); }
