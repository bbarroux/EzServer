package net.barroux.ezserver.db;

public class DbException extends RuntimeException {

   private static final long serialVersionUID = 1L;

   public DbException(String message) {
      super(message);
   }

   public DbException(Throwable e) {
      super(e);
   }

   public DbException(String message, Throwable cause) {
      super(message, cause);
   }
}
