package org.example.spring_boot2.admin.servlet;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author lifei
 */
@Slf4j
// @WebFilter(urlPatterns = {"/my", "/css/*"})
public class MyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        log.info("执行过滤器方法：{}.{}", "MyFilter", "init()");
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request1 = (HttpServletRequest) request;
        log.info("|-->执行过滤器方法：{}.{} {} {}", "MyFilter", "doFilter()",
                "before", request1.getServletPath());
        chain.doFilter(request, response);
        log.info("-~->过滤的请求路径是: {}", request1.getServletPath());
        log.info("--|>执行过滤器方法：{}.{} {} {}", "MyFilter", "doFilter()",
                "after", request1.getServletPath());
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
        log.info("执行过滤器方法：{}.{}", "MyFilter", "destroy()");
    }
}
