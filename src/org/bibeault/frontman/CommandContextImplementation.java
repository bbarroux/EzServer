/*
 * Copyright (c) 2006, Bear Bibeault
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - The name of Bear Bibeault may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * This software is provided by the copyright holders and contributors "as is"
 * and any express or implied warranties, including, but not limited to, the
 * implied warranties of merchantability and fitness for a particular purpose
 * are disclaimed. In no event shall the copyright owner or contributors be
 * liable for any direct, indirect, incidental, special, exemplary, or
 * consequential damages (including, but not limited to, procurement of
 * substitute goods or services; loss of use, data, or profits; or business
 * interruption) however caused and on any theory of liability, whether in
 * contract, strict liability, or tort (including negligence or otherwise)
 * arising in any way out of the use of this software, even if advised of the
 * possibility of such damage.
 */
package org.bibeault.frontman;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A context object that maintains the state information for the invocation of a command,
 * as well as making various useful methods available to Command implementations.
 * <p>
 * The state information includes the current request and response instances, as well as a reference back to
 * the invoking broker.
 * </p>
 * <p>
 * A CommandContext is considered valid only for the duration of an invocation of the execute() method of the
 * {@link Command} interface. A reference to the CommandContext should never be retained beyond the scope of that
 * method, for example, by storing it in a scoped variable.
 * </p>
 */
public final class CommandContextImplementation implements CommandContext {

  private static final String ENCODING = "UTF-8";

  private CommandBroker commandBroker;
  private HttpServletRequest request;
  private HttpServletResponse response;

  public CommandContextImplementation(CommandBroker commandBroker, HttpServletRequest request, HttpServletResponse response) {
    this.commandBroker = commandBroker;
    this.request = request;
    this.response = response;
  }

  /**
   * @return the Command Broker instance
   */
  public CommandBroker getCommandBroker() {
    return this.commandBroker;
  }

  /**
   * @return the current servlet request instance.
   */
  public HttpServletRequest getRequest() { return this.request; }

  /**
   * @return the current response.
   */
  public HttpServletResponse getResponse() { return this.response; }

  /**
   * @return the current session instance.
   */
  public HttpSession getSession() {
    return getRequest().getSession();
  }

  /**
   * @return the servlet context.
   */
  public ServletContext getServletContext() {
    return this.commandBroker.getServletContext();
  }

  /**
   * Returns the path info for the current request. This method is
   * sensitive to requests initiated via an include and will return
   * the path info of the originating request.
   *
   * @return the determined path info
   */
  public String getPathInfo() {
    String includeUri = (String)this.request.getAttribute("javax.servlet.include.request_uri");
    if (includeUri == null) {
      return this.request.getPathInfo();
    }
    else {
      return (String)this.request.getAttribute("javax.servlet.include.path_info");
    }
  }

  /**
   * Sets a scoped variable into the specified context.
   * <p>
   * This is a convenience method that shortens the syntax of setting scoped variables into their
   * contexts as well as providing imporoved semantics.
   * </p>
   *
   * @param name  the name of the scoped variable
   * @param value the value of the scoped variable
   * @param scope one of SCOPE_REQUEST, SCOPE_SESSION or SCOPE_APPLICATION
   * @throws javax.servlet.ServletException if the specified scope is not a valid value
   * @throws NullPointerException           if <code>name</code> is null
   */
  public void setScopedVariable(Object name, Object value, ScopedContext scope) throws ServletException, NullPointerException {
    if (name == null)
      throw new NullPointerException("a scoped variable's name cannot be null");
    switch (scope) {
      case REQUEST:
        getRequest().setAttribute(name.toString(), value);
        break;
      case SESSION:
        getSession().setAttribute(name.toString(), value);
        break;
      case APPLICATION:
        getServletContext().setAttribute(name.toString(), value);
        break;
      default:
        throw new ServletException("The scope value " + scope + " is invalid");
    }
  }

