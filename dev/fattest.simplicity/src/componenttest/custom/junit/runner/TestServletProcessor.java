/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
import componenttest.topology.impl.LibertyServer;

public class TestServletProcessor {

    private static final Class<?> c = TestServletProcessor.class;

    /**
     * Create and return synthetic test methods based on {@link TestServlet}
     * and {@link TestServlets} annotations on a test class.
     *
     * Test servlet and test servlets annotations must be specified on
     * public {@link LibertyServer} type fields of the test class.
     * 
     * 
     *
     * @param testClass The test class for which to generate synthetic test
     *     methods.
     * @return The generated test methods.
     */
    public static List<FrameworkMethod> getServletTests(TestClass testClass) {
        final String m = "getServletTests";

        Set<FrameworkField> serverFields = new HashSet<FrameworkField>();
        serverFields.addAll(testClass.getAnnotatedFields(TestServlet.class));
        serverFields.addAll(testClass.getAnnotatedFields(TestServlets.class));

        if ( serverFields.isEmpty() ) {
            return Collections.emptyList();
        }

        List<FrameworkMethod> testMethods = new ArrayList<FrameworkMethod>();

        for ( FrameworkField server : serverFields ) {
            if ( !server.isStatic() ) {
                throw new RuntimeException("Test servlet annotated field '" + server.getName() + "' must be static.");
            } else if ( !server.isPublic() ) {
                throw new RuntimeException("Test servlet annotated field '" + server.getName() + "' must be public.");
            } else if ( !LibertyServer.class.isAssignableFrom(server.getType()) ) {
                throw new RuntimeException("Test servlet annotated field '" + server.getName() + "' must be of type or subtype of " + LibertyServer.class.getCanonicalName());
            }

            Field serverField = server.getField();

            List<TestServlet> testServlets = new ArrayList<TestServlet>();

            TestServlet testServletAnno = serverField.getAnnotation(TestServlet.class);
            if ( testServletAnno != null ) {
                testServlets.add(testServletAnno);
            }

            TestServlets testServletsAnno = serverField.getAnnotation(TestServlets.class);
            if ( testServletsAnno != null ) {
                for ( TestServlet testServletsElement : testServletsAnno.value() ) {
                    testServlets.add(testServletsElement);
                }
            }

            for ( TestServlet testServlet : testServlets ) {
                int initialSize = testMethods.size();
                for ( Method method : getMethods(testServlet) ) {
                    if ( method.isAnnotationPresent(Test.class) ) {
                        testMethods.add( new SyntheticServletTest(serverField, getQueryPath(testServlet), method) );
                    }
                }
                Log.info(c, m, "Added " + (testMethods.size() - initialSize) + " test methods from " + testServlet.servlet());
            }
        }

        return testMethods;
    }

    /**
     * Generate the HTTP query for a specified test servlet annotation.
     *
     * An annotation that has an explicit path must not specify a
     * context root.
     *
     * If a context root is specified, the complete query path is generated
     * by appending the servlet or URL pattern from the web servlet
     * annotation of the target servlet.
     *
     * @param anno The test servlet annotation for which to generate
     *     an HTTP query.
     *
     * @return The HTTP query for the test servlet annotation.
     */
    private static String getQueryPath(TestServlet anno) {
        String directQueryPath = anno.path();
        if ( !directQueryPath.isEmpty() ) {
            if ( !anno.contextRoot().isEmpty() ) {
                throw new IllegalArgumentException(
                    "Test servlet annotation " + annoToString(anno) +
                    " specifies both 'path' and 'contextRoot'.");
            }
            return directQueryPath;
        }

        String[] webServletValue = new String[] {};
        String[] webServletUrlPatterns = new String[] {};

        javax.servlet.annotation.WebServlet javaxServlet =
            anno.servlet().getAnnotation(javax.servlet.annotation.WebServlet.class);
        if ( javaxServlet != null ) {
            webServletValue = javaxServlet.value();
            webServletUrlPatterns = javaxServlet.urlPatterns();
        } else {
            jakarta.servlet.annotation.WebServlet jakartaServlet =
                anno.servlet().getAnnotation(jakarta.servlet.annotation.WebServlet.class);
            if ( jakartaServlet != null ) {
                webServletValue = jakartaServlet.value();
                webServletUrlPatterns = jakartaServlet.urlPatterns();
            }
        }

        if ( (webServletValue.length == 0) && (webServletUrlPatterns.length == 0) ) {
            throw new IllegalArgumentException(
                "Test servlet annotation " + annoToString(anno) +
                " uses 'contextRoot' but does not obtain a URL path from the @WebServlet annotation.");
        }

        String queryPath = anno.contextRoot();
        if ( webServletValue.length > 0 ) {
            queryPath += webServletValue[0];
        } else {
            queryPath += webServletUrlPatterns[0];
        }

        return queryPath.replace("//", "/").replace("*", "");
    }

    /**
     * Retrieve all methods of the servlet class of a test servlet annotation.
     * 
     * Validate that the servlet class is typed as an HTTP servlet.  (Either as
     * a javax or as a jakarta HTTP servlet.)
     * 
     * @param anno A test servlet annotation.
     *
     * @return The methods of the servlet class of the annotation.
     */
    private static Method[] getMethods(TestServlet anno) {
        Class<?> servlet = anno.servlet();

        if ( !javax.servlet.http.HttpServlet.class.isAssignableFrom(servlet) &&
             !jakarta.servlet.http.HttpServlet.class.isAssignableFrom(servlet) ) {
            throw new IllegalArgumentException(
                "The class referenced by servlet annotation " + annoToString(anno) +
                " is not a subclass of javax.servlet.http.HttpServlet" +
                " or jakarta.servlet.http.HttpServlet.");
        }

        try {
            return servlet.getMethods();

        } catch ( TypeNotPresentException | LinkageError e ) {
            throw new RuntimeException(
                "The servlet referenced by annotation " + annoToString(anno) +
                " imports a class that is not available to the JUnit classpath." +
                "  Make sure that the missing type is present on the runtime classpath of the FAT." +
                "  Usually, somewhere in 'autoFVT/lib/' or 'autoFVT/build/lib/'.",
                e);
        }
    }

    /**
     * Answer a print string for a test servlet annotation.  The
     * print string is used in error messages.
     *
     * @param anno A test servlet annotation.
     *
     * @return A print string for the test servlet annotation.
     */
    private static String annoToString(TestServlet anno) {
        StringBuilder s = new StringBuilder();
        
        s.append( "@TestServlet(");
        
        s.append( "servlet=" );
        s.append( anno.servlet().getSimpleName() );
        s.append( ".class" );

        if ( !anno.path().isEmpty() ) {
            s.append( ", path=\"" );
            s.append( anno.path() );
            s.append( '"' );
        }

        if ( !anno.contextRoot().isEmpty() ) {
            s.append( ", contextRoot=\"" );
            s.append( anno.contextRoot() );
            s.append( '"' );
        }

        s.append( ')' );

        return s.toString();
    }

}
