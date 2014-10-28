package net.barroux.ezserver.filters;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogRequestFilter implements Filter {
   private static final Logger        log     = LoggerFactory.getLogger(LogRequestFilter.class);
   private static final AtomicInteger COUNTER = new AtomicInteger(0);

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      log.trace("entering");
      int reqNr = COUNTER.incrementAndGet();
      final long deb = System.currentTimeMillis();
      final HttpServletRequest req = (HttpServletRequest) request;
      if (log.isTraceEnabled()) {
         log.trace("filtering {} - paramsMap : {}", req.getRequestURI(), req.getParameterMap());
      }
      chain.doFilter(req, response);
      response.flushBuffer();
      long total = System.currentTimeMillis() - deb;
      log.info("{} - [{}]; total : {}ms", reqNr, req.getRequestURI(), total);
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
   }

   @Override
   public void destroy() {
   }

}