  /**
   * Sets a scoped variable into the request context.
   * <p>
   * This is a convenience method that shortens the syntax of setting scoped
   * variables into request context.
   * </p>
   *
   * @param name  the name of the scoped variable
   * @param value the value of the scoped variable
   * @throws NullPointerException           if <code>name</code> is null
   * @throws javax.servlet.ServletException if anything else fails
   */
  public void setScopedVariable(Object name, Object value) throws ServletException {
    setScopedVariable(name, value, ScopedContext.REQUEST);
  }

  /**
   * Gets a scoped variable from the specified context. The name can be proviede as any
   * object type whose string equivalent is used as the name.
   *
   * @param name  the name of the scoped variable
   * @param scope the scoped context in which to look
   * @return the located object, or null if not found
   */
  public Object getScopedVariable(Object name, ScopedContext scope) throws ServletException {
    if (name == null)
      throw new NullPointerException("a scoped variable's name cannot be null");
    switch (scope) {
      case REQUEST:
        return getRequest().getAttribute(name.toString());
      case SESSION:
        return getSession().getAttribute(name.toString());
      case APPLICATION:
        return getServletContext().getAttribute(name.toString());
      default:
        throw new ServletException("The scope value " + scope + " is invalid");
    }
  }

  /**
   * Finds a scoped variable. The name can be proviede as any
   * object type whose string equivalent is used as the name.
   *
   * @param name the name of the scoped variable
   * @return the located object, or null if not found
   */
  public Object findScopedVariable(Object name) {
    Object value = getRequest().getAttribute(name.toString());
    if (value == null)
      value = getSession().getAttribute(name.toString());
    if (value == null)
      value = getServletContext().getAttribute(name.toString());
    return value;
  }

