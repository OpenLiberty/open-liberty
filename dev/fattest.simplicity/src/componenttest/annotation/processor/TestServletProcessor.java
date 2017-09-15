/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

        List<FrameworkMethod> testMethods = new ArrayList<FrameworkMethod>();

        Set<FrameworkField> servers = new HashSet<FrameworkField>();
        servers.addAll(testClass.getAnnotatedFields(TestServlet.class));
        servers.addAll(testClass.getAnnotatedFields(TestServlets.class));
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

            // For each @TestServlet for this server, add all @Test methods
            for (TestServlet anno : testServlets) {
                int initialSize = testMethods.size();
                for (Method method : getTestServletMethods(anno)) {
                    if (method.isAnnotationPresent(Test.class)) {
                        testMethods.add(new SyntheticServletTest(serverField, anno, method));
                    }
                }
                Log.info(c, m, "Added " + (testMethods.size() - initialSize) + " test methods from " + anno.servlet());
            }
        }
        return testMethods;
    }

    private static Method[] getTestServletMethods(TestServlet anno) {
        try {
            return anno.servlet().getMethods();
        } catch (TypeNotPresentException e) {
            throw new RuntimeException("The HttpServlet referenced by the @TestServlet(servlet=???, path=" + anno.path() +
                                       ") annotation imported a class that was not available to the JUnit classpath.", e);
        }
    }

}
