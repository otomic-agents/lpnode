package com.bytetrade.obridge;

import javax.servlet.http.*;
import javax.servlet.*;
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

        // Log before processing the request
        logger.info("Request received - {} {} - IP: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());

        chain.doFilter(request, response);

        // Log after processing the request
        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Request processed - {} {} - Processing Time: {} ms",
                httpResponse.getStatus(),
                httpRequest.getRequestURI(),
                processingTime);
    }

    @Override
    public void destroy() {
        // Cleanup code
    }
}