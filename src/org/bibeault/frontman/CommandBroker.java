/*
 * Copyright (c) 2006-2009, Bear Bibeault
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - The name of Bear Bibeault may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.bibeault.frontman.utensils.ClassFinder;

/**
 * A simple Front Controller using a simple "configuration by convention"
 * mechanism to locate
 * Command classes and View resources. Optionally, properties files can be used
 * to define explicit
 * command and view mappings.
 * <p>
 * See the description of the {@link org.bibeault.frontman} package for details
 * on how to establish this servlet in the deployment descriptor.
 * </p>
 * <p>
 * Notes:
 * <ol>
 * <li>
 * Upon init, this servlet will record any command and view paths, and attempt
 * to load any properties files, specified by the init parameters in the
 * &lt;servlet&gt; element in the deplyment descriptor.</li>
 * <li>
 * GET and POST requests are handled equally by this servlet.</li>
 * </ol>
 * </p>
 */
@Slf4j
public class CommandBroker extends HttpServlet {

   private static final String INIT_PARAM_COMMANDS_ROOT            = "commandsPath";
   private static final String INIT_PARAM_VIEWS_ROOT               = "viewsPath";

   private static final String INIT_PARAM_COMMAND_VERBS_PROPERTIES = "commandVerbsProperties";
   private static final String INIT_PARAM_VIEW_NAMES_PROPERTIES    = "viewNamesProperties";

   private String              commandsPathRoot;
   private String              viewsPathRoot;
   private Properties          commandVerbProperties;
   private Properties          viewNameProperties;
   private Map<String, Class>  annotatedCommands;

   public void init() throws ServletException {
      log.info("Initializing...");
      //
      // Collect and validate init param values
      //
      this.commandsPathRoot = getServletConfig().getInitParameter(INIT_PARAM_COMMANDS_ROOT);
      this.viewsPathRoot = getServletConfig().getInitParameter(INIT_PARAM_VIEWS_ROOT);
      String commandVerbsPropertiesPath = getServletConfig().getInitParameter(INIT_PARAM_COMMAND_VERBS_PROPERTIES);
      String viewNamesPropertiesPath = getServletConfig().getInitParameter(INIT_PARAM_VIEW_NAMES_PROPERTIES);
      if (log.isDebugEnabled()) {
         log.debug(INIT_PARAM_COMMAND_VERBS_PROPERTIES + "=" + commandVerbsPropertiesPath);
         log.debug(INIT_PARAM_COMMANDS_ROOT + "=" + this.commandsPathRoot);
         log.debug(INIT_PARAM_VIEW_NAMES_PROPERTIES + "=" + viewNameProperties);
         log.debug(INIT_PARAM_VIEWS_ROOT + "=" + this.viewsPathRoot);
      }
      //
      // Make sure that at least one way of locating command classes has been
      // specified
      //
      if (commandVerbsPropertiesPath == null && this.commandsPathRoot == null) {
         throw new UnavailableException("At least one of init parameters "
                                        + INIT_PARAM_COMMANDS_ROOT
                                        + " or "
                                        + INIT_PARAM_COMMAND_VERBS_PROPERTIES
                                        + " must be provided");
      }
      //
      // Get any resource bundle for command verbs and view names
      //
      if (commandVerbsPropertiesPath != null) this.commandVerbProperties = getPropertiesFrom(commandVerbsPropertiesPath);
      if (viewNamesPropertiesPath != null) this.viewNameProperties = getPropertiesFrom(viewNamesPropertiesPath);
      //
      // If a command verb path was provided, find all command classes
      // (those that are annotated with FrontmanCommand).
      //
      this.annotatedCommands = findAnnotatedCommands();
      //
      // If logging is enabled, dump the results of the resource loading
      //
      if (log.isInfoEnabled()) {
         if (this.commandsPathRoot != null) log.info("  Commands path root: " + this.commandsPathRoot);
         if (this.commandVerbProperties != null) {
            log.info("  Command verbs: (loaded from " + commandVerbsPropertiesPath + ")");
            Enumeration<?> keys = this.commandVerbProperties.propertyNames();
            while (keys.hasMoreElements()) {
               Object key = keys.nextElement();
               log.info("    " + key + "=" + this.commandVerbProperties.getProperty(key.toString()));
            }
         }
         if (this.annotatedCommands.size() > 0) {
            log.info("  Command classes: (annotated in package " + this.commandsPathRoot + ")");
            for (Map.Entry<String, Class> entry : this.annotatedCommands.entrySet()) {
               log.info("    " + entry.getKey() + "=" + entry.getValue());
            }
         }
         if (this.viewsPathRoot != null) log.info("  Views path root: " + this.viewsPathRoot);
         if (this.viewNameProperties != null) {
            log.info("  View names: (loaded from " + viewNamesPropertiesPath + ")");
            Enumeration<?> keys = this.viewNameProperties.propertyNames();
            while (keys.hasMoreElements()) {
               Object key = keys.nextElement();
               log.info("    " + key + "=" + this.viewNameProperties.getProperty(key.toString()));
            }
         }
      }
      log.info("Done initializing.");
   }

