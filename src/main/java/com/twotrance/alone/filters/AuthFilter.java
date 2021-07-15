package com.twotrance.alone.filters;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.key.AloneKey;
import com.twotrance.alone.service.key.AloneKeyService;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * AuthFilter
 *
 * @author trance
 * @description auth filer
 * @date 2021/7/8
 */
@WebFilter(filterName = "authFilter", urlPatterns = "/*", asyncSupported = true)
public class AuthFilter implements Filter {

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
        String requestURI = request.getRequestURI();
        Boolean idPattern = Pattern.compile("/id/(seg|snow)\\?\\S*").matcher(requestURI + "?").matches();
        Boolean modelPattern = Pattern.compile("/model").matcher(requestURI).matches();
        if (!idPattern && !modelPattern) {
            request.getRequestDispatcher("/error/404").forward(request, response);
            return;
        }
        String phone = request.getParameter("phone");
        String appKey = request.getParameter("appKey");
        if (StrUtil.isEmpty(phone) || StrUtil.isEmpty(appKey)) {
            handlerExceptionResolver.resolveException(request, response, null, ex.exception(1002));
            return;
        }
        AloneKey aloneKey = aloneKeyService.byPhoneAndKey(phone, appKey);
        if (ObjectUtil.isEmpty(aloneKey)) {
            handlerExceptionResolver.resolveException(request, response, null, ex.exception(1003));
            return;
        }
        if (modelPattern.booleanValue()) {
            if (!aloneKey.getAdmin().booleanValue()) {
                handlerExceptionResolver.resolveException(request, response, null, ex.exception(1004));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

}



