/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * The configuration for a PersistenceServiceUnit. The data contained in this object is used at
 * PersistenceServiceUnit creation time and is then discarded.</P> The following properties are
 * <b>required:</b>
 * <ul>
 * <li>jtaDataSource
 * <li>nonJtaDataSource
 * <li>unitName
 * <li>classes
 * <li>consumerLoader
 * </ul>
 * </p> If required properties are not provided, an IllegalArgumentException will be thrown when
 * trying to create PersistenceServiceUnit.
 * 
 */
public final class PersistenceServiceUnitConfig {
    // Required
    private DataSource jtaDataSource;
    // Required
    private DataSource nonJaDataSource;
    private DataSource privilegedDataSource;

    private Map<String, Object> properties;
    private List<String> mappingFileNames;
    private List<InMemoryMappingFile> inMemoryMappingFiles;

    // Required
    private List<String> classes;
    // Required
    private ClassLoader consumerLoader;

    private boolean createOrUpdateTables = false;

    private Boolean allowUnicode;

    public PersistenceServiceUnitConfig() {}

    public PersistenceServiceUnitConfig(PersistenceServiceUnitConfig other) {
        allowUnicode = other.allowUnicode;
        classes = new ArrayList<String>(other.getClasses());
        consumerLoader = other.consumerLoader;
        createOrUpdateTables = other.createOrUpdateTables;
        inMemoryMappingFiles = new ArrayList<InMemoryMappingFile>(other.getInMemoryMappingFiles());
        jtaDataSource = other.jtaDataSource;
        mappingFileNames = new ArrayList<String>(other.getMappingFileNames());
        nonJaDataSource = other.nonJaDataSource;
        privilegedDataSource = other.privilegedDataSource;
        properties = new HashMap<String, Object>(other.getProperties());
    }

    /**
     * @return the createTables
     */
    public boolean getCreateOrUpdateTables() {
        return createOrUpdateTables;
    }

    /**
     * @param p
     *            the createTables to set
     */
    public void setCreateOrUpdateTables(boolean p) {
        this.createOrUpdateTables = p;
    }

    /**
     * @return the jtaDataSource
     */
    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }

    /**
     * @param jtaDataSource
     *            the jtaDataSource to set
     */
    public void setJtaDataSource(DataSource jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }

    /**
     * @return the nonJaDataSource
     */
    public DataSource getNonJaDataSource() {
        return nonJaDataSource;
    }

    /**
     * @param nonJaDataSource
     *            the nonJaDataSource to set
     */
    public void setNonJtaDataSource(DataSource nonJaDataSource) {
        this.nonJaDataSource = nonJaDataSource;
    }

    /**
     * @return the privilegedDataSource
     */
    public DataSource getPrivilegedDataSource() {
        return privilegedDataSource;
    }

    /**
     * @param privilegedDataSource
     *            the privilegedDataSource to set
     */
    public void setPrivilegedDataSource(DataSource privilegedDataSource) {
        this.privilegedDataSource = privilegedDataSource;
    }

    /**
     * @return the properties
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties() {
        return properties == null ? Collections.EMPTY_MAP : properties;
    }

    /**
     * @param properties
     *            the properties to set
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Stringified path to orm mapping files.
     * 
     * @return the mappingFileNames
     */
    public List<String> getMappingFileNames() {
        return (mappingFileNames != null ? mappingFileNames : Collections.<String> emptyList());
    }

    /**
     * @param mappingFileNames
     *            A list of fully qualified orm.xml files
     */
    public void setMappingFileNames(List<String> mappingFileNames) {
        this.mappingFileNames = mappingFileNames;
    }

    public List<InMemoryMappingFile> getInMemoryMappingFiles() {
        return (inMemoryMappingFiles != null ? inMemoryMappingFiles : Collections.<InMemoryMappingFile> emptyList());
    }

    public void setInMemoryMappingFiles(List<InMemoryMappingFile> files) {
        this.inMemoryMappingFiles = files;
    }

    /**
     * Stringified class names of all Entities, Embeddables, and MappedSuperClasses.
     * 
     * @return the classes
     */
    public List<String> getClasses() {
        return classes;
    }

    /**
     * @param classes
     *            Stringified class names of all Entities, Embeddables, and MappedSuperClasses.
     */
    public void setClasses(List<String> c) {
        classes = c;
    }

    /**
     * A ClassLoader that can be used to load the resources returned by
     * 
     * {@link com.ibm.wsspi.persistence.PersistenceServiceUnitConfig#getMappingFileNames()} and {@link com.ibm.wsspi.persistence.PersistenceServiceUnitConfig#getClasses()}
     * 
     * @return the consumerLoader
     */
    public ClassLoader getConsumerLoader() {
        return consumerLoader;
    }

    /**
     * @param loader
     *            the consumerLoader to set
     */
    public void setConsumerLoader(ClassLoader loader) {
        this.consumerLoader = loader;
    }

    /**
     * If true, the PersistenceService will not validate consumers' input for validity.
     * <p>
     * <b>Note:</b> This is a tertiary property. If True/False that value wins. If unspecified the
     * service will detect the target database / driver and will decide whether or not to allow
     * unicode.
     */
    public void setAllowUnicode(Boolean b) {
        allowUnicode = b;
    }

    public Boolean getAllowUnicode() {
        return allowUnicode;
    }

    // TODO (151910) -- FIXME
    // - handle no jta/nonjta when generating schema
    // - check if mappingfile exists
    //
    public void validate() {
        // boolean fail = false;
        // StringBuilder invalidMappingFile = new StringBuilder();
        // StringBuilder missingProps =
        // new StringBuilder(
        // "Invalid configuration. The following properties were not specified, but are required : [");
        // List<String> unspecifiedProps = new ArrayList<String>();
        // if (jtaDataSource == null) {
        // unspecifiedProps.add("jtaDataSource");
        // fail = true;
        // }
        // if (nonJaDataSource == null) {
        // unspecifiedProps.add("nonJaDataSource");
        // fail = true;
        // }
        // if (unitName == null || unitName.isEmpty()) {
        // unspecifiedProps.add("unitName");
        // fail = true;
        // }
        // if (classes == null || classes.size() == 0) {
        // unspecifiedProps.add("classes");
        // fail = true;
        // }
        // if (consumerLoader == null) {
        // unspecifiedProps.add("consumerLoader");
        // fail = true;
        // }
        // if (mappingFileNames != null) {
        // invalidMappingFile.append("invalid mappingFiles - ");
        // List<String> mappingFiles = new ArrayList<String>();
        // for (String file : mappingFileNames) {
        // File f = new File(file);
        // //TODO -- dopriv
        // if (!f.exists()) {
        // fail = true;
        // mappingFiles.add(file);
        // }
        // }
        // if (mappingFiles.size() > 0) {
        // invalidMappingFile.append(mappingFiles.toString());
        // invalidMappingFile.append("\n");
        // }
        // }
        // missingProps.append(invalidMappingFile.toString());
        //
        // if (fail) {
        // throw new IllegalArgumentException(missingProps.toString());
        // }
    }

}
