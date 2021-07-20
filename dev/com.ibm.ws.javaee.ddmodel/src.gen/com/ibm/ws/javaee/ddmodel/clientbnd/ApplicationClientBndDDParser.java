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
package com.ibm.ws.javaee.ddmodel.clientbnd;

import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationClientBndDDParser extends DDParserBndExt {
    public ApplicationClientBndDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi) throws DDParser.ParseException {
        super( ddRootContainer, ddEntry, ApplicationClient.class,
               xmi,
               (xmi ? "ApplicationClientBinding" : "application-client-bnd"),
               NAMESPACE_CLIENT_BND_XMI,               
               XML_VERSION_MAPPINGS_10_12, 12);
    }

    @Override
    public ApplicationClientBndType parse() throws ParseException {
        super.parseRootElement();

        return (ApplicationClientBndType) rootParsable;
    }

    @Override
    protected ApplicationClientBndType createRootParsable() throws ParseException {
        return (ApplicationClientBndType) super.createRootParsable();
    }

    @Override
    protected ApplicationClientBndType createRoot() {
        return new ApplicationClientBndType( getDeploymentDescriptorPath(), isXMI() );
    }
}
