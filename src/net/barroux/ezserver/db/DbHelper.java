package net.barroux.ezserver.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;

public class DbHelper {
	private static final ThreadLocal<Connection> CONNECTIONS = new ThreadLocal<>();
	private static final ThreadLocal<DSLContext> CTXS = new ThreadLocal<>();
	private static final Logger log = LoggerFactory.getLogger(DbHelper.class);
	private static BoneCP POOL;
	private static DbConfig CFG;

	public static void init(DbConfig cfg) {
		if (CFG != null) {
			throw new IllegalStateException("Db access already initialized");
		}

		try {

			CFG = cfg;
			POOL = new BoneCP(CFG.getBoneCPConfig());
			log.debug("Db access is now ready");
		} catch (SQLException e) {
			throw new DbException("Could not initialize pool", e);
		}
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
			Connection conn = POOL.getConnection();
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
			DSLContext ctx = DSL.using(dcp, CFG.getSqlDialect());
			CTXS.set(ctx);
			return ctx;
	}
}
