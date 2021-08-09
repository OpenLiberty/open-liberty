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

package com.ibm.ws.jpa.diagnostics.ormparser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PersistenceUnitDocumentHandler extends DefaultHandler {
    private static final String ENTITYMAPPINGS_NAMESPACE_URI = "http://java.sun.com/xml/ns/persistence/orm";
    private static final String JCP_ENTITYMAPPINGS_NAMESPACE_URI = "http://xmlns.jcp.org/xml/ns/persistence/orm";

    private static final String ENTITYMAPPINGS_LOCAL_NAME = "entity-mappings";
    private static final String VERSION_ATTRIBUTE_NAME = "version";
    
    private String namespace = null;
    private String version = null;
    
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (ENTITYMAPPINGS_LOCAL_NAME.equalsIgnoreCase(localName) && 
                (ENTITYMAPPINGS_NAMESPACE_URI.equalsIgnoreCase(uri) || 
                 JCP_ENTITYMAPPINGS_NAMESPACE_URI.equalsIgnoreCase(uri))) {
            namespace = uri;
            version = atts.getValue("", VERSION_ATTRIBUTE_NAME);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getVersion() {
        return version;
    }
}
