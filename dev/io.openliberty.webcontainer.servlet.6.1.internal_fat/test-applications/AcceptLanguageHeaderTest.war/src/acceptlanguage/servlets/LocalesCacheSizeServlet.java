/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package acceptlanguage.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet that reflects to retrieve the server-wide EncodingUtils localesCache size
 */
@WebServlet("/localesCacheSize")
public class LocalesCacheSizeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            // Wrap classloader access in privileged action to handle security permissions
            Integer cacheSize = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    try {
                        Class<?> encodingUtilsClass = null;
                        ClassLoader targetClassLoader = request.getLocales().getClass().getClassLoader();

                        try {
                            encodingUtilsClass = Class.forName("com.ibm.wsspi.webcontainer.util.EncodingUtils", true, targetClassLoader);
                        } catch (ClassNotFoundException e) {
                            // Try parent classloaders
                            ClassLoader current = targetClassLoader;
                            while (current != null && encodingUtilsClass == null) {
                                try {
                                    encodingUtilsClass = Class.forName("com.ibm.wsspi.webcontainer.util.EncodingUtils", true, current);
                                    break;
                                } catch (ClassNotFoundException e2) {
                                    current = current.getParent();
                                }
                            }
                        }

                        if (encodingUtilsClass != null) {
                            // Access the localesCache field
                            Field localesCacheField = encodingUtilsClass.getDeclaredField("localesCache");
                            localesCacheField.setAccessible(true);

                            @SuppressWarnings("unchecked")
                            Map<String, ?> localesCache = (Map<String, ?>) localesCacheField.get(null);

                            if (localesCache != null) {
                                return localesCache.size();
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            if (cacheSize != null) {
                System.out.println(LocalesCacheSizeServlet.class.getName() + " , localesCache size: " + cacheSize);
                out.println("Cache Size: " + cacheSize);
            } else {
                out.println("Error: Could not load EncodingUtils class or localesCache is null");
            }

        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
