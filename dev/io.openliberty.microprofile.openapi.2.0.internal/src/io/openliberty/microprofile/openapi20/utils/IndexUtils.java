/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;

/**
 *
 */
public class IndexUtils {

    private static final TraceComponent tc = Tr.register(IndexUtils.class);

    /**
     * The getIndexView method generates an org.jboss.jandex.IndexView that contains all of the classes that need to be
     * scanned for OpenAPI/JAX-RS annotations. This IndexView is passes to the SmallRye OpenAPI implementation which
     * performs the scanning.  
     * 
     * @param webModuleInfo
     *          The module info for the web module
     * @param moduleClassesContainerInfo
     *          The module classes container info for the web module
     * @param config
     *          The configuration that may specify which classes/packages/JARs to include/exclude. 
     * @return IndexView
     *          The org.jboss.jandex.IndexView instance.
     */
    public static IndexView getIndexView(WebModuleInfo webModuleInfo, ModuleClassesContainerInfo moduleClassesContainerInfo, OpenApiConfig config) {

        long startTime = System.currentTimeMillis();
        
        Indexer indexer = new Indexer();
        FilteredIndexView filter = new FilteredIndexView(null, config);
        
        for (ContainerInfo ci : moduleClassesContainerInfo.getClassesContainerInfo()) {
            if (ci.getType() == Type.WEB_INF_CLASSES) {
                indexContainer(ci.getContainer(), null, indexer, filter);
            } else if (ci.getType() == Type.WEB_INF_LIB) {
                if (acceptJarForScanning(config, ci.getContainer().getName())) {
                    indexContainer(ci.getContainer(), null, indexer, filter);
                }
            }
        }

        // Complete the index
        IndexView view = indexer.complete();
        long endTime = System.currentTimeMillis();
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Index size: " + view.getKnownClasses().size());
            Tr.event(tc, "Indexing elapsed time: " + (endTime - startTime));
        }

        return view;
    }
    
    private static void indexContainer(Container container, String packageName, Indexer indexer, FilteredIndexView filter) {
        for (Entry entry : container) {
            String entryName = entry.getName();
            try {
                if (entryName.endsWith(Constants.FILE_SUFFIX_CLASS)) {
                    int nameLength = entryName.length() - Constants.FILE_SUFFIX_CLASS.length();
                    String className = entryName.substring(0, nameLength);
                    String qualifiedName;
                    if (packageName == null) {
                        qualifiedName = className;
                    } else {
                        qualifiedName = packageName + "." + className;
                    }
                    if (acceptClassForScanning(filter, qualifiedName)) {
                        try (InputStream is = entry.adapt(InputStream.class)) {
                            indexer.index(is);
                        }
                    }
                } else {
                    Container entryContainer = entry.adapt(Container.class);
                    if (entryContainer != null) {
                        String entryPackageName;
                        if (packageName == null) {
                            entryPackageName = entryContainer.getName();
                        } else {
                            entryPackageName = packageName + "."  + entryContainer.getName();
                        }
                        indexContainer(entryContainer, entryPackageName, indexer, filter);
                    }
                }
            } catch (UnableToAdaptException | IOException e) {
                // FFDC and ignore this entry
            }
        }
    }

    /**
     * The acceptClassForScanning method determines whether the specified class should be scanned for MicroProfile
     * OpenAPI annotations based on the configuration specified in the following proeprties:
     * 
     *     mp.openapi.scan.classes
     *     mp.openapi.scan.packages
     *     mp.openapi.scan.exclude.classes
     *     mp.openapi.scan.exclude.packages
     * 
     * @param filter
     *          The SmallRye {@link FilterIndexView} class which wraps an {@link IndexView} instance and filters the
     *          contents based on the settings provided via {@link OpenApiConfig}.
     * @param className
     *          The name of the class
     * @return boolean
     *          True if the class should be accepted for scanning, false otherwise
     */
    private static boolean acceptClassForScanning(final FilteredIndexView filter, final String className) {
        
        // Create the variable to return
        boolean acceptClass = false;
        
        // Make sure that we have a valid class name
        if (className != null && !className.isEmpty()) {
            acceptClass = filter.accepts(DotName.createSimple(className));
        }

        return acceptClass;
    }

    /**
     * The acceptJarForScanning method determines whether the specified JAR file should be opened and the contents 
     * scanned for MicroProfile OpenAPI annotations. The configuration specified in the following proeprties is used
     * to determine whether the JAR file should be opened:
     * 
     *     mp.openapi.extensions.smallrye.scan-dependencies.disable
     *     mp.openapi.extensions.smallrye.scan-dependencies.jars
     * 
     * @param config
     *          The OpenAPIConfig representation of the configuration
     * @param jarFileName
     *          The full name of the JAR file, including the path
     * @return boolean
     *          True if the contents of the JAR file should be accepted for scanning, false otherwise
     */
    private static boolean acceptJarForScanning(final OpenApiConfig config, final String jarFileName) {
        
        // Create the variable to return
        boolean acceptJar = false;
        
        //  First, make sure that dependency scanning has not been disabled
        if (!config.scanDependenciesDisable()) {
            // Now check whether specific JARs have been configured for scanning
            Set<String> scanDependenciesJars = config.scanDependenciesJars();
            String nameOnly = new File(jarFileName).getName();
            if (scanDependenciesJars.isEmpty() || scanDependenciesJars.contains(nameOnly)) {
                acceptJar = true;
            }
        }
        
        return acceptJar;
    }
}
