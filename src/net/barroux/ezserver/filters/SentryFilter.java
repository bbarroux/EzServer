package net.barroux.ezserver.filters;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

public class SentryFilter implements Filter {
	private static final Logger log = LoggerFactory
			.getLogger(SentryFilter.class);
	public static final String USER = "SentryUserAttrName";
	private Identifier identifier;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		log.trace("entering");
		Object user = null;
		HttpServletRequest req = (HttpServletRequest) request;
		HttpSession session = req.getSession(false);
		if (session == null || (user = session.getAttribute("user")) == null) {
			user = identifyUser(req);
			if (user != null) {
				log.debug("User authenticated");
				req.getSession(true).setAttribute("user", user);
				((HttpServletResponse) response).sendRedirect(req
						.getRequestURI());
			} else {
				log.warn("Unauthenticated access ");
				((HttpServletResponse) response).sendRedirect("/");
			}
		}

		else {
			chain.doFilter(request, response);
		}

		log.trace("done");
	}

	private Object identifyUser(HttpServletRequest req) {
		String login = req.getParameter("login");
		String password = req.getParameter("password");
		if (login == null || password == null)
			return null;
		log.debug("Authenticating user {}", login);
		String hashedPassword = hash(login, password);
		return identifier.identify(login, hashedPassword);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		identifier = (Identifier) filterConfig.getServletContext()
				.getAttribute("identifier");
	}

	@Override
	public void destroy() {
	}

	public static interface Identifier {
		public Object identify(String login, String hashedPassword);
	}

	private static String hash(final String login, final String password) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-512");
			final byte[] pwd = digest.digest((login + password).getBytes());
			String hashed = BaseEncoding.base64().encode(pwd);
			return hashed;
		} catch (final NoSuchAlgorithmException e) {
			log.error("Could not get MessageDigest", e);
			throw new RuntimeException("Could not get MessageDigest", e);
		}
	}
}
