/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInformationType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InnerClassesType;

public final class EntityMappingsScanner {
    public static EntityMappingsScannerResults scanTargetArchive(URL targetArchive, ClassLoader scannerCL) throws ClassScannerException {
        if (targetArchive == null || scannerCL == null) {
            throw new ClassScannerException("EntityMappingsScanner.scanTargetArchive cannot accept null arguments.");
        }

        EntityMappingsScanner ems = new EntityMappingsScanner(targetArchive, scannerCL);
        ClassInformationType cit = ems.scanTargetArchive();
        return new EntityMappingsScannerResults(cit, targetArchive);
    }

    private final URL targetArchive;
    private final ClassLoader scannerCL;
    private final InnerOuterResolver ioResolver = new InnerOuterResolver();

    private EntityMappingsScanner(URL targetArchive, ClassLoader scannerCL) {
        this.targetArchive = targetArchive;
        this.scannerCL = scannerCL;
    }

    private ClassInformationType scanTargetArchive() throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();

        /*
         * The JPA Specification's PersistenceUnitInfo contract for getJarFileURLs() and
         * getPersistenceUnitRoot() makes the following mandate:
         *
         * A URL will either be a file: URL referring to a jar file or referring to a
         * directory that contains an exploded jar file, or some other URL from which an
         * InputStream in jar format can be obtained.
         */
        final String urlProtocol = targetArchive.getProtocol();
        if ("file".equalsIgnoreCase(urlProtocol)) {
            // Protocol is "file", which either addresses a jar file or an exploded jar file
            try {
                Path taPath = Paths.get(targetArchive.toURI());
                if (Files.isDirectory(taPath)) {
                    // Exploded Archive
                    citSet.addAll(processExplodedJarFormat(taPath));
                } else {
                    // Unexploded Archive
                    citSet.addAll(processUnexplodedFile(taPath));
                }
            } catch (URISyntaxException e) {
                FFDCFilter.processException(e, EntityMappingsScanner.class.getName() + ".scanTargetArchive", "85");
                throw new ClassScannerException(e);
            }

        } else if (targetArchive.toString().startsWith("jar:file")) {
            citSet.addAll(processJarFileURL(targetArchive));
        } else {
            // InputStream will be in jar format.
            citSet.addAll(processJarFormatInputStreamURL(targetArchive));
        }

        // Find Inner Classes, merge them into their encapsulating class, and remove them as a standalone ClassInfoType.
        processInnerClasses(citSet);

        ClassInformationType cit = new ClassInformationType();
        List<ClassInfoType> citList = cit.getClassInfo();
        citList.addAll(citSet);

        ioResolver.resolve(citList);

