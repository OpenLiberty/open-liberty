/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jakarta.xml.bind;

import java.io.IOException;

/**
 * Intended to be overridden on JDK9, with JEP 238 multi-release class copy.
 * Contains only stubs for methods needed on JDK9 runtime.
 *
 * @author Roman Grigoriadi
 */
class ModuleUtil {

    /**
     * When JAXB is in J2SE, rt.jar has to have a JAXB implementation.
     * However, rt.jar cannot have META-INF/services/jakarta.xml.bind.JAXBContext
     * because if it has, it will take precedence over any file that applications have
     * in their jar files.
     *
     * <p>
     * When the user bundles his own JAXB implementation, we'd like to use it, and we
     * want the platform default to be used only when there's no other JAXB provider.
     *
     * <p>
     * For this reason, we have to hard-code the class name into the API.
     */
    // NOTICE: .toString() is used to prevent constant inlining by Java Compiler
    static final String DEFAULT_FACTORY_CLASS = "org.glassfish.jaxb.runtime.v2.ContextFactory".toString();

    /**
     * Resolves classes from context path.
     * Only one class per package is needed to access its {@link java.lang.Module}
     */
    static Class[] getClassesFromContextPath(String contextPath, ClassLoader classLoader) throws JAXBException {
        return null;
    }

    /**
     * Find first class in package by {@code jaxb.index} file.
     */
    static Class findFirstByJaxbIndex(String pkg, ClassLoader classLoader) throws IOException, JAXBException {
        return null;
    }

    /**
     * Implementation may be defined in other module than {@code java.xml.bind}. In that case openness
     * {@linkplain java.lang.Module#isOpen open} of classes should be delegated to implementation module.
     *
     * @param classes    used to resolve module for {@linkplain java.lang.Module#addOpens(String, java.lang.Module)}
     * @param factorySPI used to resolve {@link java.lang.Module} of the implementation.
     */
    static void delegateAddOpensToImplModule(Class[] classes, Class<?> factorySPI) {
        //stub
    }

}
