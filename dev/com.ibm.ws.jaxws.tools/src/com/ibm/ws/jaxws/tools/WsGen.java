/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.tools;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;

import com.sun.tools.ws.wscompile.WsgenTool;

/**
 *
 */
public class WsGen {

    /**
     * @param args
     */
    public static void main(String[] args) {

        //Pass in the JWS and JAX-B APIs as a -classpath arg when Java 9 or above.
        //Otherwise the javac process started by the tooling doesn't contain these APIs.
        if (getMajorJavaVersion() > 8) {
            String classpathValue = null;
            Class<?> JAXB = null;
            Class<?> WebService = null;
            try {
                JAXB = Thread.currentThread().getContextClassLoader().loadClass("javax.xml.bind.JAXB");
                WebService = Thread.currentThread().getContextClassLoader().loadClass("javax.jws.WebService");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(2);
            }

            classpathValue = getJarFileOfClass(JAXB);
            classpathValue = classpathValue + File.pathSeparator + getJarFileOfClass(WebService);

            if (classpathValue != null) {
                boolean classpathSet = false;

                //Search for existing -cp or -classpath arg.
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("-cp") || args[i].equals("-classpath")) {
                        args[i + 1] = args[i + 1] + File.pathSeparator + classpathValue;
                        classpathSet = true;
                    }
                }

                //No existing -cp or -classpath arg was found so add it to the end (just before the SEI class).
                if (!classpathSet && args.length > 0) {
                    args = Arrays.copyOf(args, args.length + 2);
                    args[args.length - 1] = args[args.length - 3]; //push SEI class to the end of args
                    args[args.length - 2] = classpathValue; //insert paths for -classpath arg
                    args[args.length - 3] = "-classpath"; //insert the -classpath arg
                }
            }
        }

        System.exit(new WsgenTool(System.out).run(args) ? 0 : 1);
    }

    private static String getJarFileOfClass(final Class<?> javaClass) {
        ProtectionDomain protectionDomain = AccessController.doPrivileged(
                                                                          new PrivilegedAction<ProtectionDomain>() {
                                                                              @Override
                                                                              public ProtectionDomain run() {
                                                                                  return javaClass.getProtectionDomain();
                                                                              }
                                                                          });

        return protectionDomain.getCodeSource().getLocation().getPath();

    }

    private static int getMajorJavaVersion() {
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D");
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        return Integer.valueOf(versionElements[i]);
    }

}
