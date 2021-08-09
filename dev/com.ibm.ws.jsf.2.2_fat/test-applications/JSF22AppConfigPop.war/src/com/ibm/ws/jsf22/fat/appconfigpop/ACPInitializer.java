/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.appconfigpop;

import javax.faces.application.ApplicationConfigurationPopulator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a callback to do configuration via an empty DOM root which respresents faces.config.xml. Existing configuration options can be added, but cannot overwrite existing ones.
 * 
 * This callback tests the presence of META-INF/services/javax.faces.application.ApplicationConfigurationPopulator in the war file.
 * 
 */
public class ACPInitializer extends ApplicationConfigurationPopulator {

    @Override
    public void populateApplicationConfiguration(Document document) {
        String ns = document.getDocumentElement().getNamespaceURI();

        // Try to overrwrite faces-config.xml defined existingBean name,  you should not be able to.
        Element mb = document.createElementNS(ns, "managed-bean");
        mb.appendChild(createNode(document, "managed-bean-name", "existingBean"));
        mb.appendChild(createNode(document, "managed-bean-class", "com.ibm.ws.jsf22.fat.appconfigpop.AddedBean"));
        mb.appendChild(createNode(document, "managed-bean-scope", "request"));

        document.getChildNodes().item(0).appendChild(mb);

        // Simple bean test.  Uses same bean as above, but unique name.
        mb = document.createElementNS(ns, "managed-bean");
        mb.appendChild(createNode(document, "managed-bean-name", "addedBean"));
        mb.appendChild(createNode(document, "managed-bean-class", "com.ibm.ws.jsf22.fat.appconfigpop.AddedBean"));
        mb.appendChild(createNode(document, "managed-bean-scope", "session"));

        document.getChildNodes().item(0).appendChild(mb);

        //  Bean with resource injection.
        Element mp = document.createElementNS(ns, "managed-property");
        mp.appendChild(createNode(document, "property-name", "boolVal"));
        mp.appendChild(createNode(document, "property-class", "boolean"));
        mp.appendChild(createNode(document, "value", "true"));

        Element mp2 = document.createElementNS(ns, "managed-property");
        mp2.appendChild(createNode(document, "property-name", "addedBeanVal"));
        mp2.appendChild(createNode(document, "property-class", "java.lang.String"));
        mp2.appendChild(createNode(document, "value", "#{addedBean.message}"));

        mb = document.createElementNS(ns, "managed-bean");
        mb.appendChild(mp);
        mb.appendChild(mp2);
        mb.appendChild(createNode(document, "managed-bean-name", "mpBean"));
        mb.appendChild(createNode(document, "managed-bean-class", "com.ibm.ws.jsf22.fat.appconfigpop.MPBean"));
        mb.appendChild(createNode(document, "managed-bean-scope", "session"));

        document.getChildNodes().item(0).appendChild(mb);

        //   Navigation rule
        Element nr = document.createElementNS(ns, "navigation-rule");
        Element nc = document.createElementNS(ns, "navigation-case");

        nc.appendChild(createNode(document, "from-outcome", "testParams"));
        nc.appendChild(createNode(document, "to-view-id", "results.jsf"));
        nr.appendChild(nc);

        document.getChildNodes().item(0).appendChild(nr);

    }

    private Element createNode(Document doc, String element, String value) {
        String ns = doc.getDocumentElement().getNamespaceURI();
        Element e = doc.createElementNS(ns, element);
        e.appendChild(doc.createTextNode(value));
        return e;

    }

}
