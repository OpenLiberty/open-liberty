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

package com.ibm.ws.jpa.diagnostics.puscanner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.ClassScannerException;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScanner;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScannerResults;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInformationType;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsException;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsFactory;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEmbeddable;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntity;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntityMappings;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IMappedSuperclass;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IPersistenceUnitDefaults;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IPersistenceUnitMetadata;

public final class PersistenceUnitScanner {
    public static PersistenceUnitScannerResults scanPersistenceUnit(PersistenceUnitInfo pUnit) throws PersistenceUnitScannerException {
        if (pUnit == null) {
            throw new PersistenceUnitScannerException("Cannot accept a null value for PersistenceUnitInfo argument.");
        }

        PersistenceUnitScanner puScanner = new PersistenceUnitScanner(pUnit);
        return puScanner.scan();
    }

    final private PersistenceUnitInfo pUnit;
    final private ClassLoader tempCL;

    final private URL puRoot;
    final private List<URL> jarFileList = new ArrayList<URL>();

    // Scanner Result Containers
    final List<EntityMappingsDefinition> entityMappingsDefinitionsList = new ArrayList<EntityMappingsDefinition>();
    final List<EntityMappingsScannerResults> classScannerResults = new ArrayList<EntityMappingsScannerResults>();

    private PersistenceUnitScanner(PersistenceUnitInfo pUnit) {
        this.pUnit = pUnit;
        this.tempCL = pUnit.getNewTempClassLoader();

        this.puRoot = pUnit.getPersistenceUnitRootUrl();
        if (pUnit.getJarFileUrls() != null) {
            jarFileList.addAll(pUnit.getJarFileUrls());
        }
    }

    private PersistenceUnitScannerResults scan() throws PersistenceUnitScannerException {
        scanEntityMappings();
        scanClasses();

        // Discovered all available JPA ORM XML files, and scanned all classes in the persistence unit
        // root and jar-file references.  Trim the scanned classes found that are unrelated to the JPA ORM Model.
        trim();

        return new PersistenceUnitScannerResults(pUnit, entityMappingsDefinitionsList, classScannerResults);
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

        try {
            // Search for the default "orm.xml" in the persistence unit root and jar files
            mappingFilesLocated.addAll(findORMResources("META-INF/orm.xml"));

            // Search for all other declared mapping files
            final List<String> puiMappingFiles = pUnit.getMappingFileNames();
            if (puiMappingFiles != null && !puiMappingFiles.isEmpty()) {
                for (String mappingFile : puiMappingFiles) {
                    if ("META-INF/orm.xml".equals(mappingFile)) {
                        continue; // Skip, already processed the default orm.xml
                    }
                    mappingFilesLocated.addAll(findORMResources(mappingFile));
                }
            }

            // Process discovered mapping files
            for (final URL mappingFileURL : mappingFilesLocated) {
                EntityMappingsDefinition emapdef = EntityMappingsFactory.parseEntityMappings(mappingFileURL);
                entityMappingsDefinitionsList.add(emapdef);
            }
        } catch (IOException ioe) {
            throw new PersistenceUnitScannerException(ioe);
        } catch (EntityMappingsException eme) {
            throw new PersistenceUnitScannerException(eme);
        }
    }

    /**
     * Scan classes in persistence unit root and referenced jar-files
     */
    private void scanClasses() throws PersistenceUnitScannerException {

        // Persistence Unit Root
        try {
            // Persistence Unit Root
            classScannerResults.add(EntityMappingsScanner.scanTargetArchive(puRoot, tempCL));

            // Listed Jar Files
            for (final URL jarFileURL : jarFileList) {
                classScannerResults.add(EntityMappingsScanner.scanTargetArchive(jarFileURL, tempCL));
            }
        } catch (ClassScannerException cse) {
            throw new PersistenceUnitScannerException(cse);
        }
    }

