package com.jade.oauth.config;

import com.jade.common.oauth.constant.OauthConstant;
import com.jade.common.oauth.domain.LoginUser;
import com.jade.oauth.exception.CustomWebResponseExceptionTranslator;
import com.jade.oauth.service.redis.RedisClientDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableAuthorizationServer
// @RequiredArgsConstructor
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    @Qualifier(value = "userDetailsServiceImpl")
    private UserDetailsService userDetailsService;
    @Autowired
    private TokenEnhancer tokenEnhancer;

    /**
     * ?????????????????????????????????????????????
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints
                // ????????????
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                // ??????token????????????
                .tokenStore(tokenStore())
                // ?????????????????????
                .tokenEnhancer(tokenEnhancer)
                // ????????????????????????
                .userDetailsService(userDetailsService)
                // ?????????????????????
                .authenticationManager(authenticationManager)
                // ?????????????????? refresh_token
                .reuseRefreshTokens(false)
                // ?????????????????????
                .exceptionTranslator(new CustomWebResponseExceptionTranslator());
    }

    /**
     * ??????????????????(Token Endpoint)???????????????
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer.allowFormAuthenticationForClients().checkTokenAccess("permitAll()");
    }

    /**
     * ?????? ClientDetails??????
     */
    public RedisClientDetailsService clientDetailsService() {
        return new RedisClientDetailsService(dataSource);
    }

    /**
     * ?????????????????????
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientDetailsService());
    }

    /**
     * ?????? Redis ??????????????????????????????
     */
    @Bean
    public TokenStore tokenStore() {
        RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
        tokenStore.setPrefix(OauthConstant.OAUTH_ACCESS);
        return tokenStore;
    }

    /**
     * ?????????????????????
     */
    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            if (authentication.getUserAuthentication() != null) {
                Map<String, Object> additionalInformation = new LinkedHashMap<>();
                LoginUser user = (LoginUser) authentication.getUserAuthentication().getPrincipal();
                additionalInformation.put(OauthConstant.DETAILS_USER_ID, user.getUserId());
                additionalInformation.put(OauthConstant.DETAILS_USERNAME, user.getUsername());
                ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInformation);
            }
            return accessToken;
        };
    }
}
