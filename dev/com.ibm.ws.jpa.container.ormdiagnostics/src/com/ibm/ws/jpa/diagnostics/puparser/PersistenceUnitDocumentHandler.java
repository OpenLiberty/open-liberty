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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PersistenceUnitDocumentHandler extends DefaultHandler {
    public final static String PERSISTENCE_LOCAL_NAME = "persistence";
    private static final String VERSION_ATTRIBUTE_NAME = "version";

    private JPA_Schema jpaSchema = null;
    
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {       
        if (PERSISTENCE_LOCAL_NAME.equalsIgnoreCase(localName) && 
                (Constants.SUN_NAMESPACE.equalsIgnoreCase(uri) || Constants.JCP_NAMESPACE.equalsIgnoreCase(uri))) {
            // TODO: Should resolve by schema location, not by what value is put in version field
            if (jpaSchema == null) {
                jpaSchema = JPA_Schema.resolveByVersion(atts.getValue("", VERSION_ATTRIBUTE_NAME));
            }
        }
    }
    
    public JPA_Schema getJpaSchema() {
        return jpaSchema;
    }

}
