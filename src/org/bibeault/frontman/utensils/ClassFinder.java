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
package org.bibeault.frontman.utensils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassFinder {

   public static List<Class> findClassesIn(String packageName) throws Exception {
      //
      // We'll be searching both jar resources and the file structure
      //
      List<Class> classes = new ArrayList<Class>();
      List<File> folders = new ArrayList<File>();
      try {
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         if (classLoader == null) throw new ClassNotFoundException("Can't get class loader.");
         //
         // Look for all resources for the given path
         //
         log.debug("searching by resource...");
         Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));
         while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            log.debug("  resource: " + resource);
            //
            // Get the classes from the jar
            //
            if (resource.getProtocol().equalsIgnoreCase("jar")) {
               JarURLConnection conn = (JarURLConnection) resource.openConnection();
               JarFile jar = conn.getJarFile();
               for (JarEntry entry : Collections.list(jar.entries())) {
                  log.debug("  jar entry: " + entry);
                  if (entry.getName().startsWith(packageName.replace('.', '/'))
                      && entry.getName().endsWith(".class")
                      && !entry.getName().contains("$")) {
                     String className = entry.getName().replace("/", ".").substring(0,
                                                                                    entry.getName().length() - 6);
                     classes.add(Class.forName(className));
                     log.debug("  added: " + className);
                  }
               }
            }
            //
            // Otherwise remember the folder to search through
            //
            else {
               File directory = new File(URLDecoder.decode(resource.getPath(), "UTF-8"));
               folders.add(directory);
               log.debug("  added directory: " + directory);
            }
         }
         log.debug("Done searching by resource...");
      }
      catch (NullPointerException x) {
         throw new ClassNotFoundException(packageName
                                          + " does not appear to be a valid package (Null pointer exception)");
      }
      catch (UnsupportedEncodingException encex) {
         throw new ClassNotFoundException(packageName
                                          + " does not appear to be a valid package (Unsupported encoding)");
      }
      catch (IOException ioex) {
         throw new ClassNotFoundException("IOException was thrown when trying to get all resources for "
                                          + packageName);
      }
      //
      // For every folder identified capture all the .class files and recurse
      // through the structure
      //
      log.debug("searching folders...");
      for (File folder : folders) {
         processFolder(folder, packageName, classes);
      }
      log.debug("done searching folders...");
      // TODO: remove
      for (Class found : classes) {
         log.info("Found class: " + found.getName());
      }
      return classes;
   }

   private static void processFolder(File folder, String packageName, List<Class> classes) throws ClassNotFoundException {
      log.debug("  processing folder: "
                + folder.getName()
                + " at "
                + folder.getAbsolutePath()
                + " package: "
                + packageName);
      if (folder.exists()) {
         //
         // Get the list of the files contained in the package
         //
         File[] files = folder.listFiles();
         for (File file : files) {
            log.debug("    file: " + file);
            //
            //
            //
            if (file.isDirectory()) {
               processFolder(file, packageName + '.' + file.getName(), classes);
            }
            //
            // we are only interested in .class files
            //
            else if (file.getName().endsWith(".class")) {
               //
               // Remove the .class extension
               //
               String filename = file.getName();
               String className = packageName
                                  + '.'
                                  + filename.substring(0, filename.length() - ".class".length());
               classes.add(Class.forName(className));
               log.debug("    added2: " + className);
            }
         }
      }
      else {
         throw new ClassNotFoundException(packageName
                                          + " ("
                                          + folder.getPath()
                                          + ") does not appear to be a valid package");
      }
   }

   public static List<Class> findInterfaceImplementors(String packageName, Class interfaceClass) throws Exception {
      List<Class> classList = new ArrayList<Class>();
      for (Class discovered : findClassesIn(packageName)) {
         log.debug("considering: " + discovered);
         if (Arrays.asList(discovered.getInterfaces()).contains(interfaceClass)) {
            classList.add(discovered);
            log.debug("  accepted!");
         }
      }
      return classList;
   }

   public static void main(String[] args) throws ClassNotFoundException {
      /*
       * org.apache.log4j.PropertyConfigurator.configure( "log4j.properties" );
       * org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger(
       * ClassFinder.class );
       * log4j.setLevel( org.apache.log4j.Level.DEBUG );
       */
      String packageName = args[0];
      String interfaceName = (args.length > 1) ? args[1] : null;
      if (interfaceName != null) {
         log.info("Finding implementations of " + interfaceName + " in " + packageName);
         Class interfaceClass = Class.forName(interfaceName);
         try {
            List<Class> classes = findInterfaceImplementors(packageName, interfaceClass);
            log.info("Implementing classes found:" + classes.size());
            for (Class className : classes) {
               log.info(className.toString());
            }
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      else {
         log.info("Finding classes rooted at package " + packageName);
         try {
            List<Class> classes = findClassesIn(packageName);
            log.info("Classes found:" + classes.size());
            for (Class className : classes) {
               log.info("  " + className.toString());
            }
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

}
