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
package com.ibm.ws.jpa.diagnostics.puscanner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.AsmClassAnalyzer;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.ClassScannerException;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.InnerOuterResolver;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InnerClassesType;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsFactory;

/**
 *
 */
public class PersistenceUnitScanner {
    public static PersistenceUnitScannerResults scan(List<PersistenceUnitInfo> puiList, HashMap<URL, String> pxmlMap) throws PersistenceUnitScannerException {
        final PersistenceUnitScanner scanner = new PersistenceUnitScanner(puiList, pxmlMap);
        return scanner.scan();
    }

    private final List<PersistenceUnitInfo> puiList;
    private final HashMap<URL, String> pxmlMap;

    private final Set<URL> urlSet = new HashSet<URL>();
    private final HashMap<URL, Set<ClassInfoType>> scannedClassesMap = new HashMap<URL, Set<ClassInfoType>>();

    private final HashMap<PersistenceUnitInfo, List<URL>> pu_ormFiles_map = new HashMap<PersistenceUnitInfo, List<URL>>();
    private final HashMap<URL, EntityMappingsDefinition> scanned_ormfile_map = new HashMap<URL, EntityMappingsDefinition>();
    private final HashMap<PersistenceUnitInfo, List<EntityMappingsDefinition>> pu_ormFileParsed_map = new HashMap<PersistenceUnitInfo, List<EntityMappingsDefinition>>();

    private final InnerOuterResolver ioResolver = new InnerOuterResolver();

    private PersistenceUnitScanner(List<PersistenceUnitInfo> puiList, HashMap<URL, String> pxmlMap) {
        this.puiList = puiList;
        this.pxmlMap = pxmlMap;
    }

    private PersistenceUnitScannerResults scan() throws PersistenceUnitScannerException {
        // Collect all URLs which are persistence unit roots and listed jar files.
        for (PersistenceUnitInfo pui : puiList) {
            urlSet.add(pui.getPersistenceUnitRootUrl());
            urlSet.addAll(pui.getJarFileUrls());

            pu_ormFiles_map.put(pui, new ArrayList<URL>());
            pu_ormFileParsed_map.put(pui, new ArrayList<EntityMappingsDefinition>());
        }

        scanEntityMappings();
        scanClasses();

        return new PersistenceUnitScannerResults(puiList, pxmlMap, urlSet, scannedClassesMap, scanned_ormfile_map, pu_ormFileParsed_map);
    }

    /**
     * Scan classes in persistence unit root and referenced jar-files
     */
    private void scanClasses() throws PersistenceUnitScannerException {
        try {
            for (URL url : urlSet) {
                final HashSet<ClassInfoType> citSet = new HashSet<ClassInfoType>();
                final String urlProtocol = url.getProtocol();
                if ("file".equalsIgnoreCase(urlProtocol)) {
                    // Protocol is "file", which either addresses a jar file or an exploded jar file
                    final Path taPath = Paths.get(url.toURI());
                    if (Files.isDirectory(taPath)) {
                        // Exploded Archive
                        citSet.addAll(processExplodedJarFormat(taPath));
                    } else {
                        // Unexploded Archive
                        citSet.addAll(processUnexplodedFile(taPath));
                    }
                } else if (url.toString().startsWith("jar:file")) {
                    citSet.addAll(processJarFileURL(url));
                } else {
                    // InputStream will be in jar format.
                    citSet.addAll(processJarFormatInputStreamURL(url));
                }

                processInnerClasses(citSet);
                scannedClassesMap.put(url, citSet);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, PersistenceUnitScanner.class.getName() + ".scanClasses", "118");
            throw new PersistenceUnitScannerException(e);
        }
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
            throw cse;
        } catch (Throwable t) {
            FFDCFilter.processException(t, PersistenceUnitScanner.class.getName() + ".processExplodedJarFormat", "153");
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
            throw cse;
        } catch (Throwable t) {
            FFDCFilter.processException(t, PersistenceUnitScanner.class.getName() + ".processUnexplodedFile", "199");
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
            FFDCFilter.processException(e, PersistenceUnitScanner.class.getName() + ".processJarFileURL", "227");
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
            FFDCFilter.processException(t, PersistenceUnitScanner.class.getName() + ".processJarFormatInputStreamURL", "247");
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
            FFDCFilter.processException(t, PersistenceUnitScanner.class.getName() + ".scanByteCodeFromInputStream", "273");
            throw new ClassScannerException(t);
        }
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
                        // Didn't find the inner class containing its nested inner class.  That's a problem.
                        // TODO: <spaceballs>Do Something!</spaceballs>
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

