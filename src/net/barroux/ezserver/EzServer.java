package net.barroux.ezserver;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import net.barroux.ezserver.db.DbConfig;
import net.barroux.ezserver.db.DbHelper;
import net.barroux.ezserver.filters.LogRequestFilter;
import net.barroux.ezserver.filters.SentryFilter;
import net.barroux.ezserver.filters.SentryFilter.Identifier;
import net.barroux.ezserver.filters.TransactionFilter;

import org.apache.commons.lang.SystemUtils;
import org.bibeault.frontman.CommandBroker;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for EzServer with Frontman framework.
 * 
 * Exemple usage : {@code new EzServer("net.bx.commands").port(7777).context("myapp").start();}
 * 
 * Note that starting a server will trigger the shutdown of any other server
 * running on the same port. This is handy for quickly simulate a server
 * "restart"
 * 
 * The frontman commandBroker is mapped on "/cmd/*" urls.
 */

public class EzServer {
   private static final Logger           log           = LoggerFactory.getLogger(EzServer.class);
   private static final String           JETTY_DEFAULT = "org.eclipse.jetty.servlet.Default.";
   private final String                  commandsPath;
   private int                           port          = 8765;
   private String                        context       = "";
   private String                        webContent    = "WebContent";
   private String                        classesDir    = "build/bin";
   private String                        viewsPath     = "/WEB-INF/jsp";
   private List<Class<? extends Filter>> filters       = new ArrayList<>();
   private List<Class<?>>                webSockets    = new ArrayList<>();
   private DbConfig                      dbConfig;
   private Identifier                    identifier;
   private Map<String, Object>           attributes;

   /**
    * Initializing an EzServer with the only parameter without default.
    * 
    * @param commandsPath
    *           The base package for your frontman commands.
    */
   public EzServer(String commandsPath) {
      this.commandsPath = commandsPath;
   }

   /**
    * Fluent setter for Server frontman viewsPath (defaults to /WEB-INF/jsp).
    */
   public EzServer viewsPath(String viewsPath) {
      this.viewsPath = viewsPath;
      return this;
   }

   /**
    * Fluent setter for server port (defaults to 8765)
    */
   public EzServer port(int port) {
      this.port = port;
      return this;
   }

   /**
    * Fluent setter for attributes to be added to servletContext
    */
   public EzServer attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
   }

   /**
    * Fluent setter for filters to be added to webapp
    */
   @SafeVarargs
   public final EzServer filters(Class<? extends Filter>... filters) {
      this.filters.addAll(Arrays.asList(filters));
      return this;
   }

   /**
    * Fluent setter for application context path (defaults to root ("/"))
    */
   public EzServer context(String context) {
      this.context = context;
      return this;
   }

   /**
    * Fluent setter for web content eclipse folder relative to project path
    * (defaults to "WebContent").
    */
   public EzServer webContent(String webContent) {
      this.webContent = webContent;
      return this;
   }

   /**
    * Fluent setter for eclipse classes output dir (defaults to "build/bin").
    */
   public EzServer classesDir(String classesDir) {
      this.classesDir = classesDir;
      return this;
   }

   /**
    * Fluent setter for eclipse classes output dir (defaults to "build/bin").
    */
   public EzServer webSockets(Class<?>... webSockets) {
      this.webSockets.addAll(Arrays.asList(webSockets));
      return this;
   }

   /**
    * Fluent setter for DbConfig.
    */
   public EzServer dbConfig(DbConfig dbConfig) {
      this.dbConfig = dbConfig;
      return this;
   }

   /**
    * Fluent setter for sentry identifier.
    */
   public EzServer identifier(Identifier identifier) {
      this.identifier = identifier;
      return this;
   }

   public void start() throws Exception {
      log.info("preparing server start on port {} ", port);
      // For some reason, oracle jdbc driver won't let me connect if
      // I'm using jdk instead of jre (which I need for jsp)...sigh
      // unless... bouncyCastle to the rescue!
      Security.addProvider(new BouncyCastleProvider());
      Server server = new Server(port);

      WebAppContext app = new WebAppContext(webContent + "/", "/" + context);
      server.setHandler(app);
      setWebSockets(app);

      app.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
      ServletHolder cmdBroker = getCommandBroker();

      String pathSpec = "/cmd/*";
      app.addServlet(cmdBroker, pathSpec);
      app.setExtraClasspath(classesDir);
      app.setInitParameter(JETTY_DEFAULT + "welcomeServlets", true + "");
      // on renvoie sur le welcome servlet/page par un redirecthttp
      app.setInitParameter(JETTY_DEFAULT + "redirectWelcome", true + "");
      if (SystemUtils.IS_OS_WINDOWS) {
         // On désactive l'utilisation des filemappedBuffer sur windows sans
         // quoi les fichiers sous-jacent sont vérouillés)
         app.setInitParameter(JETTY_DEFAULT + "useFileMappedBuffer", false + "");
      }

      EnumSet<DispatcherType> dts = EnumSet.of(DispatcherType.REQUEST);
      app.addFilter(LogRequestFilter.class, pathSpec, dts);
      if (dbConfig != null) {
         DbHelper.init(dbConfig);
         app.addFilter(TransactionFilter.class, pathSpec, dts);
      }
      if (identifier != null) {
         app.setAttribute("identifier", identifier);
         app.addFilter(SentryFilter.class, pathSpec, dts);
      }
      filters.stream().forEach(f -> app.addFilter(f, pathSpec, dts));

      StopMonitor.sendStopCommand(port, 2000);
      new StopMonitor(server, port).start();

      server.start();
      server.dump(System.out);
      log.info("Server started");
   }

   private void setWebSockets(WebAppContext app) throws ServletException, DeploymentException {
      if (!webSockets.isEmpty()) {
         ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(app);
         for (Class<?> webSocket : webSockets) {
            wscontainer.addEndpoint(webSocket);
         }
      }
   }

   private ServletHolder getCommandBroker() {
      ServletHolder cmdBroker = new ServletHolder("CommandBroker", CommandBroker.class);
      cmdBroker.setInitParameter("commandsPath", commandsPath);
      cmdBroker.setInitParameter("viewsPath", viewsPath);
      return cmdBroker;
   }
}
