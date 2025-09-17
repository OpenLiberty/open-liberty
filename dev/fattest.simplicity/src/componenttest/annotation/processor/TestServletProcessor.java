/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.annotation.processor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.SyntheticServletTest;
import componenttest.topology.impl.LibertyServer;

public class TestServletProcessor {

    private static final Class<?> c = TestServletProcessor.class;

    public static List<FrameworkMethod> getServletTests(TestClass testClass) {
        final String m = "getServletTests";

        Set<FrameworkField> servers = new HashSet<FrameworkField>();
        servers.addAll(testClass.getAnnotatedFields(TestServlet.class));
        servers.addAll(testClass.getAnnotatedFields(TestServlets.class));

        List<FrameworkMethod> orderedTestMethods = new ArrayList<FrameworkMethod>();

        for (FrameworkField server : servers) {
            // Verify server is declared as "public static LibertyServer"
            if (!server.isStatic())
                throw new RuntimeException("Annotated field '" + server.getName() + "' must be static.");
            if (!server.isPublic())
                throw new RuntimeException("Annotated field '" + server.getName() + "' must be public.");
            if (!LibertyServer.class.isAssignableFrom(server.getType()))
                throw new RuntimeException("Annotated field '" + server.getName() + "' must be of type or subtype of " + LibertyServer.class.getCanonicalName());

            // Get the @TestServlet annotation(s) and linked HttpServlet
            Field serverField = server.getField();
            List<TestServlet> testServlets = new ArrayList<TestServlet>();
            if (serverField.getAnnotation(TestServlet.class) != null)
                testServlets.add(serverField.getAnnotation(TestServlet.class));
            if (serverField.getAnnotation(TestServlets.class) != null)
                testServlets.addAll(Arrays.asList(serverField.getAnnotation(TestServlets.class).value()));

            List<FrameworkMethod> testMethods = new ArrayList<FrameworkMethod>();
            List<FrameworkMethod> beforeMethods = new ArrayList<FrameworkMethod>();
            List<FrameworkMethod> afterMethods = new ArrayList<FrameworkMethod>();

            // For each @TestServlet for this server, add all @Test methods
            for (TestServlet anno : testServlets) {
                int initialSize = testMethods.size();
                for (Method method : getTestServletMethods(anno)) {
                    if (method.isAnnotationPresent(Test.class)) {
                        testMethods.add(new SyntheticServletTest(anno.servlet(), serverField, getQueryPath(anno), method));
                    } else if (method.isAnnotationPresent(Before.class)) {
                        beforeMethods.add(new SyntheticServletTest(anno.servlet(), serverField, getQueryPath(anno), method));
                    } else if (method.isAnnotationPresent(After.class)) {
                        afterMethods.add(new SyntheticServletTest(anno.servlet(), serverField, getQueryPath(anno), method));
                    }
                }
                Log.info(c, m, "Added " + (testMethods.size() - initialSize) + " test methods from " + anno.servlet());
            }

            for (int i = 0; i < testMethods.size(); i++) {
                for (FrameworkMethod fm : beforeMethods)
                    orderedTestMethods.add(fm);

                orderedTestMethods.add(testMethods.get(i));

                for (FrameworkMethod fm : afterMethods)
                    orderedTestMethods.add(fm);
            }
        }

        return orderedTestMethods;
    }

    private static String getQueryPath(TestServlet anno) {
        String queryPath = anno.path();
        if (queryPath != null && !queryPath.isEmpty()) {
            // If path() is specified, must not specify contextRoot()
            if (!anno.contextRoot().isEmpty())
                throw new IllegalArgumentException("For the @TestServlet annotation, either path() or contextRoot() should be specified, but not both!");
            return queryPath;
        }

        // Infer queryPath from contextRoot() and @WebServlet annotation
        String[] webServletValue = new String[] {};
        String[] webServletUrlPatterns = new String[] {};
        javax.servlet.annotation.WebServlet webServlet = anno.servlet().getAnnotation(javax.servlet.annotation.WebServlet.class);
        if (webServlet != null) {
            webServletValue = webServlet.value();
            webServletUrlPatterns = webServlet.urlPatterns();
        } else {
            jakarta.servlet.annotation.WebServlet jakartaServlet = anno.servlet().getAnnotation(jakarta.servlet.annotation.WebServlet.class);
            if (jakartaServlet != null) {
                webServletValue = jakartaServlet.value();
                webServletUrlPatterns = jakartaServlet.urlPatterns();
            }
        }
        if (webServletValue.length == 0 && webServletUrlPatterns.length == 0)
            throw new IllegalArgumentException("When using @TestServlet.contextRoot(), the referenced HTTPServlet must define a URL path via the @WebServlet annotation");

        queryPath = anno.contextRoot();
        if (webServletValue.length > 0)
            queryPath += webServletValue[0];
        else
            queryPath += webServletUrlPatterns[0];
        return queryPath.replace("//", "/").replace("*", "");
    }

    private static Method[] getTestServletMethods(TestServlet anno) {
        if (!!!(javax.servlet.http.HttpServlet.class.isAssignableFrom(anno.servlet()) ||
                jakarta.servlet.http.HttpServlet.class.isAssignableFrom(anno.servlet()))) {
            throw new IllegalArgumentException("The servlet referenced by the " + annoToString(anno) + " annotation " +
                                               "is not a subclass of javax.servlet.http.HttpServlet nor jakarta.servlet.http.HttpServlet. " +
                                               "When using the @TestServlet annotation make sure to declare a servlet class that " +
                                               "extends either javax.servlet.http.HttpServlet or jakarta.servlet.http.HttpServlet");

        }
        try {
            return anno.servlet().getMethods();
        } catch (TypeNotPresentException | LinkageError e) {
            throw new RuntimeException("The HttpServlet referenced by the " + annoToString(anno) +
                                       " annotation imported a class that was not available to the JUnit classpath. " +
                                       "Make sure that the missing type is present on the runtime classpath of the FAT " +
                                       " (i.e. somewhere in autoFVT/lib/ or autoFVT/build/lib/ )", e);
        }
    }

    private static String annoToString(TestServlet anno) {
        String servletClass = "???";
        try {
            servletClass = anno.servlet().getSimpleName() + ".class";
        } catch (Throwable ignore) {
        }
        StringBuilder s = new StringBuilder("@TestServlet(servlet=" + servletClass);
        if (!anno.path().isEmpty())
            s.append(", path=\"" + anno.path() + '"');
        if (!anno.contextRoot().isEmpty())
            s.append(", contextRoot=\"" + anno.contextRoot() + '"');
        return s.append(')').toString();
    }

}
