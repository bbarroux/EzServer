package net.barroux.ezserver.db;

import java.sql.Driver;

import org.jooq.SQLDialect;

import com.jolbox.bonecp.BoneCPConfig;

public class DbConfig {
   private final BoneCPConfig boneCPConfig;
   private final SQLDialect   sqlDialect;

   public DbConfig(Class<? extends Driver> driverClass, BoneCPConfig boneCPConfig, SQLDialect sqlDialect) {
      if (driverClass == null) {
         throw new IllegalArgumentException("No jdbc driver loaded");
      }
      this.boneCPConfig = boneCPConfig;
      this.sqlDialect = sqlDialect;
   }

   public BoneCPConfig getBoneCPConfig() {
      return boneCPConfig;
   }

   public SQLDialect getSqlDialect() {
      return sqlDialect;
   }
}
