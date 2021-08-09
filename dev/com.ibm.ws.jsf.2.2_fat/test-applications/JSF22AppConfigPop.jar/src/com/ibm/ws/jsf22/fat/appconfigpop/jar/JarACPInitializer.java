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
package com.ibm.ws.jsf22.fat.appconfigpop.jar;

import javax.faces.application.ApplicationConfigurationPopulator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This is a callback to do configuration via an empty DOM root which respresents faces.config.xml. Existing configuration options can be added, but cannot overwrite existing ones.
 * 
 * This callback tests the presence of META-INF/services/javax.faces.application.ApplicationConfigurationPopulator in a jar in a war file.
 * 
 */
public class JarACPInitializer extends ApplicationConfigurationPopulator {

    @Override
    public void populateApplicationConfiguration(Document document) {

        String ns = document.getDocumentElement().getNamespaceURI();

        Element a = document.createElementNS(ns, "application");
        Element sel = document.createElementNS(ns, "system-event-listener");
        sel.appendChild(createNode(document, "system-event-listener-class", "com.ibm.ws.jsf22.fat.appconfigpop.jar.SEListener"));
        sel.appendChild(createNode(document, "system-event-class", "javax.faces.event.PostConstructApplicationEvent"));
        a.appendChild(sel);
        document.getChildNodes().item(0).appendChild(a);

        Element l = document.createElementNS(ns, "lifecycle");
        l.appendChild(createNode(document, "phase-listener", "com.ibm.ws.jsf22.fat.appconfigpop.jar.PhaseTracker"));
        document.getChildNodes().item(0).appendChild(l);

    }

    private Element createNode(Document doc, String element, String value) {
        String ns = doc.getDocumentElement().getNamespaceURI();
        Element e = doc.createElementNS(ns, element);
        e.appendChild(doc.createTextNode(value));
        return e;

    }

}
