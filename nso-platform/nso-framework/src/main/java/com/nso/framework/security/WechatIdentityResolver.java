package com.nso.framework.security;

import com.nso.common.exception.BusinessException;
import com.nso.framework.config.NsoWechatProperties;
import com.nso.system.domain.SysUser;
import com.nso.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WechatIdentityResolver {
    private final NsoWechatProperties properties;
    private final ISysUserService userService;
    private final RestClient restClient = RestClient.create("https://api.weixin.qq.com");

    public WechatIdentityResolver(NsoWechatProperties properties, ISysUserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    public SysUser resolveBoundUser(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("微信登录 code 不能为空");
        }
        if (properties.isMockEnabled()) {
            if (!code.startsWith("dev:")) {
                throw new BusinessException("开发模拟登录请使用 dev:用户名 格式的 code");
            }
            return userService.findByUsername(code.substring("dev:".length()))
                    .orElseThrow(() -> new BusinessException("开发用户不存在或已禁用"));
        }
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new IllegalStateException("NSO_WECHAT_APP_ID and NSO_WECHAT_APP_SECRET are required when mock login is disabled");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get().uri(uriBuilder -> uriBuilder
                        .path("/sns/jscode2session")
                        .queryParam("appid", properties.getAppId())
                        .queryParam("secret", properties.getAppSecret())
                        .queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve().body(Map.class);
        if (response == null || response.get("openid") == null) {
            throw new BusinessException("微信登录失败，请重新获取 code");
        }
        return userService.findByWechatOpenId(String.valueOf(response.get("openid")))
                .orElseThrow(() -> new BusinessException("微信账号尚未绑定企业用户"));
    }
}