    /**
     * Scan Entity Mappings Files
     */
    private void scanEntityMappings() throws PersistenceUnitScannerException {
        /*
         * From the JPA 2.1 Specification:
         *
         * 8.2.1.6.2 Object/relational Mapping Files
         * An object/relational mapping XML file contains mapping information for the classes listed in it.
         *
         * A object/relational mapping XML file named orm.xml may be specified in the META-INF directory
         * in the root of the persistence unit or in the META-INF directory of any jar file referenced by the persistence.
         * xml. Alternatively, or in addition, one or more mapping files may be referenced by the
         * mapping-file elements of the persistence-unit element. These mapping files may be
         * present anywhere on the class path.
         *
         * An orm.xml mapping file or other mapping file is loaded as a resource by the persistence provider. If
         * a mapping file is specified, the classes and mapping information specified in the mapping file will be
         * used as described in Chapter 12. If multiple mapping files are specified (possibly including one or more
         * orm.xml files), the resulting mappings are obtained by combining the mappings from all of the files.
         * The result is undefined if multiple mapping files (including any orm.xml file) referenced within a single
         * persistence unit contain overlapping mapping information for any given class. The object/relational
         * mapping information contained in any mapping file referenced within the persistence unit must be disjoint
         * at the class-level from object/relational mapping information contained in any other such mapping
         * file.
         */
        final HashSet<URL> mappingFilesLocated = new HashSet<URL>();
        final HashSet<String> searchNames = new HashSet<String>();

        for (PersistenceUnitInfo pui : puiList) {
            try {
                mappingFilesLocated.clear();
                searchNames.clear();
                searchNames.add("META-INF/orm.xml");

                if (pui.getMappingFileNames() != null) {
                    searchNames.addAll(pui.getMappingFileNames());
                }

                for (String mappingFile : searchNames) {
                    mappingFilesLocated.addAll(findORMResources(pui, mappingFile));
                }

                final List<EntityMappingsDefinition> parsedOrmList = pu_ormFileParsed_map.get(pui);
                pu_ormFiles_map.get(pui).addAll(mappingFilesLocated);

                // Process discovered mapping files
                for (final URL mappingFileURL : mappingFilesLocated) {
                    if (scanned_ormfile_map.containsKey(mappingFileURL)) {
                        // Already processed this ORM File, no need to process it again.
                        parsedOrmList.add(scanned_ormfile_map.get(mappingFileURL));
                        continue;
                    }

                    EntityMappingsDefinition emapdef = EntityMappingsFactory.parseEntityMappings(mappingFileURL);
                    parsedOrmList.add(emapdef);
                    scanned_ormfile_map.put(mappingFileURL, emapdef);
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, PersistenceUnitScanner.class.getName() + ".scanEntityMappings", "460");
                throw new PersistenceUnitScannerException(e);
            }
        }
    }

