package org.example.spring_boot2.admin.servlet;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * @author lifei
 */
@Configuration(proxyBeanMethods = true)
public class MyConfig {
    @Bean
    public ServletRegistrationBean myServlet() {
        MyServlet myServlet = new MyServlet();
        return new ServletRegistrationBean(myServlet, "/my1", "/my2");
    }

    @Bean
    public FilterRegistrationBean myFilter() {
        MyFilter myFilter = new MyFilter();
        // return new FilterRegistrationBean(myFilter, myServlet());
        FilterRegistrationBean bean = new FilterRegistrationBean(myFilter);
        bean.setUrlPatterns(Arrays.asList("/my1", "/css/*"));
        return bean;
    }

    @Bean
    public ServletListenerRegistrationBean myListener() {
        MyListener myListener = new MyListener();
        return new ServletListenerRegistrationBean<>(myListener);
    }
}
