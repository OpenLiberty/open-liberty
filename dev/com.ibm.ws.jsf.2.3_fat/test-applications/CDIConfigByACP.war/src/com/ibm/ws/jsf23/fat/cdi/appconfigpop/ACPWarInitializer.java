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
package com.ibm.ws.jsf23.fat.cdi.appconfigpop;

import javax.faces.application.ApplicationConfigurationPopulator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ACPWarInitializer extends ApplicationConfigurationPopulator {

    @Override
    public void populateApplicationConfiguration(Document document) {

        addFactory(document, "tag-handler-delegate-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomTagHandlerDelegateFactory");
        addFactory(document, "exception-handler-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomExceptionHandlerFactory");
        addFactory(document, "faces-context-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomFacesContextFactory");
        addFactory(document, "partial-view-context-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomPartialViewContextFactory");
        addFactory(document, "view-declaration-language-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomViewDeclarationLanguageFactory");
        addFactory(document, "facelet-cache-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.CustomFaceletCacheFactory");

        // Fixed in 2.3 - https://github.com/javaee/javaserverfaces-spec/issues/1241
        addFactory(document, "client-window-factory", "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window.CustomClientWindowFactory");

        String ns = document.getDocumentElement().getNamespaceURI();

        Element a = document.createElementNS(ns, "application");

        Element res = document.createElementNS(ns, "el-resolver");
        res.setTextContent("com.ibm.ws.jsf23.fat.cdi.common.managed.CustomELResolver");
        a.appendChild(res);

        Element rm = document.createElementNS(ns, "state-manager");
        rm.setTextContent("com.ibm.ws.jsf23.fat.cdi.common.managed.CustomStateManager");
        a.appendChild(rm);

        document.getChildNodes().item(0).appendChild(a);

        Element l = document.createElementNS(ns, "lifecycle");
        l.appendChild(createNode(document, "phase-listener", "com.ibm.ws.jsf23.fat.cdi.common.managed.CustomPhaseListener"));
        document.getChildNodes().item(0).appendChild(l);

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
