package com.huochai.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ClientApplicationConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/oauth2/loginSuccess", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("client-app")
            .clientId("client-app")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1:8082/login/oauth2/code/client-app")
            .scope("openid", "message.read", "message.write")
            .authorizationUri("http://127.0.0.1:9000/oauth2/authorize")
            .tokenUri("http://127.0.0.1:9000/oauth2/token")
            .userInfoUri("http://127.0.0.1:9000/userinfo")
            .jwkSetUri("http://127.0.0.1:9000/oauth2/jwks")
            .userNameAttributeName("sub")
            .clientName("Client App")
            .build();

        return new InMemoryClientRegistrationRepository(clientRegistration);
    }
}