    /**
     * Finds all specified ORM files, by name, constrained in location by the persistence unit root and jar files.
     *
     * @param ormFileName The name of the ORM file to search for
     * @return A List of URLs of resources found by the ClassLoader. Will be an empty List if none are found.
     * @throws IOException
     */
    private List<URL> findORMResources(PersistenceUnitInfo pui, String ormFileName) throws IOException {
        final boolean isMetaInfoOrmXML = "META-INF/orm.xml".equals(ormFileName);
        final ArrayList<URL> retArr = new ArrayList<URL>();

        Enumeration<URL> ormEnum = pui.getClassLoader().getResources(ormFileName);
        while (ormEnum.hasMoreElements()) {
            final URL url = ormEnum.nextElement();
            final String urlExtern = url.toExternalForm(); //  ParserUtils.decode(url.toExternalForm());

            if (!isMetaInfoOrmXML) {
                // If it's not "META-INF/orm.xml", then the mapping files may be present anywhere in the classpath.
                retArr.add(url);
                continue;
            }

            // Check against persistence unit root
            if (urlExtern.startsWith(pui.getPersistenceUnitRootUrl().toExternalForm())) {
                retArr.add(url);
                continue;
            }

            // Check against Jar files, if any
            for (URL jarUrl : pui.getJarFileUrls()) {
                final String jarExtern = jarUrl.toExternalForm();
                if (urlExtern.startsWith(jarExtern)) {
                    retArr.add(url);
                    continue;
                }
            }
        }

        return retArr;
    }

//    /*
//     * JPA 2.1: 8.2.1.6 mapping-file, jar-file, class, exclude-unlisted-classes
//     *
//     * The following classes must be implicitly or explicitly denoted as managed persistence classes to be
//     * included within a persistence unit: entity classes; embeddable classes; mapped superclasses;
//     * converter classes.
//     *
//     * 8.2.1.6.1 Annotated Classes in the Root of the Persistence Unit
//     * All classes contained in the root of the persistence unit are searched for annotated managed persistence
//     * classes—classes with the Entity, Embeddable, MappedSuperclass, or Converter annotation—
//     * and any mapping metadata annotations found on these classes will be processed, or they will be
//     * mapped using the mapping annotation defaults. If it is not intended that the annotated persistence
//     * classes contained in the root of the persistence unit be included in the persistence unit, the
//     * exclude-unlisted-classes element must be specified as true. The
//     * exclude-unlisted-classes element is not intended for use in Java SE environments.
//     *
//     * 8.2.1.6.2 Object/relational Mapping Files
//     * An object/relational mapping XML file contains mapping information for the classes listed in it.
//     * An object/relational mapping XML file named orm.xml may be specified in the META-INF directory
//     * in the root of the persistence unit or in the META-INF directory of any jar file referenced by the persistence.
//     * xml. Alternatively, or in addition, one or more mapping files may be referenced by the
//     * mapping-file elements of the persistence-unit element. These mapping files may be
//     * present anywhere on the class path.
//     *
//     * An orm.xml mapping file or other mapping file is loaded as a resource by the persistence provider. If
//     * a mapping file is specified, the classes and mapping information specified in the mapping file will be
//     * used as described in Chapter 12. If multiple mapping files are specified (possibly including one or more
//     * orm.xml files), the resulting mappings are obtained by combining the mappings from all of the files.
//     * The result is undefined if multiple mapping files (including any orm.xml file) referenced within a single
//     * persistence unit contain overlapping mapping information for any given class. The object/relational
//     * mapping information contained in any mapping file referenced within the persistence unit must be disjoint
//     * at the class-level from object/relational mapping information contained in any other such mapping
//     * file.
//     *
//     * 8.2.1.6.3 Jar Files
//     * One or more JAR files may be specified using the jar-file elements instead of, or in addition to the
//     * mapping files specified in the mapping-file elements. If specified, these JAR files will be searched
//     * for managed persistence classes, and any mapping metadata annotations found on them will be processed,
//     * or they will be mapped using the mapping annotation defaults defined by this specification.
//     * Such JAR files are specified relative to the directory or jar file that contains[89] the root of the persistence
//     * unit.[90]
//     *
//     * The following examples illustrate the use of the jar-file element to reference additional persistence
//     * classes. These examples use the convention that a jar file with a name terminating in “PUnit” contains
//     * the persistence.xml file and that a jar file with a name terminating in “Entities” contains
//     * additional persistence classes.
//     *
//     * 8.2.1.6.4 List of Managed Classes
//     * A list of named managed persistence classes—entity classes, embeddable classes, mapped superclasses,
//     * and converter classes—may be specified instead of, or in addition to, the JAR files and mapping files.
//     * Any mapping metadata annotations found on these classes will be processed, or they will be mapped
//     * using the mapping annotation defaults. The class element is used to list a managed persistence class.
//     *
//     * A list of all named managed persistence classes must be specified in Java SE environments to insure
//     * portability. Portable Java SE applications should not rely on the other mechanisms described here to
//     * specify the managed persistence classes of a persistence unit. Persistence providers may require that the
//     * set of entity classes and classes that are to be managed must be fully enumerated in each of the persistence.
//     * xml files in Java SE environments.
//     *
//     * 12.1 Use of the XML Descriptor
//     *
//     * If the xml-mapping-metadata-complete subelement is specified, the complete set of mapping
//     * metadata for the persistence unit is contained in the XML mapping files for the persistence unit, and any
//     * persistence annotations on the classes are ignored.
//     */
//    private void trim() {
//        // Relevant data from <persistence-unit>
//        final boolean excludeUnlistedClasses = pUnit.excludeUnlistedClasses();
//        final List<String> managedClassNames = pUnit.getManagedClassNames();
//        final boolean xmlMetaDataComplete = checkXMLMetadataComplete();
//
//        // Identify all persistence-involved classes identified by the ORM XML files.  Include entities,
//        // embeddables, mapped superclasses, idclasses, entitylisteners, etc in the set.
//        final HashSet<String> persistenceAwareClassSet = new HashSet<String>();
//
//        final HashSet<String> ormDefinedEntitySet = new HashSet<String>();
//        final HashSet<String> metadataCompleteEntitySet = new HashSet<String>();
//
//        final HashSet<String> ormDefinedEmbeddableSet = new HashSet<String>();
//        final HashSet<String> metadataCompleteEmbeddableSet = new HashSet<String>();
//
//        final HashSet<String> ormDefinedMappedSuperclassSet = new HashSet<String>();
//        final HashSet<String> metadataCompleteMappedSuperclassSet = new HashSet<String>();
//
//        for (EntityMappingsDefinition emd : entityMappingsDefinitionsList) {
//            final IEntityMappings entityMappings = emd.getEntityMappings();
//            parseORMXMLDocument(entityMappings,
//                                persistenceAwareClassSet,
//                                ormDefinedEntitySet,
//                                metadataCompleteEntitySet,
//                                ormDefinedEmbeddableSet,
//                                metadataCompleteEmbeddableSet,
//                                ormDefinedMappedSuperclassSet,
//                                metadataCompleteMappedSuperclassSet);
//        }
//
//        // All persistence-aware types from the discovered ORM XML documents have been identified.
//        // If xml-metadata-complete has been flagged, then there is no need to scan classes for
//        // annotations.
//
//        // First, identify all classes found by the class scanner
//        final Map<String, List<ClassInfoType>> allClassMap = new HashMap<String, List<ClassInfoType>>();
//        final Map<ClassInfoType, EntityMappingsScannerResults> citEmsrMap = new HashMap<ClassInfoType, EntityMappingsScannerResults>();
//
//        for (EntityMappingsScannerResults emsr : classScannerResults) {
//            final ClassInformationType citInfo = emsr.getCit();
//            final List<ClassInfoType> citList = citInfo.getClassInfo();
//            if (citList == null || citList.size() == 0) {
//                continue;
//            }
//
//            for (ClassInfoType cit : citList) {
//                final String className = cit.getClassName();
//
//                List<ClassInfoType> cList = allClassMap.get(className);
//                if (cList == null) {
//                    cList = new ArrayList<ClassInfoType>();
//                    allClassMap.put(className, cList);
//                }
//                cList.add(cit);
//                citEmsrMap.put(cit, emsr);
//            }
//        }
//
//        // Trim the Class scanner report
//
//        if (!xmlMetaDataComplete) {
//            // Since no ORM XML file declared XML Metadata Complete, we need to scan classes for
//            // annotations.  If exclude-unlisted-classes is set true, then only scan those classes
//            // in the managed classes list -- otherwise scan every class in the persistence unit
//            // root and designated jar-files.
//
//            if (excludeUnlistedClasses) {
//                // Excluding unlisted classes, which means only persistence capable classes defined by the
//                // ORM files or by the persistence unit's managed class names are permitted.
//
//                final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
//                trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
//                trimSet.removeAll(managedClassNames); // Remove classes listed in managed-class-names
//                removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
//            } else {
//                // Not excluding unlisted classes, which means we scan all classes in the persistence unit
//                // root and designed jar-files for annotated classes.
//                final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
//                trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
//                trimSet.removeAll(managedClassNames); // Remove classes listed in managed-class-names
//
//                for (final String className : allClassMap.keySet()) {
//                    if (persistenceAwareClassSet.contains(className) || managedClassNames.contains(className)) {
//                        continue;
//                    }
//
//                    final List<ClassInfoType> citList = allClassMap.get(className);
//                    boolean hasJpaAnnotations = false;
//
//                    classsearch: for (ClassInfoType cit : citList) {
//                        AnnotationsType ait = cit.getAnnotations();
//                        if (ait != null) {
//                            for (AnnotationInfoType aInfT : ait.getAnnotation()) {
//                                String type = aInfT.getType();
//                                if (type != null && type.startsWith("javax.persistence.")) {
//                                    hasJpaAnnotations = true;
//                                    break classsearch;
//                                }
//                            }
//                        }
//                    }
//
//                    if (hasJpaAnnotations) {
//                        trimSet.add(className);
//                    }
//                }
//
//                removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
//            }
//        } else {
//            // Trim out every class not listed in the ORM XML.
//            final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
//            trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
//            removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
//        }
//
//    }
//
//    private void removeScannedClassEntries(final Set<String> classNamesToRemove,
//                                           final Map<String, List<ClassInfoType>> allClassMap,
//                                           final Map<ClassInfoType, EntityMappingsScannerResults> citEmsrMap) {
//        for (final String className : classNamesToRemove) {
//            final List<ClassInfoType> citList = allClassMap.get(className);
//            for (final ClassInfoType cit : citList) {
//                final EntityMappingsScannerResults emsr = citEmsrMap.get(cit);
//                final ClassInformationType c = emsr.getCit();
//                final List<ClassInfoType> cList = c.getClassInfo();
//                cList.remove(cit);
//            }
//        }
//    }
//
//    private boolean checkXMLMetadataComplete() {
//        boolean xmlMetaDataComplete = false;
//        for (EntityMappingsDefinition emd : entityMappingsDefinitionsList) {
//            final IEntityMappings entityMappings = emd.getEntityMappings();
//            final IPersistenceUnitMetadata puMetaData = entityMappings.getIPersistenceUnitMetadata();
//            if (puMetaData == null) {
//                continue;
//            }
//            if (puMetaData.isXmlMappingMetadataComplete()) {
//                xmlMetaDataComplete = true;
//            }
//        }
//        return xmlMetaDataComplete;
//    }
//
//    /*
//     * Invariants: None of the arguments can be null.
//     */
//    private void parseORMXMLDocument(final IEntityMappings entityMappings,
//                                     final Set<String> persistenceAwareClassSet,
//                                     final Set<String> ormDefinedEntitySet,
//                                     final Set<String> metadataCompleteEntitySet,
//                                     final Set<String> ormDefinedEmbeddableSet,
//                                     final Set<String> metadataCompleteEmbeddableSet,
//                                     final Set<String> ormDefinedMappedSuperclassSet,
//                                     final Set<String> metadataCompleteMappedSuperclassSet) {
//        // Process Persistence Unit Defaults
//        final IPersistenceUnitMetadata puMetaData = entityMappings.getIPersistenceUnitMetadata();
//        if (puMetaData != null) {
//            IPersistenceUnitDefaults puDefaults = puMetaData.getIPersistenceUnitDefaults();
//            if (puDefaults != null) {
//                persistenceAwareClassSet.addAll(puDefaults._getEntityListeners());
//            }
//        }
//
//        // Process Entity definitions for entity class and JPA important classes
//        final List<IEntity> entityList = entityMappings.getEntityList();
//        if (entityList != null && entityList.size() > 0) {
//            for (IEntity entity : entityList) {
//                final String className = entity.getClazz();
//                persistenceAwareClassSet.add(className);
//                ormDefinedEntitySet.add(className);
//
//                if (Boolean.TRUE.equals(entity.isMetadataComplete())) {
//                    metadataCompleteEntitySet.add(className);
//                }
//
//                if (entity._getIDClass() != null) {
//                    persistenceAwareClassSet.add(entity._getIDClass());
//                }
//
//                persistenceAwareClassSet.addAll(entity._getConverters());
//                persistenceAwareClassSet.addAll(entity._getEntityListeners());
//                persistenceAwareClassSet.addAll(entity._getNamedEntityGraphClasses());
//                persistenceAwareClassSet.addAll(entity._getNamedNativeQueryClasses());
//                persistenceAwareClassSet.addAll(entity._getSQLResultSetClasses());
//
//                // TODO: Process attributes
//            }
//        }
//
//        // Process Embeddable definitions for embeddable class and JPA important classes
//        final List<IEmbeddable> embeddableList = entityMappings.getEmbeddableList();
//        if (embeddableList != null && embeddableList.size() > 0) {
//            for (IEmbeddable embeddable : embeddableList) {
//                final String className = embeddable.getClazz();
//                persistenceAwareClassSet.add(className);
//                ormDefinedEmbeddableSet.add(className);
//
//                if (Boolean.TRUE.equals(embeddable.isMetadataComplete())) {
//                    metadataCompleteEmbeddableSet.add(className);
//                }
//
//                // TODO: Need to dive into the EmbeddableAttributes to find converters, etc.
//            }
//        }
//
//        // Process MappedSuperclass definitions for mapped super class and JPA important classes
//        final List<IMappedSuperclass> mappedSuperclassList = entityMappings.getMappedSuperclassList();
//        if (mappedSuperclassList != null && mappedSuperclassList.size() > 0) {
//            for (IMappedSuperclass msc : mappedSuperclassList) {
//                final String className = msc.getClazz();
//                persistenceAwareClassSet.add(className);
//                ormDefinedMappedSuperclassSet.add(className);
//
//                if (msc._getIDClass() != null) {
//                    persistenceAwareClassSet.add(msc._getIDClass());
//                }
//
//                if (Boolean.TRUE.equals(msc.isMetadataComplete())) {
//                    metadataCompleteMappedSuperclassSet.add(className);
//                }
//
//                // TODO: Need to dive into the Attributes to find converters, etc.
//            }
//        }
//
//        // Named Native Query
//
//        // NamedStoredProcedureQuery
//
//        // SQL Result Set Mapping
//    }
}
