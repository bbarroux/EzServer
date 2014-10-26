package net.barroux.ezserver.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPDataSource;

public class DbHelper {
	private static final ThreadLocal<Connection> CONNECTIONS = new ThreadLocal<>();
	private static final ThreadLocal<DSLContext> CTXS = new ThreadLocal<>();
	private static final Logger log = LoggerFactory.getLogger(DbHelper.class);
	private static BoneCPDataSource DS;
	private static SQLDialect DIALECT;

	protected static DataSource getDs() {
		return DS;
	}

	public static void init(DbConfig cfg) {
		DS = new BoneCPDataSource(cfg.getBoneCPConfig());
		DIALECT = cfg.getSqlDialect();
		log.debug("Db access is now ready");
	}

	public static void commit() {
		try {
			CONNECTIONS.get().commit();
		} catch (SQLException e) {
			throw new DbException("Could not commit", e);
		}
	}

	public static void rollback() {
		try {
			CONNECTIONS.get().rollback();
		} catch (SQLException e) {
			throw new DbException("Could not rollback", e);
		}
	}

	public static void close() {
		DbUtils.closeQuietly(CONNECTIONS.get());
		CTXS.remove();
		CONNECTIONS.remove();
	}

	public static DSLContext db() {
		DSLContext ctx = CTXS.get();
		if (ctx == null) {
			ctx = initCtx();
		}
		return ctx;
	}

	public static Connection conn() {
		Connection conn = CONNECTIONS.get();
		if (conn == null) {
			conn = initConn();
		}
		return conn;
	}

	private static Connection initConn() {
		try {
			long deb = System.nanoTime();
			Connection conn = DS.getConnection();
			conn.setAutoCommit(false);
			CONNECTIONS.set(conn);
			long elapsed = (System.nanoTime() - deb) / 1000;
			log.debug("new connection initialized for thread {}µs", elapsed);
			return conn;
		} catch (SQLException e) {
			throw new DbException("Could not get connection from pool", e);
		}
	}

	private static DSLContext initCtx() {
		DefaultConnectionProvider dcp = new DefaultConnectionProvider(conn());
		DSLContext ctx = DSL.using(dcp, DIALECT);
		CTXS.set(ctx);
		return ctx;
	}
}
