package net.barroux.ezserver.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbutils.DbUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;

public class DbHelper {
   private static final ThreadLocal<DSLContext> CTXS = new ThreadLocal<>();
   private static final Logger                  log  = LoggerFactory.getLogger(DbHelper.class);
   private static BoneCP                        POOL;
   private static DbConfig                      CFG;

   public static void init(DbConfig cfg) {
      if (CFG != null) {
         throw new IllegalStateException("Db access already initialized");
      }

      try {

         CFG = cfg;
         POOL = new BoneCP(CFG.getBoneCPConfig());
      }
      catch (SQLException e) {
         throw new DbException("Could not initialize pool", e);
      }
   }

   private static DefaultConnectionProvider getCp() {
      DSLContext ctx = CTXS.get();
      if (ctx == null) return null;
      return (DefaultConnectionProvider) ctx.configuration().connectionProvider();
   }

   public static void commit() {
      DefaultConnectionProvider conn = getCp();
      if (conn != null) {
         log.debug("committing transaction");
         conn.commit();
      }
   }

   public static void rollback() {
      DefaultConnectionProvider conn = getCp();
      if (conn != null) {
         log.debug("rollbacking transaction");
         conn.rollback();
      }
   }

   public static void close() {
      DefaultConnectionProvider conn = getCp();
      if (conn != null) {
         DbUtils.closeQuietly(conn.acquire());
         CTXS.remove();
      }
   }

   public static DSLContext db() {
      DSLContext ctx = CTXS.get();
      if (ctx == null) {
         ctx = initCtx();
      }
      return ctx;
   }

   private static DSLContext initCtx() {
      try {
         Connection conn = POOL.getConnection();
         conn.setAutoCommit(false);
         DSLContext ctx = DSL.using(new DefaultConnectionProvider(conn), CFG.getSqlDialect());
         CTXS.set(ctx);
         log.debug("new connection initialized");
         return ctx;
      }
      catch (Exception e) {
         throw new DbException("Could not get connection from pool", e);
      }

   }
}
