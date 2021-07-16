package com.twotrance.alone.filters;

import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.service.key.AloneKeyService;
import com.twotrance.alone.wrapper.XssHttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * AuthFilter
 *
 * @author trance
 * @description auth filer
 * @date 2021/7/8
 */
@Order(2)
@WebFilter(filterName = "xssFilter", urlPatterns = "/*", asyncSupported = true)
public class XssFilter implements Filter {

    @Resource
    private AloneKeyService aloneKeyService;

    @Resource
    private ExceptionHandler ex;

    @Resource
    private HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        XssHttpServletRequestWrapper requestOfXss = new XssHttpServletRequestWrapper(request);
        filterChain.doFilter(requestOfXss, response);
    }

}

