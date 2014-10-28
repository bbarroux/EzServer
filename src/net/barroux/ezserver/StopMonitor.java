package net.barroux.ezserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread qui écoute sur un port prédéterminé pour déclencher l'arrêt du serveur
 * jetty et de la jvm.
 * 
 * Le thread ouvre une serverSocket sur un port 100 au dessus du port du
 * serveur. lorsque cette serverSocket est contactée, le server jetty est arrêté
 * dans le délai de grace communiqué, et la jvm est arrêtée.
 */

class StopMonitor extends Thread {
   private static final Logger log = LoggerFactory.getLogger(StopMonitor.class);
   private final ServerSocket  socket;
   private final Server        server;

   StopMonitor(Server server, int port) {
      setDaemon(true);
      setName("StopMonitor");
      this.server = server;
      try {
         this.socket = new ServerSocket(port + 100, 1, InetAddress.getByName("127.0.0.1"));
      }
      catch (IOException e) {
         log.error("Could not initialize StopMonitor", e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public void run() {
      try {
         log.debug("StopMonitor ready and waiting for orders");
         // Blocking method. l'exécution est stoppée ci-dessous tant que
         // personne ne parle sur la socket
         Socket accept = socket.accept();
         log.debug("Command connection received");
         Reader raw = new InputStreamReader(accept.getInputStream(), UTF_8);
         BufferedReader reader = new BufferedReader(raw);
         String gDelayS = reader.readLine();
         int graceDelay = gDelayS == null ? 100 : Integer.parseInt(gDelayS);
         doStop(graceDelay);
         log.debug("exiting");
         Runtime.getRuntime().halt(0);
      }
      catch (IOException e) {
         log.error("StopMonitor failed", e);
         throw new RuntimeException(e);
      }
   }

   private void doStop(int graceDelay) {
      server.setStopTimeout(graceDelay);
      server.setStopAtShutdown(true);
      try {
         server.stop();
         log.debug("Server stopped");
      }
      catch (Exception e) {
         log.error("Could not stop properly server", e);
      }

   }

   /**
    * Méthode statique utilitaire pour lancer une commande d'arrêt à StopMonitor
    * en exécution.
    * 
    * A noter que dans le cas d'utilisation courant, cette méthode qui ouvre une
    * socket sur le port surlequel pourrait écouter un StopMonitor se retrouve
    * de
    * fait à communiquer avec un StopMonitor qui s'exécute dans une autre JVM.
    * 
    * @param port
    *           le port du serveur jetty qu'on souhaite lancer.
    * @param graceDelay
    *           Le délai de grace (en ms) qu'on veut donner au serveur jetty
    *           pour
    *           s'arrêter
    */
   static void sendStopCommand(int port, int graceDelay) {
      try {
         Socket s = new Socket(InetAddress.getByName("127.0.0.1"), port + 100);
         try {
            OutputStream out = s.getOutputStream();
            out.write((graceDelay + "\r\n").getBytes(UTF_8));
            out.flush();
            log.info("Sending stop command");
            boolean connected = true;
            while (connected) {
               try {
                  // waiting for socket to be closed by server
                  out.write(("whatever \r\n").getBytes(UTF_8));
                  out.flush();
                  Thread.sleep(10);
               }
               catch (IOException e) {
                  log.info("Server has processed command");
                  connected = false;
               }
            }
         }
         finally {
            s.close();
         }
      }
      catch (Exception e) {
         String es = e.getClass().getName() + " - " + e.getMessage();
         log.info("Couldn't issue command to server - Probably not running ." + es);
      }
   }
}
