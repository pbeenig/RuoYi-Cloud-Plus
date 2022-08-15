package com.ruoyi.auth.listener;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.ruoyi.common.core.constant.CacheNames;
import com.ruoyi.common.core.enums.UserType;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.ip.AddressUtils;
import com.ruoyi.common.redis.utils.CacheUtils;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.system.api.domain.SysUserOnline;
import com.ruoyi.system.api.model.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户行为 侦听器的实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserActionListener implements SaTokenListener {

    private final SaTokenConfig tokenConfig;

    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        UserType userType = UserType.getUserType(loginId.toString());
        if (userType == UserType.SYS_USER) {
            UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
            String ip = ServletUtils.getClientIP();
            LoginUser user = LoginHelper.getLoginUser();
            SysUserOnline userOnline = new SysUserOnline();
            userOnline.setIpaddr(ip);
            userOnline.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
            userOnline.setBrowser(userAgent.getBrowser().getName());
            userOnline.setOs(userAgent.getOs().getName());
            userOnline.setLoginTime(System.currentTimeMillis());
            userOnline.setTokenId(tokenValue);
            userOnline.setUserName(user.getUsername());
            if (ObjectUtil.isNotNull(user.getDeptName())) {
                userOnline.setDeptName(user.getDeptName());
            }
            String cacheNames = CacheNames.ONLINE_TOKEN;
            if (tokenConfig.getTimeout() > 0) {
                // 增加 ttl 过期时间 单位秒
                cacheNames = CacheNames.ONLINE_TOKEN + "#" + tokenConfig.getTimeout() + "s";
            }
            CacheUtils.put(cacheNames, tokenValue, userOnline);
            log.info("user doLogin, useId:{}, token:{}", loginId, tokenValue);
        } else if (userType == UserType.APP_USER) {
            // app端 自行根据业务编写
        }
    }

    /**
     * 每次注销时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        CacheUtils.evict(CacheNames.ONLINE_TOKEN, tokenValue);
        log.info("user doLogout, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        CacheUtils.evict(CacheNames.ONLINE_TOKEN, tokenValue);
        log.info("user doLogoutByLoginId, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        CacheUtils.evict(CacheNames.ONLINE_TOKEN, tokenValue);
        log.info("user doReplaced, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, long disableTime) {
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
    }


}