    /**
     * Finds all specified ORM files, by name, constrained in location by the persistence unit root and jar files.
     *
     * @param ormFileName The name of the ORM file to search for
     * @return A List of URLs of resources found by the ClassLoader. Will be an empty List if none are found.
     * @throws IOException
     */
    private List<URL> findORMResources(String ormFileName) throws IOException {
        boolean isMetaInfoOrmXML = "META-INF/orm.xml".equals(ormFileName);

        final ArrayList<URL> retArr = new ArrayList<URL>();

        Enumeration<URL> ormEnum = pUnit.getClassLoader().getResources(ormFileName);
        while (ormEnum.hasMoreElements()) {
            final URL url = ormEnum.nextElement();
            final String urlExtern = url.toExternalForm(); //  ParserUtils.decode(url.toExternalForm());

            if (!isMetaInfoOrmXML) {
                // If it's not "META-INF/orm.xml", then the mapping files may be present anywhere in the classpath.
                retArr.add(url);
                continue;
            }

            // Check against persistence unit root
            if (urlExtern.startsWith(puRoot.toExternalForm())) {
                retArr.add(url);
                continue;
            }

            // Check against Jar files, if any
            for (URL jarUrl : jarFileList) {
                final String jarExtern = jarUrl.toExternalForm();
                if (urlExtern.startsWith(jarExtern)) {
                    retArr.add(url);
                    continue;
                }
            }
        }

        return retArr;
    }

