package com.huochai.config;

import com.huochai.config.filter.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置
 * 
 * <p>功能说明：</p>
 * <ul>
 *   <li>配置 JWT 认证过滤器</li>
 *   <li>配置密码编码器（BCrypt）</li>
 *   <li>配置 CORS 跨域策略</li>
 *   <li>配置请求授权规则（URL级别权限控制）</li>
 *   <li>启用方法级别权限注解（@PreAuthorize）</li>
 * </ul>
 * 
 * <p>授权规则：</p>
 * <ul>
 *   <li>Swagger 文档：允许匿名访问</li>
 *   <li>认证接口：允许匿名访问（登录、验证码、刷新Token）</li>
 *   <li>OAuth2 端点：允许匿名访问</li>
 *   <li>健康检查：允许匿名访问</li>
 *   <li>其他请求：需要认证</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * // 方法级别权限控制
 * &#64;PreAuthorize("hasAuthority('user:create')")
 * &#64;PostMapping("/users")
 * public Result createUser() { ... }
 * 
 * // 角色级别权限控制
 * &#64;PreAuthorize("hasRole('ADMIN')")
 * &#64;DeleteMapping("/users/{id}")
 * public Result deleteUser() { ... }
 * </pre>
 *
 * @author peilizhi
 * @date 2026/3/25 19:19
 * @see JwtAuthenticationFilter
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * JWT 认证过滤器
     * 负责解析和验证 JWT Token，加载用户权限
     */
    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    /**
     * 密码编码器
     * 
     * <p>使用 BCrypt 加密算法，强度为默认值（10）</p>
     * <p>BCrypt 是一种安全的单向哈希算法，专门用于密码存储</p>
     * 
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 跨域配置
     * 
     * <p>允许所有来源、所有方法、所有请求头的跨域请求</p>
     * <p>生产环境建议配置具体的允许域名</p>
     * 
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（生产环境建议配置具体域名）
        configuration.setAllowedOriginPatterns(List.of("*"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 允许携带凭证（Cookie、Authorization头等）
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤链配置
     * 
     * <p>主要配置：</p>
     * <ul>
     *   <li>CORS 跨域支持</li>
     *   <li>禁用 CSRF（使用 JWT 无需 CSRF 保护）</li>
     *   <li>无状态会话（STATELESS）</li>
     *   <li>请求授权规则</li>
     *   <li>JWT 过滤器</li>
     * </ul>
     * 
     * @param http HttpSecurity 配置对象
     * @return 配置完成的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ========== CORS 配置 ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // ========== CSRF 配置 ==========
                // 禁用 CSRF，因为使用 JWT 进行认证，不需要 CSRF 保护
                .csrf(csrf -> csrf.disable())
                
                // ========== Session 配置 ==========
                // 使用无状态 Session，不创建和使用 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // ========== 请求授权配置 ==========
                .authorizeHttpRequests(auth -> auth
                        // ----- Swagger 文档放行 -----
                        .requestMatchers(
                                "/swagger-ui/**",       // Swagger UI 页面
                                "/swagger-ui.html",     // Swagger UI 入口
                                "/v3/api-docs/**",      // OpenAPI 文档
                                "/webjars/**"           // Swagger 静态资源
                        ).permitAll()

                        // ----- 认证接口放行 -----
                        .requestMatchers(
                                "/auth/login",          // 用户登录
                                "/auth/captcha",        // 获取验证码
                                "/auth/refresh"         // 刷新Token
                        ).permitAll()

                        // ----- 健康检查放行 -----
                        .requestMatchers(
                                "/actuator/**"          // Spring Actuator 端点
                        ).permitAll()

                        // ----- OAuth2 端点放行 -----
                        .requestMatchers(
                                "/oauth2/**",           // OAuth2 授权端点
                                "/.well-known/**",      // OIDC 发现端点
                                "/userinfo",            // OIDC UserInfo端点
                                "/login",               // 登录页面
                                "/oauth2/consent"       // 授权同意页面
                        ).permitAll()

                        // ----- 静态资源放行 -----
                        .requestMatchers(
                                "/favicon.ico",         // 网站图标
                                "/error"                // 错误页面
                        ).permitAll()

                        // ----- 其他请求需要认证 -----
                        .anyRequest().authenticated()
                )
                
                // ========== JWT 过滤器配置 ==========
                // 在 UsernamePasswordAuthenticationFilter 之前执行 JWT 认证
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}