package net.barroux.ezserver;

import org.bibeault.frontman.CommandBroker;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for EzServer with Frontman framework.
 * 
 * Exemple usage :
 * {@code new EzServer("net.bx.commands").port(7777).context("myapp").start();}
 * 
 * Note that starting a server will trigger the shutdown of any other server
 * running on the same port. This is handy for quickly simulate a server
 * "restart"
 * 
 * The frontman commandBroker is mapped on "/cmd/*" urls.
 */

public class EzServer {
   private static final Logger log        = LoggerFactory.getLogger(EzServer.class);
   private final String        commandsPath;
   private int                 port       = 8765;
   private String              context    = "";
   private String              webContent = "WebContent";
   private String              classesDir = "build/bin";
   private String              viewsPath  = "/WEB-INF/jsp";

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
    * Fluent setter for application context path (defaults to root ("/"))
    */
   public EzServer context(String context) {
      this.context = context;
      return this;
   }

   /**
    * Fluent setter for web content eclipse folder relative to project
    * path (defaults to "WebContent").
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

   public void start() throws Exception {
      log.info("preparing server start on port {} ", port);
      Server server = new Server(port);

      WebAppContext app = new WebAppContext(webContent + "/", "/" + context);
      ServletHolder cmdBroker = new ServletHolder("CommandBroker", CommandBroker.class);
      cmdBroker.setInitParameter("commandsPath", commandsPath);
      cmdBroker.setInitParameter("viewsPath", viewsPath);
      app.addServlet(cmdBroker, "/cmd/*");
      app.setExtraClasspath(classesDir);
      server.setHandler(app);
      StopMonitor.sendStopCommand(port, 2000);
      new StopMonitor(server, port).start();
      server.start();
      log.info("Server started");
   }
}