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
package com.ibm.ws.jsf22.fat.cdiconfigbyacp;

import javax.faces.application.ApplicationConfigurationPopulator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ACPWarInitializer extends ApplicationConfigurationPopulator {

    @Override
    public void populateApplicationConfiguration(Document document) {

        addFactory(document, "tag-handler-delegate-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomTagHandlerDelegateFactory");
        addFactory(document, "exception-handler-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomExceptionHandlerFactory");
        addFactory(document, "faces-context-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomFacesContextFactory");
        addFactory(document, "partial-view-context-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomPartialViewContextFactory");
        addFactory(document, "view-declaration-language-factory", "com.ibm.ws.jsf22.fat.cdicommon.managed.factories.CustomViewDeclarationLanguageFactory");

        String ns = document.getDocumentElement().getNamespaceURI();

        Element a = document.createElementNS(ns, "application");

        Element res = document.createElementNS(ns, "el-resolver");
        res.setTextContent("com.ibm.ws.jsf22.fat.cdicommon.managed.CustomELResolver");
        a.appendChild(res);

        Element rm = document.createElementNS(ns, "state-manager");
        rm.setTextContent("com.ibm.ws.jsf22.fat.cdicommon.managed.CustomStateManager");
        a.appendChild(rm);

        document.getChildNodes().item(0).appendChild(a);

        Element l = document.createElementNS(ns, "lifecycle");
        l.appendChild(createNode(document, "phase-listener", "com.ibm.ws.jsf22.fat.cdicommon.managed.CustomPhaseListener"));
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
