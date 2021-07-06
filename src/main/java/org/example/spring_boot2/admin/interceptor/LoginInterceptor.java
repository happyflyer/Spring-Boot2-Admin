package org.example.spring_boot2.admin.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author lifei
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 目标方法执行之前
     *
     * @param request  request
     * @param response response
     * @param handler  handler
     * @return boolean
     * @throws Exception Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        log.info("执行拦截器方法: {}.{}", "LoginInterceptor", "preHandle()");
        String requestUri = request.getRequestURI();
        // 登录检查逻辑
        HttpSession session = request.getSession();
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            log.info("--->放行的请求路径是: {}", requestUri);
            return true;
        }
        log.info("-|->拦截的请求路径是: {}", requestUri);
        request.setAttribute("msg", "请先登录");
        request.getRequestDispatcher("/login").forward(request, response);
        return false;
    }

    /**
     * 目标方法执行之后
     *
     * @param request      request
     * @param response     response
     * @param handler      handler
     * @param modelAndView modelAndView
     * @throws Exception Exception
     */
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        log.info("执行拦截器方法: {}.{}", "LoginInterceptor", "postHandle()");
    }

    /**
     * 页面渲染之后
     *
     * @param request  request
     * @param response response
     * @param handler  handler
     * @param ex       ex
     * @throws Exception Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        log.info("执行拦截器方法: {}.{}", "LoginInterceptor", "afterCompletion()");
    }
}
