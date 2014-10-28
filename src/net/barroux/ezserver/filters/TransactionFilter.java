package net.barroux.ezserver.filters;

import static net.barroux.ezserver.db.DbHelper.close;
import static net.barroux.ezserver.db.DbHelper.commit;
import static net.barroux.ezserver.db.DbHelper.rollback;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.barroux.ezserver.db.DbHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionFilter implements Filter {
   private static final Logger log = LoggerFactory.getLogger(TransactionFilter.class);

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      log.trace("entering");
      try {
         DbHelper.conn();
         chain.doFilter(request, response);
         commit();
      }
      catch (Exception e) {
         log.warn("exception during request processing. propagating", e.getMessage());
         rollback();
         throw e;
      }
      finally {
         close();
      }
      log.trace("done");
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
   }

   @Override
   public void destroy() {
   }

}
