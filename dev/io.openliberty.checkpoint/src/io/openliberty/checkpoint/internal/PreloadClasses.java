/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
@Component
public class PreloadClasses {
    private final static String CLASSES_LIST_PATH = "io.openliberty.checkpoint.classes";
    private final String classesList;
    private final FrameworkWiring fwkWiring;

    @Activate
    public PreloadClasses(BundleContext context) {
        this.classesList = context.getProperty(CLASSES_LIST_PATH);
        this.fwkWiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        preloadClasses();
    }

    @FFDCIgnore(IOException.class)
    public void preloadClasses() {
        if (classesList == null) {
            return;
        }

        Collection<ClassLoader> loaders = getLoaders();
        try (Stream<String> lines = Files.lines(Paths.get(classesList))) {
            lines.map(PreloadClasses::getClassFromLine).forEach((c) -> {
                loadClass(loaders, c);
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private void loadClass(Collection<ClassLoader> loaders, String className) {
        for (ClassLoader cl : loaders) {
            try {
                cl.loadClass(className);
                System.out.println("Found class: " + className);
                return;
            } catch (ClassNotFoundException e) {
                // continue
            }
        }
        try {
            ClassLoader.getSystemClassLoader().loadClass(className);
            System.out.println("Found class: " + className);
        } catch (ClassNotFoundException e) {
            // nothing
        }
        System.out.println("Failed to find class: " + className);
    }

    private Collection<ClassLoader> getLoaders() {
        Collection<ClassLoader> loaders = new ArrayList<>();
        fwkWiring.findProviders(new AllIdentityRequirement()).forEach(c -> {
            BundleWiring wiring = c.getRevision().getWiring();
            if (wiring != null) {
                ClassLoader cl = wiring.getClassLoader();
                if (cl != null) {
                    loaders.add(cl);
                }
            }
        });
        return loaders;
    }

    static String getClassFromLine(String line) {
        int start = line.lastIndexOf('\t') + 1;
        if (start <= 0) {
            return line;
        }
        return line.substring(start).replace('/', '.');
    }

    static class AllIdentityRequirement implements Requirement {

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getDirectives() {
            return Collections.emptyMap();
        }

        @Override
        public String getNamespace() {
            return IdentityNamespace.IDENTITY_NAMESPACE;
        }

        @Override
        public Resource getResource() {
            return null;
        }

    }
}
