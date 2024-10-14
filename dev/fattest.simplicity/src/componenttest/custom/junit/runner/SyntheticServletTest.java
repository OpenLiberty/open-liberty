/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package componenttest.custom.junit.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.runners.model.FrameworkMethod;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class SyntheticServletTest extends FrameworkMethod {

    private final Class<?> servletClass;
    private final Field server;
    private final String queryPath;
    private final String testName;
    private final String syntheticName;

    public SyntheticServletTest(Class<?> servletClass, Field server, String queryPath, Method method) {
        super(method);
        this.servletClass = servletClass;
        this.server = server;
        this.queryPath = queryPath;
        this.testName = method.getName();
        this.syntheticName = servletClass.getSimpleName() + "." + this.testName;
    }

    @Override
    public Object invokeExplosively(Object target, Object... params) throws Throwable {
        Log.info(SyntheticServletTest.class, "invokeExplosively", "Running test: " + testName);
        LibertyServer s = (LibertyServer) server.get(null);
        FATServletClient.runTest(s, queryPath, testName);
        return null;
    }

    @Override
    public String getName() {
        return this.syntheticName;
    }

    @Override
    public Annotation[] getAnnotations() {
        final HashMap<Class<? extends Annotation>, Annotation> collection = new HashMap<>();

        Stream<Annotation> methodAnnos = Stream.of(super.getAnnotations());
        Stream<Annotation> servletAnnos = Stream.of(servletClass.getAnnotations())
                        .filter(anno -> anno.annotationType().getCanonicalName().startsWith("componenttest."));

        Stream.concat(servletAnnos, methodAnnos)
                        .forEachOrdered(anno -> {
                            Annotation overwritten = collection.put(anno.annotationType(), anno);
                            if (overwritten != null)
                                Log.warning(SyntheticServletTest.class, "The test method " + testName + " was annotated with " + anno +
                                                                        " and the test servlet " + servletClass.getSimpleName() + " was annotated with " + overwritten +
                                                                        " the servlet annotation will be ignored.");
                        });

        return collection.values().toArray(new Annotation[collection.size()]); //TODO use .toArray(Annotation[]::new); in java 11
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        T anno = null;

        anno = super.getAnnotation(annotationType);
        if (anno != null) {
            return anno;
        }

        anno = servletClass.getAnnotation(annotationType);
        return anno; //Null or annotation from servlet class
    }
}