    /*
     * JPA 2.1: 8.2.1.6 mapping-file, jar-file, class, exclude-unlisted-classes
     *
     * The following classes must be implicitly or explicitly denoted as managed persistence classes to be
     * included within a persistence unit: entity classes; embeddable classes; mapped superclasses;
     * converter classes.
     *
     * 8.2.1.6.1 Annotated Classes in the Root of the Persistence Unit
     * All classes contained in the root of the persistence unit are searched for annotated managed persistence
     * classes—classes with the Entity, Embeddable, MappedSuperclass, or Converter annotation—
     * and any mapping metadata annotations found on these classes will be processed, or they will be
     * mapped using the mapping annotation defaults. If it is not intended that the annotated persistence
     * classes contained in the root of the persistence unit be included in the persistence unit, the
     * exclude-unlisted-classes element must be specified as true. The
     * exclude-unlisted-classes element is not intended for use in Java SE environments.
     *
     * 8.2.1.6.2 Object/relational Mapping Files
     * An object/relational mapping XML file contains mapping information for the classes listed in it.
     * An object/relational mapping XML file named orm.xml may be specified in the META-INF directory
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
     *
     * 8.2.1.6.3 Jar Files
     * One or more JAR files may be specified using the jar-file elements instead of, or in addition to the
     * mapping files specified in the mapping-file elements. If specified, these JAR files will be searched
     * for managed persistence classes, and any mapping metadata annotations found on them will be processed,
     * or they will be mapped using the mapping annotation defaults defined by this specification.
     * Such JAR files are specified relative to the directory or jar file that contains[89] the root of the persistence
     * unit.[90]
     *
     * The following examples illustrate the use of the jar-file element to reference additional persistence
     * classes. These examples use the convention that a jar file with a name terminating in “PUnit” contains
     * the persistence.xml file and that a jar file with a name terminating in “Entities” contains
     * additional persistence classes.
     *
     * 8.2.1.6.4 List of Managed Classes
     * A list of named managed persistence classes—entity classes, embeddable classes, mapped superclasses,
     * and converter classes—may be specified instead of, or in addition to, the JAR files and mapping files.
     * Any mapping metadata annotations found on these classes will be processed, or they will be mapped
     * using the mapping annotation defaults. The class element is used to list a managed persistence class.
     *
     * A list of all named managed persistence classes must be specified in Java SE environments to insure
     * portability. Portable Java SE applications should not rely on the other mechanisms described here to
     * specify the managed persistence classes of a persistence unit. Persistence providers may require that the
     * set of entity classes and classes that are to be managed must be fully enumerated in each of the persistence.
     * xml files in Java SE environments.
     *
     * 12.1 Use of the XML Descriptor
     *
     * If the xml-mapping-metadata-complete subelement is specified, the complete set of mapping
     * metadata for the persistence unit is contained in the XML mapping files for the persistence unit, and any
     * persistence annotations on the classes are ignored.
     */
    private void trim() {
        // Relevant data from <persistence-unit>
        final boolean excludeUnlistedClasses = pUnit.excludeUnlistedClasses();
        final List<String> managedClassNames = pUnit.getManagedClassNames();
        final boolean xmlMetaDataComplete = checkXMLMetadataComplete();

        // Identify all persistence-involved classes identified by the ORM XML files.  Include entities,
        // embeddables, mapped superclasses, idclasses, entitylisteners, etc in the set.
        final HashSet<String> persistenceAwareClassSet = new HashSet<String>();

        final HashSet<String> ormDefinedEntitySet = new HashSet<String>();
        final HashSet<String> metadataCompleteEntitySet = new HashSet<String>();

        final HashSet<String> ormDefinedEmbeddableSet = new HashSet<String>();
        final HashSet<String> metadataCompleteEmbeddableSet = new HashSet<String>();

        final HashSet<String> ormDefinedMappedSuperclassSet = new HashSet<String>();
        final HashSet<String> metadataCompleteMappedSuperclassSet = new HashSet<String>();

        for (EntityMappingsDefinition emd : entityMappingsDefinitionsList) {
            final IEntityMappings entityMappings = emd.getEntityMappings();
            parseORMXMLDocument(entityMappings,
                                persistenceAwareClassSet,
                                ormDefinedEntitySet,
                                metadataCompleteEntitySet,
                                ormDefinedEmbeddableSet,
                                metadataCompleteEmbeddableSet,
                                ormDefinedMappedSuperclassSet,
                                metadataCompleteMappedSuperclassSet);
        }

        // All persistence-aware types from the discovered ORM XML documents have been identified.
        // If xml-metadata-complete has been flagged, then there is no need to scan classes for
        // annotations.

        // First, identify all classes found by the class scanner
        final Map<String, List<ClassInfoType>> allClassMap = new HashMap<String, List<ClassInfoType>>();
        final Map<ClassInfoType, EntityMappingsScannerResults> citEmsrMap = new HashMap<ClassInfoType, EntityMappingsScannerResults>();

        for (EntityMappingsScannerResults emsr : classScannerResults) {
            final ClassInformationType citInfo = emsr.getCit();
            final List<ClassInfoType> citList = citInfo.getClassInfo();
            if (citList == null || citList.size() == 0) {
                continue;
            }

            for (ClassInfoType cit : citList) {
                final String className = cit.getClassName();

                List<ClassInfoType> cList = allClassMap.get(className);
                if (cList == null) {
                    cList = new ArrayList<ClassInfoType>();
                    allClassMap.put(className, cList);
                }
                cList.add(cit);
                citEmsrMap.put(cit, emsr);
            }
        }

        // Trim the Class scanner report

        if (!xmlMetaDataComplete) {
            // Since no ORM XML file declared XML Metadata Complete, we need to scan classes for
            // annotations.  If exclude-unlisted-classes is set true, then only scan those classes
            // in the managed classes list -- otherwise scan every class in the persistence unit
            // root and designated jar-files.

            if (excludeUnlistedClasses) {
                // Excluding unlisted classes, which means only persistence capable classes defined by the
                // ORM files or by the persistence unit's managed class names are permitted.

                final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
                trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
                trimSet.removeAll(managedClassNames); // Remove classes listed in managed-class-names
                removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
            } else {
                // Not excluding unlisted classes, which means we scan all classes in the persistence unit
                // root and designed jar-files for annotated classes.
                final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
                trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
                trimSet.removeAll(managedClassNames); // Remove classes listed in managed-class-names

                for (final String className : allClassMap.keySet()) {
                    if (persistenceAwareClassSet.contains(className) || managedClassNames.contains(className)) {
                        continue;
                    }

                    final List<ClassInfoType> citList = allClassMap.get(className);
                    boolean hasJpaAnnotations = false;

                    classsearch: for (ClassInfoType cit : citList) {
                        AnnotationsType ait = cit.getAnnotations();
                        if (ait != null) {
                            for (AnnotationInfoType aInfT : ait.getAnnotation()) {
                                String type = aInfT.getType();
                                if (type != null && type.startsWith("javax.persistence.")) {
                                    hasJpaAnnotations = true;
                                    break classsearch;
                                }
                            }
                        }
                    }

                    if (hasJpaAnnotations) {
                        trimSet.add(className);
                    }
                }

                removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
            }
        } else {
            // Trim out every class not listed in the ORM XML.
            final Set<String> trimSet = new HashSet<String>(allClassMap.keySet());
            trimSet.removeAll(persistenceAwareClassSet); // Remove every class identified in the ORM XML as persistence-aware
            removeScannedClassEntries(trimSet, allClassMap, citEmsrMap);
        }

    }

    private void removeScannedClassEntries(final Set<String> classNamesToRemove,
                                           final Map<String, List<ClassInfoType>> allClassMap,
                                           final Map<ClassInfoType, EntityMappingsScannerResults> citEmsrMap) {
        for (final String className : classNamesToRemove) {
            final List<ClassInfoType> citList = allClassMap.get(className);
            for (final ClassInfoType cit : citList) {
                final EntityMappingsScannerResults emsr = citEmsrMap.get(cit);
                final ClassInformationType c = emsr.getCit();
                final List<ClassInfoType> cList = c.getClassInfo();
                cList.remove(cit);
            }
        }
    }

