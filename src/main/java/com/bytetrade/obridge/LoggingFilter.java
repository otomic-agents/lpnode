package com.bytetrade.obridge;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long startTime = System.currentTimeMillis();

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!httpRequest.getRequestURI().contains("/quote_and_live_confirmation")
                && !httpRequest.getRequestURI().contains("/relay/web/fetch_business")) {
            // Log before processing the request
            logger.info("Request received - {} {} - IP: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpRequest.getRemoteAddr());
        }

        chain.doFilter(request, response);

        // Log after processing the request
        long processingTime = System.currentTimeMillis() - startTime;
        if (!httpRequest.getRequestURI().contains("/quote_and_live_confirmation")
                && !httpRequest.getRequestURI().contains("/relay/web/fetch_business")) {
            logger.info("Request processed - {} {} - Processing Time: {} ms",
                    httpResponse.getStatus(),
                    httpRequest.getRequestURI(),
                    processingTime);
        }
    }

    @Override
    public void destroy() {
        // Cleanup code
    }
}