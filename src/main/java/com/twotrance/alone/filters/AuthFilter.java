package com.twotrance.alone.filters;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.key.AloneKey;
import com.twotrance.alone.service.key.AloneKeyService;
import com.twotrance.alone.wrapper.RequestBodyWrapper;
import org.springframework.core.annotation.Order;
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
@Order(1)
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
        RequestBodyWrapper requestOfBody = new RequestBodyWrapper(request);
        String phone = requestOfBody.getHeader("phone");
        String appKey = requestOfBody.getHeader("appKey");
        if (StrUtil.isEmpty(phone) || StrUtil.isEmpty(appKey)) {
            phone = requestOfBody.getParameter("phone");
            appKey = requestOfBody.getParameter("appKey");
            if (StrUtil.isEmpty(phone) || StrUtil.isEmpty(appKey)) {
                String body = requestOfBody.getBody();
                if (StrUtil.isNotEmpty(body)) {
                    JSONObject bodyJSON = JSONUtil.parseObj(body);
                    phone = bodyJSON.getStr("phone");
                    appKey = bodyJSON.getStr("appKey");
                }
            }
            if (StrUtil.isEmpty(phone) || StrUtil.isEmpty(appKey)) {
                handlerExceptionResolver.resolveException(requestOfBody, response, null, ex.exception(1002));
                return;
            }
            AloneKey aloneKey = aloneKeyService.byPhoneAndKey(phone, appKey);
            if (ObjectUtil.isEmpty(aloneKey)) {
                handlerExceptionResolver.resolveException(requestOfBody, response, null, ex.exception(1003));
                return;
            }
            if (Pattern.compile("/model").matcher(requestOfBody.getRequestURI()).matches()) {
                if (!aloneKey.getAdmin().booleanValue()) {
                    handlerExceptionResolver.resolveException(requestOfBody, response, null, ex.exception(1004));
                    return;
                }
            }
            filterChain.doFilter(requestOfBody, response);
        }
    }

}

