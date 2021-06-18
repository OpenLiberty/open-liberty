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
package com.ibm.ws.javaee.ddmodel.appext;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.ws.javaee.ddmodel.appbnd.ApplicationBndType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationExtDDParser extends DDParserBndExt {

    public ApplicationExtDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi) throws DDParser.ParseException {
        super( ddRootContainer, ddEntry, Application.class,
               xmi,
               (xmi ? "ApplicationExtension" : "application-ext"),
               NAMESPACE_APP_EXT_XMI,
               XML_VERSION_MAPPINGS_10_11, 11);
    }

    @Override
    public ApplicationExtType parse() throws ParseException {
        super.parseRootElement();
        return (ApplicationExtType) rootParsable;
    }

    @Override
    protected ApplicationExtType createRootParsable() throws ParseException {
        return (ApplicationExtType) super.createRootParsable();
    }
    
    @Override    
    protected ApplicationExtType createRoot() {
        return new ApplicationExtType( getDeploymentDescriptorPath(), isXMI() );
    }
}
