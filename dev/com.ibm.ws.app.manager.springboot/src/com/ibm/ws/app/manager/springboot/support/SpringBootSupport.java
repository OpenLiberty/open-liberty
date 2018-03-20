/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.support;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_SUPPORT_CAPABILITY;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_SUPPORT_CAPABILITY_JARS;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A service registered by the Spring Boot support bundle indicating the
 * paths to the Spring Boot support jars that should be added to the classpath
 * of Spring Boot applications.
 * <p>
 * The Spring Boot Support service is intended to be a singleton service. It represents
 * a specific level of Spring Boot that it supports. For example, Spring Boot version
 * 1.5. A Spring Boot Support service provides one or more Helpers that assist
 * in configuring and starting embedded containers for a Spring Boot application.
 * For example, to start a web container or a Reactive (WebFlux) container.
 */
@Component
public abstract class SpringBootSupport {
    private static final TraceComponent tc = Tr.register(SpringBootSupport.class);

    protected List<String> jars;

    /**
     * Returns the paths to the Spring Boot support jars. The paths are
     * contained in the bundle that registers the this service
     *
     * @return
     */
    public List<String> getJarPaths() {
        return jars;
    }

    @Activate
    protected void activate(BundleContext context) {
        jars = getJarPaths(context);
    }

    private List<String> getJarPaths(BundleContext context) {
        Bundle b = context.getBundle();
        BundleRevision rev = b.adapt(BundleRevision.class);
        List<BundleCapability> supportCaps = rev.getDeclaredCapabilities(SPRING_BOOT_SUPPORT_CAPABILITY);
        if (supportCaps.isEmpty()) {
            FFDCFilter.processException(new RuntimeException("No Spring Boot support capability found in: " + b.getSymbolicName()), SpringBootSupport.class.getName(),
                                        "getJarPaths");
            return Collections.emptyList();
        }
        BundleCapability supportCap = supportCaps.iterator().next();
        @SuppressWarnings("unchecked")
        List<String> jarsAttr = (List<String>) supportCap.getAttributes().get(SPRING_BOOT_SUPPORT_CAPABILITY_JARS);
        if (jarsAttr == null || jarsAttr.isEmpty()) {
            FFDCFilter.processException(new RuntimeException("No Spring Boot support jars found in: " + supportCap), SpringBootSupport.class.getName(), "getJarPaths");
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(jarsAttr.size());
        for (String jarPath : jarsAttr) {
            URL jarEntry = b.getEntry(jarPath);
            if (jarEntry != null) {
                result.add(jarPath);
            } else {
                FFDCFilter.processException(new RuntimeException("No Spring Boot jars found at path: " + jarPath), SpringBootSupport.class.getName(), "getJarPaths");
            }
        }
        return Collections.unmodifiableList(result);
    }
}
