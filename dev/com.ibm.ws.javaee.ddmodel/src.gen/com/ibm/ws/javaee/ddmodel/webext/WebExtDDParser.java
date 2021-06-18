/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class WebExtDDParser extends DDParserBndExt {
    public WebExtDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi)
        throws DDParser.ParseException {
        super( ddRootContainer, ddEntry, WebApp.class,
                xmi,
                (xmi ? "WebAppExtension" : "web-ext"),
                NAMESPACE_WEB_EXT_XMI,
                XML_VERSION_MAPPINGS_10_11, 11 );
    }

    @Override
    public WebExtType parse() throws ParseException {
        super.parseRootElement();
        return (WebExtType) rootParsable;
    }

    @Override
    protected WebExtType createRootParsable() throws ParseException {
        return (WebExtType) super.createRootParsable();
    }

    @Override
    protected WebExtType createRoot() {
        return new WebExtType( getDeploymentDescriptorPath(), isXMI() );
    }
}