    private boolean checkXMLMetadataComplete() {
        boolean xmlMetaDataComplete = false;
        for (EntityMappingsDefinition emd : entityMappingsDefinitionsList) {
            final IEntityMappings entityMappings = emd.getEntityMappings();
            final IPersistenceUnitMetadata puMetaData = entityMappings.getIPersistenceUnitMetadata();
            if (puMetaData == null) {
                continue;
            }
            if (puMetaData.isXmlMappingMetadataComplete()) {
                xmlMetaDataComplete = true;
            }
        }
        return xmlMetaDataComplete;
    }

    /*
     * Invariants: None of the arguments can be null.
     */
    private void parseORMXMLDocument(final IEntityMappings entityMappings,
                                     final Set<String> persistenceAwareClassSet,
                                     final Set<String> ormDefinedEntitySet,
                                     final Set<String> metadataCompleteEntitySet,
                                     final Set<String> ormDefinedEmbeddableSet,
                                     final Set<String> metadataCompleteEmbeddableSet,
                                     final Set<String> ormDefinedMappedSuperclassSet,
                                     final Set<String> metadataCompleteMappedSuperclassSet) {
        // Process Persistence Unit Defaults
        final IPersistenceUnitMetadata puMetaData = entityMappings.getIPersistenceUnitMetadata();
        if (puMetaData != null) {
            IPersistenceUnitDefaults puDefaults = puMetaData.getIPersistenceUnitDefaults();
            if (puDefaults != null) {
                persistenceAwareClassSet.addAll(puDefaults._getEntityListeners());
            }
        }

        // Process Entity definitions for entity class and JPA important classes
        final List<IEntity> entityList = entityMappings.getEntityList();
        if (entityList != null && entityList.size() > 0) {
            for (IEntity entity : entityList) {
                final String className = entity.getClazz();
                persistenceAwareClassSet.add(className);
                ormDefinedEntitySet.add(className);

                if (Boolean.TRUE.equals(entity.isMetadataComplete())) {
                    metadataCompleteEntitySet.add(className);
                }

                if (entity._getIDClass() != null) {
                    persistenceAwareClassSet.add(entity._getIDClass());
                }

                persistenceAwareClassSet.addAll(entity._getConverters());
                persistenceAwareClassSet.addAll(entity._getEntityListeners());
                persistenceAwareClassSet.addAll(entity._getNamedEntityGraphClasses());
                persistenceAwareClassSet.addAll(entity._getNamedNativeQueryClasses());
                persistenceAwareClassSet.addAll(entity._getSQLResultSetClasses());

                // TODO: Process attributes
            }
        }

        // Process Embeddable definitions for embeddable class and JPA important classes
        final List<IEmbeddable> embeddableList = entityMappings.getEmbeddableList();
        if (embeddableList != null && embeddableList.size() > 0) {
            for (IEmbeddable embeddable : embeddableList) {
                final String className = embeddable.getClazz();
                persistenceAwareClassSet.add(className);
                ormDefinedEmbeddableSet.add(className);

                if (Boolean.TRUE.equals(embeddable.isMetadataComplete())) {
                    metadataCompleteEmbeddableSet.add(className);
                }

                // TODO: Need to dive into the EmbeddableAttributes to find converters, etc.
            }
        }

        // Process MappedSuperclass definitions for mapped super class and JPA important classes
        final List<IMappedSuperclass> mappedSuperclassList = entityMappings.getMappedSuperclassList();
        if (mappedSuperclassList != null && mappedSuperclassList.size() > 0) {
            for (IMappedSuperclass msc : mappedSuperclassList) {
                final String className = msc.getClazz();
                persistenceAwareClassSet.add(className);
                ormDefinedMappedSuperclassSet.add(className);

                if (msc._getIDClass() != null) {
                    persistenceAwareClassSet.add(msc._getIDClass());
                }

                if (Boolean.TRUE.equals(msc.isMetadataComplete())) {
                    metadataCompleteMappedSuperclassSet.add(className);
                }

                // TODO: Need to dive into the Attributes to find converters, etc.
            }
        }

        // Named Native Query

        // NamedStoredProcedureQuery

        // SQL Result Set Mapping
    }
}
