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
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Defines the interface for an object instance passed to the execute() method of a
 * Command instance. Thw CommandContext not only provides access to context information
 * such as the request and response, but also provides some handy convenience methods
 * for performing common actions such as seting scoped variables and dispatching to
 * other resources.
 */
public interface CommandContext {

  /**
   * @return the Command Broker instance
   */
  CommandBroker getCommandBroker();

  /**
   * @return the current servlet request instance.
   */
  HttpServletRequest getRequest();

  /**
   * @return the current response.
   */
  HttpServletResponse getResponse();

  /**
   * @return the current session instance.
   */
  HttpSession getSession();

  /**
   * @return the servlet context.
   */
  ServletContext getServletContext();

  /**
   * Returns the path info for the current request. This method is
   * sensitive to requests initiated via an include and will return
   * the path info of the originating request.
   *
   * @return the determined path info
   */
  String getPathInfo();

  /**
   * Sets a scoped variable into the specified context.
   * <p>
   * This is a convenience method that shortens the syntax of setting scoped variables into their
   * contexts as well as providing improved semantics. The name can be provided as any object
   * whose string value is used as the name.
   * </p>
   *
   * @param name  the name of the scoped variable
   * @param value the value of the scoped variable
   * @param scope one of SCOPE_REQUEST, SCOPE_SESSION or SCOPE_APPLICATION
   * @throws javax.servlet.ServletException if the specified scope is not a valid value
   * @throws NullPointerException           if <code>name</code> is null
   */
  void setScopedVariable(Object name, Object value, ScopedContext scope) throws ServletException, NullPointerException;

  /**
   * Sets a scoped variable into the request context.
   * <p>
   * This is a convenience method that shortens the syntax of setting scoped
   * variables into request context. The name can be provided as any object
   * whose string value is used as the name.
   * </p>
   *
   * @param name  the name of the scoped variable
   * @param value the value of the scoped variable
   * @throws javax.servlet.ServletException if the specified scope is not a valid value
   * @throws NullPointerException           if <code>name</code> is null
   */
  void setScopedVariable(Object name, Object value) throws ServletException;

  /**
   * Gets a scoped variable from the specified context. The name can be proviede as any
   * object type whose string equivalent is used as the name.
   *
   * @param name    the name of the scoped variable
   * @param context the scoped context in which to look
   * @return the located object, or null if not found
   * @throws javax.servlet.ServletException if anything goes awry
   */
  Object getScopedVariable(Object name, ScopedContext context) throws ServletException;

  /**
   * Finds a scoped variable. The name can be proviede as any
   * object type whose string equivalent is used as the name.
   *
   * @param name the name of the scoped variable
   * @return the located object, or null if not found
   */
  Object findScopedVariable(Object name);

  /**
   * Forwards the current request to the resource at the specified path.
   * <p>
   * The semantics of <code>path</code> are the same as that required by the <code>javax.servlet.RequestDispatcher.forward()</code>
   * method.
   * </p>
   *
   * @param path the path of the resource to whcih the request is to be forwarded
   * @throws java.io.IOException            if the request dispatcher throws this exception
   * @throws javax.servlet.ServletException if the request dispatcher throws this exception
   * @throws IllegalStateException          if the response is already committed
   * @see javax.servlet.RequestDispatcher
   */
  void forward(String path) throws IOException, ServletException, IllegalStateException;

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
   * @throws java.io.IOException   If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed, or the location cannot be made into a valid URL
   * @see javax.servlet.http.HttpServletResponse
   */
  void redirect(String location) throws IOException, IllegalStateException;

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
   * @throws java.io.IOException   If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed, or the location cannot be made into a valid URL
   * @see javax.servlet.http.HttpServletResponse
   */
  void redirect(String location, Map<String,String[]> parameters) throws IOException, IllegalStateException;

  /**
   * Sends an error response to the client with the specified status code.
   * <p>
   * This method is a thin wrapper around the
   * <code>javax.servlet.http.HttpServletResponse.sendError()</code> method.
   * </p>
   *
   * @param statusCode one of the <code>SC_</code> codes defined by <code>javax.servlet.http.HttpServletResponse</code>
   * @throws java.io.IOException   If an input or output exception occurs
   * @throws IllegalStateException if the response is already committed
   */
  void sendError(int statusCode) throws IOException, IllegalStateException;

  /**
   * Convenience wrapper around the {@link #forward(String)} method that forwards to the command specified by the passed
   * command verb.
   *
   * @param verb the verb for the forward command
   * @throws java.io.IOException            see  {@link #forward(String)}
   * @throws javax.servlet.ServletException see {@link #forward(String)}
   */
  void forwardToCommand(String verb) throws IOException, ServletException;

  /**
   * Convenience wrapper around the {@link #forward(String)} method that forwards to the view specified by the passed
   * view name.
   *
   * @param viewName the name for the forward view
   * @throws java.io.IOException            see  {@link #forward(String)}
   * @throws javax.servlet.ServletException see {@link #forward(String)}
   */
  void forwardToView(String viewName) throws IOException, ServletException;

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the command specified by the
   * passed command verb.
   *
   * @param verb the verb for the redirect command
   * @throws java.io.IOException            see  {@link #redirect(String)}
   * @throws javax.servlet.ServletException see {@link #redirect(String)}
   */
  void redirectToCommand(String verb) throws IOException, ServletException;

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the command specified by the
   * passed command verb, with one or more request parameters.
   *
   * @param verb       the verb for the redirect command
   * @param parameters a Map of the request parameters to add the URL as a query string
   * @throws java.io.IOException            see  {@link #redirect(String)}
   * @throws javax.servlet.ServletException see {@link #redirect(String)}
   */
  void redirectToCommand(String verb, Map<String,String[]> parameters) throws IOException, ServletException;

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the view specified by the
   * passed view name.
   *
   * @param viewName the name for the redirect view
   * @throws java.io.IOException            see  {@link #redirect(String)}
   * @throws javax.servlet.ServletException see {@link #redirect(String)}
   */
  void redirectToView(String viewName) throws IOException, ServletException;

  /**
   * Convenience wrapper around the {@link #redirect(String)} method that redirects to the view specified by the
   * passed view name, with one or more request parameters.
   *
   * @param viewName   the name for the redirect view
   * @param parameters a Map of the request parameters to add the URL as a query string
   * @throws java.io.IOException            see  {@link #redirect(String)}
   * @throws javax.servlet.ServletException see {@link #redirect(String)}
   */
  void redirectToView(String viewName, Map<String,String[]> parameters) throws IOException, ServletException;
}
