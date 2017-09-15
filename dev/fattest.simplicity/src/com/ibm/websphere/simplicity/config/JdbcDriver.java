/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Defines a JDBC driver
 * 
 * @author Tim Burns
 * 
 */
public class JdbcDriver extends ConfigElement implements ModifiableConfigElement {

    private static final Class<JdbcDriver> c = JdbcDriver.class;

    private String libraryRef;
    private Library nestedLibrary;
    private String javaxSqlConnectionPoolDataSource;
    private String javaxSqlDataSource;
    private String javaxSqlXADataSource;
    private String fatModify;

    private String bootstrapJdbcDriverVersion;

    /**
     * @return the ID of the shared library where this driver's classes are stored
     */
    public String getLibraryRef() {
        return this.libraryRef;
    }

    /**
     * @param libraryRef the ID of the shared library where this driver's classes are stored
     */
    @XmlAttribute
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = ConfigElement.getValue(libraryRef);
    }

    /**
     * @param library the shared library where this driver's classes are stored
     */
    @XmlTransient
    public void setLibrary(Library library) {
        if (library != null) {
            this.setLibraryRef(library.getId());
        }
    }

    @XmlElement(name = "library")
    public void setNestedLibrary(Library nestedLibrary) {
        this.nestedLibrary = nestedLibrary;
    }

    public Library getNestedLibrary() {
        return this.nestedLibrary;
    }

    @XmlAttribute(name = "javax.sql.ConnectionPoolDataSource")
    public void setJavaxSqlConnectionPoolDataSource(String javaxSqlConnectionPoolDataSource) {
        this.javaxSqlConnectionPoolDataSource = javaxSqlConnectionPoolDataSource;
    }

    public String getJavaxSqlConnectionPoolDataSource() {
        return this.javaxSqlConnectionPoolDataSource;
    }

    @XmlAttribute(name = "javax.sql.DataSource")
    public void setJavaxSqlDataSource(String javaxSqlDataSource) {
        this.javaxSqlDataSource = javaxSqlDataSource;
    }

    public String getJavaxSqlDataSource() {
        return this.javaxSqlDataSource;
    }

    @XmlAttribute(name = "javax.sql.XADataSource")
    public void setJavaxSqlXADataSource(String javaxSqlXADataSource) {
        this.javaxSqlXADataSource = javaxSqlXADataSource;
    }

    public String getJavaxSqlXADataSource() {
        return this.javaxSqlXADataSource;
    }

    @XmlAttribute(name = "fat.modify")
    public void setFatModify(String fatModify) {
        this.fatModify = fatModify;
    }

    public String getFatModify() {
        return fatModify;
    }

    /**
     * Modifies the element if the fat.modify="true" attribute was configured for this element.
     * 
     * @param config The ServerConfiguration instance.
     */
    @Override
    public void modify(ServerConfiguration config) throws Exception {
        if (fatModify != null && fatModify.toLowerCase().equals("true")) {
            updateJdbcDriverFromBootstrap(config);
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("JdbcDriver{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\", ");
        buf.append("libraryRef=\"" + (libraryRef == null ? "" : libraryRef) + "\"");
        if (javaxSqlConnectionPoolDataSource != null)
            buf.append("javax.sql.ConnectionPoolDataSource=\"" + javaxSqlConnectionPoolDataSource + "\" ");
        if (javaxSqlDataSource != null)
            buf.append("javax.sql.DataSource=\"" + javaxSqlDataSource + "\" ");
        if (javaxSqlXADataSource != null)
            buf.append("javax.sql.XADataSource=\"" + javaxSqlXADataSource + "\" ");
        if (nestedLibrary != null)
            buf.append(", " + nestedLibrary.toString());
        buf.append("}");
        return buf.toString();
    }

    public String getBootStrapJdbcDriverVersion() throws Exception {
        if (bootstrapJdbcDriverVersion == null) {
            Bootstrap bs = Bootstrap.getInstance();
            if (bs.getValue(BootstrapProperty.DB_DRIVERVERSION.getPropertyName()) == null) {
                bootstrapJdbcDriverVersion = BootstrapProperty.DB_DRIVERVERSION_DEFAULT.toString();
            } else {
                bootstrapJdbcDriverVersion = bs.getValue(BootstrapProperty.DB_DRIVERVERSION.getPropertyName());
            }
        }
        return this.bootstrapJdbcDriverVersion;
    }

    public void updateJdbcDriverFromBootstrap(ServerConfiguration config) throws Exception {
        Log.entering(c, "updateJdbcDriverFromBootstrap");
        Bootstrap bs = Bootstrap.getInstance();
        if (bs.getValue(BootstrapProperty.DB_DRIVERNAME.getPropertyName()) == null) {
            return;
        }

        String liberty_db_jars = bs.getValue(BootstrapProperty.LIBERTY_DBJARS.getPropertyName());
        String database_jdbcjar = bs.getValue(BootstrapProperty.DB_JDBCJAR.getPropertyName());

        // Update JdbcDriver nested library with bootstrapping properties
        Log.info(c, "updateJdbcDriverFromBootstrap", "Updating jdbcDriver " + getId());

        if (getNestedLibrary() != null) {
            Log.info(c, "updateJdbcDriverFromBootstrap", "jdbcDriver has nested library");
            // Update nested fileset
            if (getNestedLibrary().getNestedFileset() != null) {
                Fileset fileset = getNestedLibrary().getNestedFileset();
                fileset.setDir(liberty_db_jars);
                fileset.setIncludes(database_jdbcjar);
                Log.info(c, "updateJdbcDriverFromBootstrap", "Updated nested fileset for nested library");
            }
            // Update referenced fileset
            if (getNestedLibrary().getFilesetRef() != null) {
                Fileset fileset = config.getFilesetById(getNestedLibrary().getFilesetRef());
                fileset.setDir(liberty_db_jars);
                fileset.setIncludes(database_jdbcjar);
                Log.info(c, "updateJdbcDriverFromBootstrap", "Updated fileset referred to by nested library");
            }
        }
        if (getLibraryRef() != null) {
            // Update JdbcDriver referenced library with bootstrapping properties
            Log.info(c, "updateJdbcDriverFromBootstrap", "jdbcDriver has referenced library");
            Library library = config.getLibraries().getBy("id", getLibraryRef());
            if (library.getNestedFileset() != null) {
                Fileset fileset = library.getNestedFileset();
                fileset.setDir(liberty_db_jars);
                fileset.setIncludes(database_jdbcjar);
                Log.info(c, "updateJdbcDriverFromBootstrap", "Updated nested fileset for referenced library");
            }
            if (library.getFilesetRef() != null) {
                Fileset fileset = config.getFilesetById(library.getFilesetRef());
                fileset.setDir(liberty_db_jars);
                fileset.setIncludes(database_jdbcjar);
                Log.info(c, "updateJdbcDriverFromBootstrap", "Updated fileset referred to by referenced library");
            }

            // Handle a nested file element entry. If the database_jdbcjar entry in the properties file contains
            // more than one archive separated by spaces or commas (FileSet), convert the File into a FileSet.
            if (library.getNestedFile() != null) {
                File file = library.getNestedFile();
                String[] jars = database_jdbcjar.split("\\s*,\\s*|\\s+");
                if (jars != null && jars.length >= 1) {
                    library.setNestedFile(null);
                    Fileset fileset = new Fileset();
                    fileset.setDir(liberty_db_jars);
                    fileset.setIncludes(database_jdbcjar);
                    library.setNestedFileset(fileset);
                } else {
                    file.setName(database_jdbcjar);
                }

                Log.info(c, "updateJdbcDriverFromBootstrap", "Updated nested file for referenced library");
            }
        }
        Log.exiting(c, "updateJdbcDriverFromBootstrap");
    }
}
