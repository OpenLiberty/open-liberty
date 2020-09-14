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
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;

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
     * @param config
     *          The configuration that may specify which classes/packages/JARs to include/exclude. 
     * @return IndexView
     *          The org.jboss.jandex.IndexView instance.
     */
    public static IndexView getIndexView(WebModuleInfo webModuleInfo, OpenApiConfig config) {

        long startTime = System.currentTimeMillis();

        Indexer indexer = new Indexer();
        FilteredIndexView filter = new FilteredIndexView(null, config);

        // Get the URL to the /WEB-INF/classes directory in the web module
        String containerPhysicalPath = webModuleInfo.getContainer().getPhysicalPath();
        Path containerPath = Paths.get(containerPhysicalPath);
        Path webinfClassesPath = containerPath.resolve(Constants.DIR_WEB_INF).resolve(Constants.DIR_CLASSES);
        if (Files.exists(webinfClassesPath) && Files.isDirectory(webinfClassesPath)) {
            // The /WEB-INF/classes directrory exists.  This is probably an expanded/loose app. Process the files in the directory.
            try (Stream<Path> walk = Files.walk(webinfClassesPath)) {
                List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

                for (Path file : files) {
                    try {
                        processFile(file.getFileName().toString(), Files.newInputStream(file), indexer, filter, config);
                    } catch (IOException e) {
                        if (LoggingUtils.isEventEnabled(tc)) {
                            Tr.event(tc, String.format("Error occurred when processing file %s: %s", file.getFileName().toString(), e.getMessage()));
                        }
                    }
                } // FOR
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, String.format("Error occurred when attempting to walk files for directory %s: %s", webinfClassesPath, e.getMessage()));
                }
            }
        } else if (  Files.isRegularFile(containerPath)
                  && (  containerPath.getFileName().toString().endsWith(Constants.FILE_SUFFIX_WAR)
                     || containerPath.getFileName().toString().endsWith(Constants.FILE_SUFFIX_JAR)
                     )
                  ) {
            try {
                // The /WEB-INF/classes directrory does not exist.  This is a JAR/WAR.
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Processing archive: " + containerPath);
                }
                processJar(Files.newInputStream(containerPath), indexer, filter, config);
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, String.format("Error occurred when processing archive file %s: %s", containerPath, e.getMessage()));
                }
            }
        } else {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, String.format("%s is not a not a jar, war or directory", containerPath));
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

    /**
     * The processJar method iterates over the contents of a JAR file, processing each file in turn.
     * 
     * @param inputStream
     *            The inputStream that should be used to read the content of the JAR file
     * @param indexer
     *            The Jandex indexer
     * @param config
     *            The OpenAPIConfig representation of the configuration
     * @throws IOException
     */
    private static void processJar(InputStream inputStream, Indexer indexer, FilteredIndexView filter, OpenApiConfig config) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                processFile(zipEntry.getName(), zipInputStream, indexer, filter, config);
            }
        }
    }

    /**
     * The processFile method will index the specified file if it is a class file or pass it to the processJar method
     * if it is a WAR/JAR file.  Before performing either task, it will check whether relevant file has been configured
     * to allow scanning based on the undlerying MP config properties.
     * 
     * @param fileName
     *            The name of the file being processed
     * @param inputStream
     *            The inputStream that should be used to read the content of the file
     * @param indexer
     *            The Jandex indexer
     * @param config
     *            The OpenAPIConfig representation of the configuration
     * @throws IOException
     */
    private static void processFile(String fileName, InputStream inputStream, Indexer indexer, FilteredIndexView filter, OpenApiConfig config) throws IOException {
        if (fileName.endsWith(Constants.FILE_SUFFIX_CLASS)) {
            final String className = convertToClassName(fileName);
            if (acceptClassForScanning(filter, className)) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Indexing class: " + className);
                }

                indexer.index(inputStream);
            } else {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Filtered class: " + className);
                }
            }
        } else if (fileName.endsWith(Constants.FILE_SUFFIX_WAR) || fileName.endsWith(Constants.FILE_SUFFIX_JAR)) {
            if (acceptJarForScanning(config, fileName)) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Processing archive: " + fileName);
                }

                byte[] archive = copyArchive(inputStream);
                processJar(new ByteArrayInputStream(archive), indexer, filter, config);
            } else {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Filtered archive: " + fileName);
                }
            }
        }
    }
    
    /**
     * The convertToClassName method converts the fileName to a canonical Java class name
     * 
     * @param fileName
     *            The name of the file to convert
     * @return String
     *             The class name
     */
    private static String convertToClassName(String fileName) {
        String className;
        // Remove ".class" extension
        className = fileName.replace(Constants.FILE_SUFFIX_CLASS, Constants.STRING_EMPTY);

        // If necessary, remove WEB-INF/classes/
        className = className.replace(Constants.DIR_WEB_INF_CLASSES, Constants.STRING_EMPTY);

        // Substitute dots for slashes and backslashes
        className = className.replace(Constants.STRING_FORWARD_SLASH, Constants.STRING_PERIOD);
        className = className.replace(Constants.STRING_BACK_SLASH, Constants.STRING_PERIOD);

        if (className.startsWith(Constants.STRING_PERIOD)) {
            className = className.substring(1);
        }

        return className;
    }
    
    /**
     * The copyArchive method copies the content of an embedded JAR file into a separate byte array.  This is required
     * because simply creating another InputStream based on the original ZipInputStream will result in all of the
     * InputStreams being closed when the first one is closed.
     * 
     * @param inputStream
     *            The InputStream that is used to read the content of the embedded JAR file
     * @return byte[]
     *            The content of the embedded JAR file
     * @throws IOException
     */
    private static byte[] copyArchive(InputStream inputStream) throws IOException {
        byte[] content = null;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[4096];
            int numBytes;
            while ((numBytes = inputStream.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, numBytes);
            }
            content = baos.toByteArray();
        }
        return content;
    }
}
