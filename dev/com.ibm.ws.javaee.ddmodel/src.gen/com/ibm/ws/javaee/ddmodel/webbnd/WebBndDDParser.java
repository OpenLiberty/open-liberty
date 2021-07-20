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
package com.ibm.ws.javaee.ddmodel.webbnd;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class WebBndDDParser extends DDParserBndExt {
    public WebBndDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi)
        throws DDParser.ParseException {
        super( ddRootContainer, ddEntry, WebApp.class,
               xmi,
               (xmi ? "WebAppBinding" : "web-bnd"),
               NAMESPACE_WEB_BND_XMI,
               XML_VERSION_MAPPINGS_10_12, 12);
    }

    @Override
    public WebBndType parse() throws ParseException {
        super.parseRootElement();
        return (WebBndType) rootParsable;
    }
    
    @Override
    protected WebBndType createRootParsable() throws ParseException {
        return (WebBndType) super.createRootParsable();
    }

    @Override
    protected WebBndType createRoot() {
        return new WebBndType( getDeploymentDescriptorPath(), isXMI() );
    }
}