  /**
   * Forwards the current request to the resource at the specified path.
   * <p>
   * The semantics of <code>path</code> are the same as that required by the <code>javax.servlet.RequestDispatcher.forward()</code>
   * method.
   * </p>
   *
   * @param path the path of the resource to whcih the request is to be forwarded
   * @throws java.io.IOException   if the request dispatcher throws this exception
   * @throws ServletException      if the request dispatcher throws this exception
   * @throws IllegalStateException if the response is already committed
   * @see javax.servlet.RequestDispatcher
   */
  public void forward(String path) throws IOException, ServletException, IllegalStateException {
    RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path);
    if (dispatcher == null)
      throw new ServletException("Could not obtain a dispatcher for path " + path);
    dispatcher.forward(this.request, this.response);
  }

  /**
   * Sends a temporary redirect response to the client using the specified redirect location URL.
   * <p>
   * The semantics of <code>location</code> are the same as that required by the
   * <code>javax.servlet.http.HttpServletResponse.sendRedirect()</code> method.
   * </p>
   * <p>
   * This method automatically ensures that the redirect is properly encoded to ensure that the session id is
   * included in the URL if necessary.
   * </p>
   *
   * @param location the location of the redirect resource
   * @throws IOException           If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed, or the location cannot be made into a valid URL
   * @see javax.servlet.http.HttpServletResponse
   */
  public void redirect(String location) throws IOException, IllegalStateException {
    this.response.sendRedirect(this.response.encodeRedirectURL(location));
  }

  /**
   * Sends a temporary redirect response to the client using the specified redirect location URL.
   * <p>
   * The semantics of <code>location</code> are the same as that required by the
   * <code>javax.servlet.http.HttpServletResponse.sendRedirect()</code> method.
   * </p>
   * <p>
   * This method automatically ensures that the redirect is properly encoded to ensure that the session id is
   * included in the URL if necessary.
   * </p>
   *
   * @param location   the location of the redirect resource
   * @param parameters a Map of the request parameters to add the URL as a query string
   * @throws IOException           If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed, or the location cannot be made into a valid URL
   * @see javax.servlet.http.HttpServletResponse
   */
  public void redirect(String location, Map<String,String[]> parameters) throws IOException, IllegalStateException {
    this.response.sendRedirect(this.response.encodeRedirectURL(location + makeQueryString(parameters)));
  }

  /**
   * Sends an error response to the client with the specified status code.
   * <p>
   * This method is a thin wrapper around the
   * <code>javax.servlet.http.HttpServletResponse.sendError()</code> method.
   * </p>
   *
   * @param statusCode one of the <code>SC_</code> codes defined by <code>javax.servlet.http.HttpServletResponse</code>
   * @throws IOException           If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed
   */
  public void sendError(int statusCode) throws IOException, IllegalStateException {
    this.response.sendError(statusCode);
  }

  /**
   * Convenience wrapper around the {@link #forward(String)} method that forwards to the command specified by the passed
   * command verb.
   *
   * @param verb the verb for the forward command
   * @throws IOException      see  {@link #forward(String)}
   * @throws ServletException see {@link #forward(String)}
   */
  public void forwardToCommand(String verb) throws IOException, ServletException {
    forward(new StringBuilder().append(getRequest().getServletPath()).append('/').append(verb).toString());
  }

  /**
   * Convenience wrapper around the {@link #forward(String)} method that forwards to the view specified by the passed
   * view name.
   *
   * @param viewName the name for the forward view
   * @throws IOException      see  {@link #forward(String)}
   * @throws ServletException see {@link #forward(String)}
   */
  public void forwardToView(String viewName) throws IOException, ServletException {
    forward(this.commandBroker.findViewPath(viewName));
  }

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the command specified by the
   * passed command verb.
   *
   * @param verb the verb for the redirect command
   * @throws IOException      see  {@link #redirect(String)}
   * @throws ServletException see {@link #redirect(String)}
   */
  public void redirectToCommand(String verb) throws IOException, ServletException {
    redirect(makeCommandURL(verb, null));
  }

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the command specified by the
   * passed command verb, with one or more request parameters.
   *
   * @param verb       the verb for the redirect command
   * @param parameters a Map of the request parameters to add the URL as a query string
   * @throws IOException      see  {@link #redirect(String)}
   * @throws ServletException see {@link #redirect(String)}
   */
  public void redirectToCommand(String verb, Map<String,String[]> parameters) throws IOException, ServletException {
    redirect(makeCommandURL(verb, parameters));
  }

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the view specified by the
   * passed view name.
   *
   * @param viewName the name for the redirect view
   * @throws IOException      see  {@link #redirect(String)}
   * @throws ServletException see {@link #redirect(String)}
   */
  public void redirectToView(String viewName) throws IOException, ServletException {
    redirect(makeViewURL(viewName, null));
  }

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the view specified by the
   * passed view name, with one or more request parameters.
   *
   * @param viewName   the name for the redirect view
   * @param parameters a Map of the request parameters to add the URL as a query string
   * @throws IOException      see  {@link #redirect(String)}
   * @throws ServletException see {@link #redirect(String)}
   */
  public void redirectToView(String viewName, Map<String,String[]> parameters) throws IOException, ServletException {
    redirect(makeViewURL(viewName, parameters));
  }

  private String makeCommandURL(String verb, Map<String,String[]> params) throws ServletException {
    return new StringBuilder()
            .append(getRequest().getContextPath())
            .append(getRequest().getServletPath())
            .append('/')
            .append(verb)
            .append(makeQueryString(params))
            .toString();
  }

  private String makeViewURL(String viewName, Map<String,String[]> params) throws ServletException {
    return new StringBuilder()
            .append(getRequest().getContextPath())
            .append(this.commandBroker.findViewPath(viewName))
            .append(makeQueryString(params))
            .toString();
  }

  private String makeQueryString(Map<String,String[]> paramMap) {
    StringBuilder queryString = new StringBuilder();
    if ((paramMap != null) && (paramMap.size() > 0)) {
      boolean first = true;
      for (Map.Entry<String,String[]> entry : paramMap.entrySet()) {
        try {
          for (String value : entry.getValue()) {
            queryString.append(first ? '?' : '&')
                    .append(entry.getKey())
                    .append('=')
                    .append(URLEncoder.encode(value, ENCODING));
            first = false;
          }
        }
        catch (UnsupportedEncodingException uee) {
          throw new IllegalArgumentException("Error attempting to encode parameter value to " + ENCODING, uee);
        }
      }
    }
    return queryString.toString();
  }

}
