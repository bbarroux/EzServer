package net.barroux.ezserver.db;

import static com.google.common.collect.Lists.newArrayList;
import static net.barroux.ezserver.db.DbHelper.conn;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.apache.commons.dbutils.AsyncQueryRunner;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EzQr {
	static final Logger log = LoggerFactory.getLogger(EzQr.class);
	private static final QueryRunner QR = new FsQueryRunner();
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
	private static final AsyncQueryRunner AQR = new AsyncQueryRunner(EXECUTOR_SERVICE, new FsQueryRunner(DbHelper.getDs()));

	private EzQr() {
	}

	public static int update(String sql, Object... params) {
		try {
			return QR.update(conn(), sql, params);
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public static Integer getInteger(String query, Object... params) {
		Object[] o = getArray(query, params);
		return o == null || o[0] == null ? null : ((BigDecimal) o[0])
				.intValue();
	}

	public static String getString(String query, Object... params) {
		Object[] o = getArray(query, params);
		return o == null || o[0] == null ? null : String.valueOf(o[0]);
	}

	public static Object[] getArray(String query, Object... params) {
		try {
			return QR.query(conn(), query, new ArrayHandler(),
					fixParams(params));
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public static <V> V query(String sql, ResultSetHandler<V> rsh,
			Object... params) {
		try {
			return QR.query(conn(), sql, rsh, params);
		} catch (SQLException s) {
			log.error("Error while executing request " + s.getMessage(), s);
			throw new DbException("Can't execute request " + sql, s);
		}
	}
	
	
	public static <V> List<V> queryAllInBeans(Class<V> bean, String query, Object... params) {
		long start = new Date().getTime();
		List<V> obj = null;
		log.debug(query);
		try {
			ResultSetHandler<List<V>> h = new BeanListHandler<V>(bean);
			obj = QR.query(conn(), query, h, fixParams(params));
		} catch (SQLException e) {
			log.error("QueryAllRowsInListBean. Cant process query", e);
			throw new DbException("QueryAllRowsInListBean. Cant process query " + query, e);
		}
		log.debug("queryAllRowsInListBean Execution Time : {}ms",(new Date().getTime() - start));
		return obj;
	}
	public static <V> Future<List<V>> queryAsyncAllInBeans(Class<V> bean, String query, Object... params) {
		long start = new Date().getTime();
		Future<List<V>> obj = null;
		log.debug(query);
		try {
			ResultSetHandler<List<V>> h = new BeanListHandler<V>(bean);
			obj = AQR.query(conn(), query, h, fixParams(params));
		} catch (SQLException e) {
			log.error("QueryAllRowsInListBean. Cant process query", e);
			throw new DbException("QueryAllRowsInListBean. Cant process query " + query, e);
		}
		log.debug("queryAllRowsInListBean launch Time : {}ms",(new Date().getTime() - start));
		return obj;
	}

	public static List<Object[]> queryAllInList(String query, Object... params) {
		long start = new Date().getTime();
		List<Object[]> obj = null;
		log.debug("--SELECT  : " + query);
		log.debug("--PARAMS-- : " + StringUtils.join(params, " , "));
		try {
			ResultSetHandler<List<Object[]>> h = new ArrayListHandler();
			obj = QR.query(conn(), query, h, fixParams(params));
		} catch (SQLException e) {
			log.error("queryAllInList - Cant process query", e);
			throw new DbException("queryAllInList - Cant process query "
					+ query, e);
		}
		log.debug("Execution Time :  {}ms ({} rows)",
				(new Date().getTime() - start), obj.size());
		return obj;
	}

	public static Future<List<Object[]>> queryAsyncAllInList(String query, Object... params) {
		Future<List<Object[]>> obj = null;
		log.debug("--ASYNC SELECT  : " + query);
		log.debug("--PARAMS-- : " + StringUtils.join(params, " , "));
		try {
			ResultSetHandler<List<Object[]>> h = new ArrayListHandler();
			obj = AQR.query(query, h, fixParams(params));
		} catch (SQLException e) {
			log.error("queryAllInList - Cant process query", e);
			throw new DbException("queryAllInList - Cant process query "
					+ query, e);
		}
		log.debug("Async query launched ({}...)", query.substring(0, Math.min(30, query.length())));
		return obj;
	}

	/**
	 * Renvoie une liste d'Integer.
	 * 
	 * Pour que cela fonctionne, il faut que la première (et idéalement unique)
	 * colonne sélectionnée soit un nombre, et ne soit jamais nulle. Si une
	 * seule entrée est nulle, la méthode lancera une NullPointerException.
	 * 
	 * Idéalement conçue pour des requêtes d'id du type
	 * " SELECT STA_ID FROM GRT_ACTOR WHERE STA_TYPE='CDI' "
	 */
	public static List<Integer> queryAllInListOfInteger(String query,
			Object... params) {
		List<Object[]> objs = queryAllInList(query, params);
		List<Integer> result = newArrayList();
		for (Object[] ob : objs) {
			result.add(((BigDecimal) ob[0]).intValue());
		}
		return result;
	}

	/**
	 * Renvoie une liste de String.
	 * 
	 * Si une entr�e est null, la liste contient une entr�e null.
	 * 
	 */
	public static List<String> queryAllInListOfString(String query,
			Object... params) {
		List<Object[]> objs = queryAllInList(query, params);
		List<String> result = newArrayList();
		for (Object[] ob : objs) {
			String val = (String) ob[0];
			result.add((val != null ? val.toString() : null));
		}
		return result;
	}

	public static interface EzList extends List<Map<String, Object>> {

	};

	public static List<Map<String, Object>> queryAllInListOfMap(String query,Object... param) {
		long start = new Date().getTime();
		List<Map<String, Object>> obj = null;
		log.debug(query);
		try {
			obj = QR.query(conn(), query, new MapListHandler(),fixParams(param));
		} catch (SQLException e) {
			log.error("Cant process query", e);
			throw new DbException("Cant process query " + query, e);
		}
		log.debug("Execution Time :  {}ms", (new Date().getTime() - start));
		return obj;
	}
	
	public static Future<List<Map<String, Object>>> queryAsyncAllInListOfMap(String query,Object... param) {
		long start = new Date().getTime();
		Future<List<Map<String, Object>>> obj = null;
		log.debug(query);
		try {
			obj = AQR.query(conn(), query, new MapListHandler(),fixParams(param));
		} catch (SQLException e) {
			log.error("Cant process query", e);
			throw new DbException("Cant process query " + query, e);
		}
		log.debug("Launch Time :  {}ms", (new Date().getTime() - start));
		return obj;
	}

	public static <V> List<V> queryFixedNumberRowsInListBean(Class<V> bean,
			String query, int rowNumberMax) {

		return queryAllInBeans(bean,
				addRowNumberCondition(query, rowNumberMax));
	}

	private static String addRowNumberCondition(String query, int rowNumberMax) {
		return "select * from ( " + query + ") where rownum<"
				+ Integer.toString(rowNumberMax);
	}

	public static int nextVal(String seqName) {
		return getInteger("SELECT " + seqName + ".nextval FROM DUAL");
	}

	/**
	 * Permet de faire des updates, deletes, insertes par batch.
	 * 
	 * Cette m�thode peut �tre utilis�e typiquement lorsque vous avez plusieurs
	 * requ�tes identiques � effectu�es avec juste des paramatres qui changent.
	 * JDBC compile la requ�te pour gagner en performance.
	 * 
	 * Cette m�thode prend en param�tre un tableau contenant l'ensemble des
	 * tableaux de parametres. Par exemple : <BR>
	 * <ul>
	 * <li>[req1[param1, 'strg1']]</li>
	 * <li>[req2[param2, 'strg2']]</li>
	 * <li>etc..</li>
	 * 
	 * Cette m�thode retourne un tableau d'entiers qui indique le nombres de
	 * lignes impact�es par les requ�tes. Ces valeurs sont retourn�es dans le
	 * m�me ordre que celui du tableau pass� en param�tre.
	 * 
	 * @param query
	 * @param params
	 *            Tableau contenant les tableaux de param�tres pour la requ�te.
	 * @return Tableau contenant le nbre de ligne impact�es pour chaque requ�te
	 *         �x�cut�e.
	 */
	public static int[] updateByBatch(String query, Object[][] params) {
		try {
			return QR.batch(conn(), query, params);
		} catch (SQLException e) {
			log.error("Cant process query", e);
			throw new DbException("Cant process query " + query, e);
		}
	}

	/**
	 * Transforme les java.util.Date en java.sql.Date et les Enum�ration en leur
	 * toString.
	 * 
	 * Le driver jdbc Oracle ne peut pas binder en param�tre de
	 * PreparedStatement des java.util.Date. Tout les param�tres qui ne sont pas
	 * des java.util.Date ou des enums sont laiss�s inchang�s.
	 * 
	 * @param params
	 *            le tableau des param�tres � convertir.
	 * @return un nouveau tableau d'objet ne contenant plus de java.util.Date
	 */
	protected static Object[] fixParams(Object... params) {
		// Dbutils and jdbc won't handle properly java.util.Date as parameters :
		List<Object> fixedParams = newArrayList();
		for (Object o : params) {
			if (o instanceof Date)
				fixedParams.add(new java.sql.Timestamp(((Date) o).getTime()));
			else if (o != null && o.getClass().isEnum())
				fixedParams.add(o.toString());
			else
				fixedParams.add(o);
		}
		return fixedParams.toArray();
	}

	public static class FsQueryRunner extends QueryRunner {
		public FsQueryRunner(DataSource ds) {
			super(ds);
		}

		public FsQueryRunner() {
			super();
		}

		@Override
		protected ResultSet wrap(ResultSet rs) {
			try {
				rs.setFetchSize(2000);
				log.trace("wrapping");
				return super.wrap(rs);
			} catch (SQLException e) {
				throw new DbException("Could not set fetch Size", e);
			}
		}

		@Override
		protected void close(Connection conn) throws SQLException {
			try {
				DbUtils.close(conn);
			} catch (SQLException e) {
				log.warn("Could not close connexion", e);
			}
		}
	}

}
