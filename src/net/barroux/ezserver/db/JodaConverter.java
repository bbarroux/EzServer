package net.barroux.ezserver.db;

import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.jooq.Converter;

public class JodaConverter implements Converter<Timestamp, DateTime> {

   private static final long serialVersionUID = 1L;

   @Override
   public DateTime from(Timestamp databaseObject) {
      return (databaseObject == null) ? null : new DateTime(databaseObject.getTime());
   }

   @Override
   public Timestamp to(DateTime userObject) {
      return (userObject == null) ? null : new Timestamp(userObject.getMillis());
   }

   @Override
   public Class<Timestamp> fromType() {
      return Timestamp.class;
   }

   @Override
   public Class<DateTime> toType() {
      return DateTime.class;
   }

}
