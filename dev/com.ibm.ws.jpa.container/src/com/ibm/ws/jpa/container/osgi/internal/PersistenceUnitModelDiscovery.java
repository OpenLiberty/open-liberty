/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.osgi.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.wsspi.adaptable.module.Container;

/**
 * Since Jakarta Persistence 4.0, discovery of application model
 * classes is explicitly a requirement  of the container.  This
 * interface defines a provider-neutral discovery of the complete
 * persistence-unit model.
 */
interface PersistenceUnitModelDiscovery {

    /**
     * Discover the complete ordered model for one persistence unit.
     *
     * @param context persistence-unit inputs and deployment boundaries
     * @return immutable discovery result
     */
    Result discover(Context context) throws DiscoveryException;

    /**
     * Failure to produce a complete model before provider bootstrap.
     */
    final class DiscoveryException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        DiscoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Opens a mapping resource without loading application classes.
     */
    interface MappingResourceAccess {
        InputStream open(String resourceName);
    }

    /**
     * Immutable Liberty deployment source selected for discovery.
     */
    final class Source {
        private final Container container;
        private final String entryPrefix;
        private final String diagnosticName;

        Source(Container container, String entryPrefix, String diagnosticName) {
            if (container == null) {
                throw new IllegalArgumentException("A persistence-unit discovery source requires a container");
            }
            this.container = container;
            this.entryPrefix = normalizePrefix(entryPrefix);
            this.diagnosticName = diagnosticName == null ? container.getPath() : diagnosticName;
        }

        Container getContainer() {
            return container;
        }

        String getEntryPrefix() {
            return entryPrefix;
        }

        String getDiagnosticName() {
            return diagnosticName;
        }

        private static String normalizePrefix(String prefix) {
            if (prefix == null || prefix.isEmpty()) {
                return null;
            }
            int start = 0;
            while (start < prefix.length() && prefix.charAt(start) == '/') {
                start++;
            }
            if (start == prefix.length()) {
                return null;
            }
            String normalized = prefix.substring(start);
            return normalized.endsWith("/") ? normalized : normalized + "/";
        }
    }

    /**
     * Immutable inputs for discovery of one persistence unit.
     */
    final class Context {
        private final String applicationName;
        private final String moduleName;
        private final String persistenceUnitName;
        private final Source rootSource;
        private final boolean discoverRoot;
        private final List<Source> referencedSources;
        private final List<String> explicitClassNames;
        private final List<String> mappingFileNames;
        private final MappingResourceAccess mappingResourceAccess;
        private final ClassLoader classLoader;
        private final boolean useJandex;

        Context(String applicationName,
                String moduleName,
                String persistenceUnitName,
                Source rootSource,
                boolean discoverRoot,
                List<Source> referencedSources,
                List<String> explicitClassNames,
                List<String> mappingFileNames,
                MappingResourceAccess mappingResourceAccess,
                ClassLoader classLoader,
                boolean useJandex) {
            this.applicationName = applicationName;
            this.moduleName = moduleName;
            this.persistenceUnitName = persistenceUnitName;
            this.rootSource = rootSource;
            this.discoverRoot = discoverRoot;
            this.referencedSources = immutableCopy(referencedSources);
            this.explicitClassNames = immutableCopy(explicitClassNames);
            this.mappingFileNames = immutableCopy(mappingFileNames);
            this.mappingResourceAccess = mappingResourceAccess;
            this.classLoader = classLoader;
            this.useJandex = useJandex;
        }

        String getApplicationName() {
            return applicationName;
        }

        String getModuleName() {
            return moduleName;
        }

        String getPersistenceUnitName() {
            return persistenceUnitName;
        }

        Source getRootSource() {
            return rootSource;
        }

        boolean isDiscoverRoot() {
            return discoverRoot;
        }

        List<Source> getReferencedSources() {
            return referencedSources;
        }

        List<String> getExplicitClassNames() {
            return explicitClassNames;
        }

        List<String> getMappingFileNames() {
            return mappingFileNames;
        }

        MappingResourceAccess getMappingResourceAccess() {
            return mappingResourceAccess;
        }

        ClassLoader getClassLoader() {
            return classLoader;
        }

        boolean isUseJandex() {
            return useJandex;
        }

        private static <T> List<T> immutableCopy(List<T> values) {
            return Collections.unmodifiableList(new ArrayList<T>(values));
        }
    }

    /**
     * Immutable complete model produced by discovery.
     */
    final class Result {
        private final List<String> allClassNames;

        Result(List<String> allClassNames) {
            this.allClassNames = Collections.unmodifiableList(new ArrayList<String>(allClassNames));
        }

        List<String> getAllClassNames() {
            return allClassNames;
        }
    }
}
