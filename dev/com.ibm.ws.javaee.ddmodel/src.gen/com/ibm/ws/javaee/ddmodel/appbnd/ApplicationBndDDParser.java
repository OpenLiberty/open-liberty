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
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationBndDDParser extends DDParserBndExt {
    public ApplicationBndDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi)
        throws ParseException {

        super( ddRootContainer, ddEntry, Application.class,
               xmi,
               (xmi ? "ApplicationBinding" : "application-bnd" ),
               NAMESPACE_APP_BND_XMI,
               XML_VERSION_MAPPINGS_10_12, 12 );        
    }

    @Override
    public ApplicationBndType parse() throws ParseException {
        super.parseRootElement();
        return (ApplicationBndType) rootParsable;
    }

    @Override
    protected ApplicationBndType createRootParsable() throws ParseException {
        return (ApplicationBndType) super.createRootParsable();
    }

    @Override    
    protected ApplicationBndType createRoot() {
        return new ApplicationBndType( getDeploymentDescriptorPath(), isXMI() );
    }
}