        return cit;
    }

    private void processInnerClasses(final Set<ClassInfoType> citSet) throws ClassScannerException {
        final HashSet<ClassInfoType> innerClassSet = new HashSet<ClassInfoType>();
        for (ClassInfoType cit : citSet) {
            final String className = cit.getClassName();
            if (className.contains("$")) {
                innerClassSet.add(cit);
            }
        }

        if (innerClassSet.size() == 0) {
            // No inner classes to process.
            return;
        }

        // Found inner classes, (index + 1) identifies the inner class nested depth (index=0 for topmost inner class)
        final ArrayList<HashSet<ClassInfoType>> innerClassDepthList = new ArrayList<HashSet<ClassInfoType>>();

        // Sort inner classes into increasing nested inner class depth
        for (ClassInfoType innerCit : innerClassSet) {
            final String innerClassName = innerCit.getClassName();
            final String outerClassName = innerClassName.substring(0, innerClassName.lastIndexOf("$"));

            int depth = 1;
            for (char c : outerClassName.toCharArray()) {
                if ('$' == c) {
                    depth++;
                }
            }

            if (innerClassDepthList.size() < (depth)) {
                for (int i = depth - innerClassDepthList.size(); i > 0; i--) {
                    innerClassDepthList.add(new HashSet<ClassInfoType>());
                }
            }

            HashSet<ClassInfoType> innerClassDepthSet = innerClassDepthList.get(depth - 1);
            innerClassDepthSet.add(innerCit);
        }

        if (innerClassDepthList.size() > 1) {
            // Collapse Inner Classes to the top inner class level
            for (int index = innerClassDepthList.size() - 1; index >= 1; index--) {
                HashSet<ClassInfoType> innerClassesAtDepth = innerClassDepthList.get(index);
                HashSet<ClassInfoType> innerClassesAtHigherDepth = innerClassDepthList.get(index - 1);

                for (ClassInfoType cit : innerClassesAtDepth) {
                    final String innerClassName = cit.getClassName();
                    final String outerClassName = innerClassName.substring(0, innerClassName.lastIndexOf("$"));

                    ClassInfoType higherInnerClass = null;
                    for (ClassInfoType uIC : innerClassesAtHigherDepth) {
                        if (uIC.getClassName().equals(outerClassName)) {
                            higherInnerClass = uIC;
                            break;
                        }
                    }

                    if (higherInnerClass == null) {
                        // Didn't find the inner class containing its nested inner class.
                        FFDCFilter.processException(new IllegalStateException("Could not locate outer-type \"" + outerClassName + "\" for inner-type \"" + innerClassName + "\""),
                                                    EntityMappingsScanner.class.getName() + ".processInnerClasses", "173");
                    } else {
                        // Now we need to walk the higher level inner class's inner classes list until we find the
                        // placeholder for he current inner class
                        InnerClassesType ict = higherInnerClass.getInnerclasses();
                        if (ict == null) {
                            ict = new InnerClassesType();
                            higherInnerClass.setInnerclasses(ict);
                        }

                        final List<ClassInfoType> innerClassList = ict.getInnerclass();
                        ClassInfoType replaceThis = null;
                        for (ClassInfoType iclCit : innerClassList) {
                            if (iclCit.getClassName().equals(innerClassName)) {
                                replaceThis = iclCit;
                                break;
                            }
                        }

                        if (replaceThis == null) {
                            innerClassList.remove(replaceThis);
                        }
                        innerClassList.add(cit);
                    }
                }
            }
        }

        // We have collapsed all of the nested inner classes, now to associate first-level inner classes with their
        // outer class that is a regular class
        HashSet<ClassInfoType> innerClassesAtDepth = innerClassDepthList.get(0);
        for (ClassInfoType innerCit : innerClassesAtDepth) {
            final String innerClassName = innerCit.getClassName();
            final String outerClassName = innerClassName.substring(0, innerClassName.lastIndexOf("$"));

            for (ClassInfoType cit : citSet) {
                if (cit.getClassName().equals(outerClassName)) {
                    InnerClassesType ict = cit.getInnerclasses();
                    if (ict == null) {
                        ict = new InnerClassesType();
                        cit.setInnerclasses(ict);
                    }

                    final List<ClassInfoType> innerClassList = ict.getInnerclass();
                    ClassInfoType replaceThis = null;
                    for (ClassInfoType iclCit : innerClassList) {
                        if (iclCit.getClassName().equals(innerClassName)) {
                            replaceThis = iclCit;
                            break;
                        }
                    }

                    if (replaceThis != null) {
                        innerClassList.remove(replaceThis);
                    }
                    innerClassList.add(innerCit);
                }
            }
        }

        // Remove the inner classes from the list of outer classes.
        citSet.removeAll(innerClassSet);
    }

    private Set<ClassInfoType> processExplodedJarFormat(Path path) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();
        final HashSet<Path> archiveFiles = new HashSet<Path>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isRegularFile(file) && Files.size(file) > 0
                        && file.getFileName().toString().endsWith(".class")) {
                        archiveFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path p : archiveFiles) {
                String cName = path.relativize(p).toString().replace("/", ".");
                cName = cName.substring(0, cName.length() - 6); // Remove ".class" from name

                try (InputStream is = Files.newInputStream(p)) {
                    citSet.add(scanByteCodeFromInputStream(cName, is));
                } catch (Throwable t) {
                    throw new ClassScannerException(t);
                }
            }
        } catch (ClassScannerException cse) {
            FFDCFilter.processException(cse, EntityMappingsScanner.class.getName() + ".processExplodedJarFormat", "258");
            throw cse;
        } catch (Throwable t) {
            FFDCFilter.processException(t, EntityMappingsScanner.class.getName() + ".processExplodedJarFormat", "261");
            throw new ClassScannerException(t);
        }

        return citSet;
    }

    private Set<ClassInfoType> processUnexplodedFile(Path path) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();
        final HashSet<Path> archiveFiles = new HashSet<Path>();

        if (path == null) {
            throw new ClassScannerException("Null argument is invalid for method processUnexplodedFile().");
        }

        // URL referring to a jar file is the only legal option here.
        try {
            try (FileSystem fs = FileSystems.getFileSystem(path.toUri())) {
                for (Path jarRootPath : fs.getRootDirectories()) {
                    Files.walkFileTree(jarRootPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (Files.isRegularFile(file) && Files.size(file) > 0
                                && file.getFileName().toString().endsWith(".class")) {
                                archiveFiles.add(file);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                for (Path p : archiveFiles) {
                    String cName = path.relativize(p).toString().replace("/", ".");
                    cName = cName.substring(0, cName.length() - 6); // Remove ".class" from name

                    try (InputStream is = Files.newInputStream(p)) {
                        citSet.add(scanByteCodeFromInputStream(cName, is));
                    } catch (Throwable t) {
                        throw new ClassScannerException(t);
                    }
                }
            }
        } catch (ClassScannerException cse) {
            FFDCFilter.processException(cse, EntityMappingsScanner.class.getName() + ".processUnexplodedFile", "304");
            throw cse;
        } catch (Throwable t) {
            FFDCFilter.processException(t, EntityMappingsScanner.class.getName() + ".processUnexplodedFile", "307");
            throw new ClassScannerException(t);
        }

        return citSet;
    }

    private Set<ClassInfoType> processJarFileURL(URL jarFileURL) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();

        try {
            final JarURLConnection conn = (JarURLConnection) jarFileURL.openConnection();
            try (final JarFile jarFile = conn.getJarFile()) {
                final Enumeration<JarEntry> jarEntryEnum = jarFile.entries();
                while (jarEntryEnum.hasMoreElements()) {
                    final JarEntry jEntry = jarEntryEnum.nextElement();
                    final String jEntryName = jEntry.getName();
                    if (jEntryName != null && jEntryName.endsWith(".class")) {
                        final String name = jEntryName.substring(0, jEntryName.length() - 6).replace("/", ".");
                        final InputStream jis = jarFile.getInputStream(jEntry);
                        citSet.add(scanByteCodeFromInputStream(name, jis));
                    }
                }
            }
        } catch (IOException e) {
            FFDCFilter.processException(e, EntityMappingsScanner.class.getName() + ".processJarFileURL", "291");
            throw new ClassScannerException(e);
        }

        return citSet;
    }

    private Set<ClassInfoType> processJarFormatInputStreamURL(URL jarURL) throws ClassScannerException {
        final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();

        try (JarInputStream jis = new JarInputStream(jarURL.openStream(), false)) {
            JarEntry jarEntry = null;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                String name = jarEntry.getName();
                if (name != null && name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6).replace("/", ".");
                    citSet.add(scanByteCodeFromInputStream(name, jis));
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, EntityMappingsScanner.class.getName() + ".processJarFormatInputStreamURL", "311");
            throw new ClassScannerException(t);
        }

        return citSet;
    }

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final byte[] buffer = new byte[4096];

    private ClassInfoType scanByteCodeFromInputStream(String cName, InputStream is) throws ClassScannerException {
        baos.reset();

        try {
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer, 0, 4096)) > -1) {
                if (bytesRead > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
            }

            byte[] classByteCode = baos.toByteArray();
            baos.reset();

            return AsmClassAnalyzer.analyzeClass(cName, classByteCode, ioResolver);
        } catch (Throwable t) {
            FFDCFilter.processException(t, EntityMappingsScanner.class.getName() + ".scanByteCodeFromInputStream", "337");
            throw new ClassScannerException(t);
        }
    }
}