   private Map<String, Class> findAnnotatedCommands() throws UnavailableException {
      Map<String, Class> commands = new HashMap<String, Class>();
      if (this.commandsPathRoot != null) {
         try {
            List<Class> candidateClasses = ClassFinder.findInterfaceImplementors(this.commandsPathRoot,
                                                                                 Command.class);
            System.out.println("found " + candidateClasses.size() + " Command implementors");
            for (Class candidate : candidateClasses) {
               FrontmanCommand annotation = (FrontmanCommand) candidate.getAnnotation(FrontmanCommand.class);
               if (annotation != null) commands.put(annotation.value(), candidate);
            }
         }
         catch (Exception e) {
            throw new UnavailableException("Error while searching "
                                           + this.commandsPathRoot
                                           + " for annotated commands: ("
                                           + e.getClass().getName()
                                           + ") "
                                           + e.getMessage());
         }
      }
      return commands;
   }

   private Properties getPropertiesFrom(String path) throws UnavailableException {
      InputStream inputStream = getServletContext().getResourceAsStream(path);
      if (inputStream == null) {
         log.error("The properties resource at path " + path + " could not be found");
         throw new UnavailableException("The properties resource at path " + path + " could not be found");
      }
      try {
         Properties properties = new Properties();
         properties.load(inputStream);
         return properties;
      }
      catch (IOException e) {
         throw new UnavailableException("I/O error: " + e.getMessage());
      }
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      doPost(request, response);
   }

   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      boolean debug = log.isDebugEnabled();
      if (debug) {
         log.debug("Begin brokering...");
         log.debug("  request uri: " + request.getRequestURI());
      }
      //
      // Create the command context
      //
      CommandContext commandContext = new CommandContextImplementation(this, request, response);
      //
      // Get the command verb from the path info and fetch the corresponding
      // command class
      //
      String pathInfo = commandContext.getPathInfo();
      if (pathInfo == null) throw new CommandNotFoundException(null);
      String commandVerb = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
      int closingSlashIndex = commandVerb.indexOf("/");
      if (closingSlashIndex != -1) commandVerb = commandVerb.substring(0, closingSlashIndex);
      if (debug) {
         log.debug("  path info: " + pathInfo);
         log.debug("  command verb: " + commandVerb);
      }
      Class commandClass = findCommandClass(commandVerb);
      if (debug) log.debug("  command class name: " + commandClass.getName());
      //
      // Create an instance of the command
      //
      Command command;
      try {
         Object commandObject = commandClass.newInstance();
         if (!(commandObject instanceof Command)) throw new ClassCastException("Class "
                                                                               + commandClass.getName()
                                                                               + " does not implement "
                                                                               + Command.class);
         command = (Command) commandObject;
      }
      catch (Exception e) {
         throw new CommandNotFoundException("Could not create command instance for command with verb"
                                            + commandVerb
                                            + "; command class was "
                                            + commandClass.getName(), e);
      }
      //
      // Execute the command
      //
      if (log.isInfoEnabled()) log.info("executing command "
                                        + commandVerb
                                        + " ("
                                        + commandClass.getName()
                                        + ")");
      command.execute(commandContext);
      if (debug) log.debug("Done.");
   }

   Class findCommandClass(String commandVerb) throws CommandNotFoundException {
      boolean debug = log.isDebugEnabled();
      if (debug) log.debug("Locating command class for verb: " + commandVerb);
      Class commandClass = null;
      //
      // If command mapping properties were supplied, try there first
      //
      if (this.commandVerbProperties != null) {
         if (debug) log.debug("  trying command properties...");
         String commandClassName = this.commandVerbProperties.getProperty(commandVerb);
         if (commandClassName != null) {
            try {
               commandClass = Class.forName(commandClassName);
            }
            catch (ClassNotFoundException e) {
               throw new CommandNotFoundException("Could not locate class for commandVerb "
                                                  + commandVerb
                                                  + " as defined by the command properties:"
                                                  + e.getMessage(), e);
            }
            if (debug && commandClass != null) log.debug("    found!");
         }
      }
      //
      // If not found, see if an annotated command class had been found
      //
      if (commandClass == null) {
         if (debug) log.debug("  trying annotated commands...");
         commandClass = this.annotatedCommands.get(commandVerb);
         if (debug && commandClass != null) log.debug("    found!");
      }
      //
      // If still not found, try the conventional command path
      //
      if (commandClass == null && this.commandsPathRoot != null) {
         if (debug) log.debug("  trying command path: " + this.commandsPathRoot);
         String commandClassName = new StringBuilder().append(this.commandsPathRoot).append('.').append(adjustVerb(commandVerb)).append("Command").toString();
         log.debug("    command class name: " + commandClassName);
         try {
            commandClass = Class.forName(commandClassName);
            log.debug("      found");
         }
         catch (ClassNotFoundException e) {
            log.debug("      not found");
         }
      }
      //
      // If still not located, throw our hands up in disgust
      //
      if (commandClass == null) throw new CommandNotFoundException("Could not associate a Command implementation with command verb "
                                                                   + commandVerb);
      return commandClass;
   }

   private static final Pattern PATTERN = Pattern.compile("(.+\\.)*((\\w)+)");

   private String adjustVerb(String commandVerb) throws CommandNotFoundException {
      Matcher matcher = PATTERN.matcher(commandVerb);
      if (matcher.matches()) {
         StringBuilder verb = new StringBuilder();
         String prefix = matcher.group(1);
         commandVerb = matcher.group(2);
         if (prefix != null) verb.append(prefix);
         verb.append(Character.toUpperCase(commandVerb.charAt(0)));
         verb.append(commandVerb.substring(1));
         return verb.toString();
      }
      else {
         throw new CommandNotFoundException("Command verb "
                                            + commandVerb
                                            + " does not match required pattern");
      }
   }

   String findViewPath(String viewName) throws ViewNotFoundException {
      boolean debug = log.isDebugEnabled();
      if (debug) log.debug("Finding view path for name: " + viewName + "...");
      String viewPath = null;
      //
      // If a view mapping bundle was supplied try it first
      //
      if (debug) log.debug("  looking in view properties");
      if (this.viewNameProperties != null) {
         try {
            viewPath = this.viewNameProperties.getProperty(viewName);
            if (debug) log.debug("    found view path: " + viewPath);
         }
         catch (MissingResourceException mre) {
            // eat the exception as we'll try the view path next
         }
      }
      else {
         if (debug) log.debug("    no view properties defined");
      }
      //
      // If not found, try the view path
      //
      if (viewPath == null) {
         if (debug) log.debug("  trying views path root");
         if (this.viewsPathRoot != null) {
            if (debug) log.debug("    views path root: " + this.viewsPathRoot);
            viewPath = new StringBuilder().append(this.viewsPathRoot).append('/').append(viewName).append(".jsp").toString();
            if (debug) log.debug("    candidate path: " + viewPath);
            try {
               if (this.getServletContext().getResource(viewPath) == null) viewPath = null;
               if (debug) log.debug((viewPath == null) ? "      does not exist" : "      exists");
            }
            catch (MalformedURLException e) {
               throw new ViewNotFoundException(viewName, e);
            }
         }
      }
      //
      // Either throw an exception if not found, or return it
      //
      if (viewPath == null) throw new ViewNotFoundException(viewName);
      log.debug("  Resolved view path: " + viewPath);
      return viewPath;
   }

}
