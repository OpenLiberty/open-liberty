/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.osgi;

import java.io.PrintWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.logging.Introspector;

/**
 *
 */
@Component
public class LoggingIntrospector implements Introspector {
    @Override
    public String getIntrospectorName() {
        return "LoggingIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspects the Logging framework";
    }

    @Override
    public void introspect(PrintWriter writer) {
        LogManager lm = LogManager.getLogManager();

        // Print system property information
        try {
            writer.println("System Property: java.util.logging.config.class = " + System.getProperty("java.util.logging.config.class"));
        } catch (Throwable t) {
        }
        try {
            writer.println("System Property: com.ibm.ejs.ras.writeSystemStreamsDirectlyToFile = " + System.getProperty("com.ibm.ejs.ras.writeSystemStreamsDirectlyToFile"));
        } catch (Throwable t) {
        }
        try {
            writer.println("System Property: java.util.logging.config.file = " + System.getProperty("java.util.logging.config.file"));
        } catch (Throwable t) {
        }
        try {
            writer.println("System Property: java.home = " + System.getProperty("java.home"));
        } catch (Throwable t) {
        }
        writer.println("");

        // Print LogManager properties
        try {
            writer.println("LogManager Property: handlers = " + System.getProperty("handlers"));
        } catch (Throwable t) {
        }
        try {
            writer.println("LogManager Property: config = " + System.getProperty("config"));
        } catch (Throwable t) {
        }
        writer.println("");

        //Print System.out/err information
        try {
            writer.println("System.out is set to " + System.out);
        } catch (Throwable t) {
        }
        try {
            writer.println("System.err is set to " + System.err);
        } catch (Throwable t) {
        }
        writer.println("");

        // Print logger and handler information
        if (lm == null) {
            writer.println("LogManager could not be retrieved. Value is NULL. Logging Introspector will now exit as no further data can be found.");
            return;
        }

        writer.println("logManager.getClass() == " + lm.getClass().getName() + "\n");

        Enumeration en = lm.getLoggerNames();
        while (en.hasMoreElements()) {
            String loggerName = (String) en.nextElement();
            Logger logger = lm.getLogger(loggerName);
            
            if (logger == null) {
                writer.println("A logger called " + loggerName + " could not be retrieved or does not exist. Skipping... \n");
                continue;
            }

            writer.println("Logger \"" + loggerName + "\" type: " + logger.getClass().getName() + " level: " + logger.getLevel());

            Handler[] handlers = logger.getHandlers();
            for (int i = 0; i < handlers.length; i++) {
                writer.println("        Handler " + i + " " + handlers[i] + " type: " + handlers[i].getClass().getName() + " level: " + handlers[i].getLevel().getName());

                if (handlers[i] instanceof Handler) {
                    final Handler h = handlers[i];
                    final Class tempClass = h.getClass();

                    try {
                        final Handler wsHandler = (Handler) AccessController.doPrivileged(
                                                                                          new PrivilegedExceptionAction() {
                                                                                              @Override
                                                                                              public Object run() throws Exception {
                                                                                                  Field[] tempObjectFields = tempClass.getDeclaredFields();
                                                                                                  if (tempObjectFields.length != 0) {
                                                                                                      AccessibleObject.setAccessible(tempObjectFields, true);
                                                                                                  }
                                                                                                  for (int i = 0; i < tempObjectFields.length; i++) {
                                                                                                      Field f = tempObjectFields[i];
                                                                                                      if (f.getName().contains("Handler")) {
                                                                                                          return f.get(h);
                                                                                                      }
                                                                                                  }
                                                                                                  return null;
                                                                                              }
                                                                                          });
                        if (wsHandler != null) {
                            writer.println("Handler.class: " + wsHandler.getClass().getName());
                        }
                    } catch (Throwable t) {
                    }
                }
            }
            writer.println("");
        }
        writer.println("Logging Introspector has completed.");
    }

}