package com.cloudferro.copernicus.experiments.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;


public class AuthenticationTokenFilter extends UsernamePasswordAuthenticationFilter {

    public static final String AUTHORIZATION_COOKIE_NAME = "Authorization";
    public static Logger logger = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    @Autowired
    private Environment env;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        logger.debug("enter doFilter()");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authToken = null;
        String authCookie = null;
        authToken = httpRequest.getParameter("jwtToken");
        logger.debug("authToken from parameter jwtToken: " + authToken);
        if(authToken == null) {
            var cookies = ((HttpServletRequest) request).getCookies();
            if (cookies != null && cookies.length > 0) {
                authToken = Arrays.stream(cookies)
                        .filter(cookie -> AUTHORIZATION_COOKIE_NAME.equals(cookie.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElse(null);
                authCookie = authToken;
                logger.debug("authToken from cookie Authorization: " + authToken);
            }
        }
        if (authToken != null) {
            try {
                var claims = getClaimsFromToken(authToken);
                logger.info("authentication successful");
                Map mlpuser = claims.get("mlpuser", Map.class);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mlpuser.get("email"), authToken, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (authCookie == null) {
                    var cookie = new Cookie(AUTHORIZATION_COOKIE_NAME, authToken);
                    cookie.setHttpOnly(true);
                    cookie.setPath(contextPath);
                    ((HttpServletResponse)response).addCookie(cookie);
                }
                chain.doFilter(request, response);
            } catch (Exception e) {
                e.printStackTrace();
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Claims getClaimsFromToken(String token) throws Exception {
        Claims claims = null;
        String secret = env.getProperty("jwt.auth.secret.key");
        claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        if (claims.getExpiration().before(new Date())) {
            throw new RuntimeException("jwt token expired");
        }
        logger.info("claims from token: " + claims);
        return claims;
    }

}

