/*
 * Copyright (c) 2006-2009, Bear Bibeault
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
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ViewBroker extends HttpServlet {

  private static final String INIT_PARAM_VIEWS_ROOT = "viewsRoot";

  public static final String VAR_VIEW_KEY = "viewKey";

  private String viewsRoot = "/WEB-INF/pages";

  public void init(ServletConfig servletConfig) {
    String value = servletConfig.getInitParameter(INIT_PARAM_VIEWS_ROOT);
    if (value != null) this.viewsRoot = value;
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    request.setAttribute(VAR_VIEW_KEY,request.getPathInfo());
    String url = this.viewsRoot + request.getPathInfo() + ".jsp";
    request.getRequestDispatcher(url).forward(request,response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    doGet(request,response);
  }

}
