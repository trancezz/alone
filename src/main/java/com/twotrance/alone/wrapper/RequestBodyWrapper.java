package com.twotrance.alone.wrapper;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;


/**
 * RequestBodyWrapper
 *
 * @author trance
 * @description request body wrapper
 * @date 2021/7/15
 */
public class RequestBodyWrapper extends HttpServletRequestWrapper {

    private String body;

    public RequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        getInputStream();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        String body = IoUtil.read(getRequest().getInputStream()).toString();
        if (StrUtil.isNotEmpty(body)) {
            this.body = body;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(StrUtil.isEmpty(this.body) ? "".getBytes() : this.body.getBytes());
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}

            @Override
            public int read() throws IOException {
                return bis.read();
            }
        };
    }

    public String getBody() {
        return body;
    }
}
