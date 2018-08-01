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

package com.ibm.ws.jpa.diagnostics.puparser;

public enum JPA_Schema {
    JPA_10 (Constants.SUN_NAMESPACE, Constants.SCHEMA_LOCATION_10, "1.0", Constants.JPA_10_JAXB_PACKAGE),
    JPA_20 (Constants.SUN_NAMESPACE, Constants.SCHEMA_LOCATION_20, "2.0", Constants.JPA_20_JAXB_PACKAGE),
    JPA_21 (Constants.JCP_NAMESPACE, Constants.SCHEMA_LOCATION_21, "2.1", Constants.JPA_21_JAXB_PACKAGE),
    JPA_22 (Constants.JCP_NAMESPACE, Constants.SCHEMA_LOCATION_22, "2.2", Constants.JPA_22_JAXB_PACKAGE);
    
    private String namespace;
    private String schema;
    private String version;
    private String jaxbPackage;
    
    private JPA_Schema(String namespace, String schema, String version, String jaxbPackage) {
        this.namespace = namespace;
        this.schema = schema;
        this.jaxbPackage = jaxbPackage;
        this.version = version;
    }

    public final String getNamespace() {
        return namespace;
    }

    public final String getSchema() {
        return schema;
    }

    public final String getVersion() {
        return version;
    }
    
    public final String getJaxbPackage() {
        return jaxbPackage;
    }
    
    public static JPA_Schema resolveByNameAndSchema(String namespace, String schema) {
        if (namespace == null || schema == null) {
            return null;
        }
        
        for (JPA_Schema j : JPA_Schema.values()) {
            if (j.getNamespace().equals(namespace) && j.getSchema().equals(schema)) {
                return j;
            }
        }
        
        return null;
    }
    
    public static JPA_Schema resolveByVersion(String version) {
        if (version == null) {
            return null;
        }
        
        for (JPA_Schema j : JPA_Schema.values()) {
            if (j.getVersion().equals(version)) {
                return j;
            }
        }
        
        return null;
    }
    
}
