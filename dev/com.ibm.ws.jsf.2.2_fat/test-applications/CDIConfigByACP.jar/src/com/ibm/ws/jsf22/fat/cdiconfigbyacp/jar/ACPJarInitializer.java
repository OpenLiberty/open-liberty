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
package com.ibm.ws.jsf22.fat.cdiconfigbyacp.jar;

import javax.faces.application.ApplicationConfigurationPopulator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ACPJarInitializer extends ApplicationConfigurationPopulator {

    @Override
    public void populateApplicationConfiguration(Document document) {

        addFactory(document, "visit-context-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomVisitContextFactory");
        addFactory(document, "application-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomApplicationFactory");
        addFactory(document, "external-context-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomExternalContextFactory");
        addFactory(document, "lifecycle-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomLifecycleFactory");
        addFactory(document, "render-kit-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomRenderKitFactory");

        String ns = document.getDocumentElement().getNamespaceURI();

        Element a = document.createElementNS(ns, "application");

        Element sel = document.createElementNS(ns, "system-event-listener");
        sel.appendChild(createNode(document, "system-event-listener-class", "com.ibm.ws.jsf22.fat.cdicommon.managed.CustomSystemEventListener"));
        sel.appendChild(createNode(document, "system-event-class", "javax.faces.event.PostConstructApplicationEvent"));
        a.appendChild(sel);

        Element nh = document.createElementNS(ns, "navigation-handler");
        nh.setTextContent("com.ibm.ws.jsf22.fat.cdicommon.managed.CustomNavigationHandler");
        a.appendChild(nh);

        Element rh = document.createElementNS(ns, "resource-handler");
        rh.setTextContent("com.ibm.ws.jsf22.fat.cdicommon.managed.CustomResourceHandler");
        a.appendChild(rh);

        document.getChildNodes().item(0).appendChild(a);

    }

    private void addFactory(Document document, String element, String value) {
        String ns = document.getDocumentElement().getNamespaceURI();
        Element a = document.createElementNS(ns, "factory");
        a.appendChild(createNode(document, element, value));

        document.getChildNodes().item(0).appendChild(a);
    }

    private Element createNode(Document doc, String element, String value) {
        String ns = doc.getDocumentElement().getNamespaceURI();
        Element e = doc.createElementNS(ns, element);
        e.appendChild(doc.createTextNode(value));
        return e;

    }

}
