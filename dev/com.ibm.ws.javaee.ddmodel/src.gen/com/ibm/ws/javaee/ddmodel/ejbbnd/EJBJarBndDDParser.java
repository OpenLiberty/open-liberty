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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class EJBJarBndDDParser extends DDParserBndExt {
    public EJBJarBndDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi) throws DDParser.ParseException {
        super( ddRootContainer, ddEntry, EJBJar.class,
               xmi,
               (xmi ? "EJBJarBinding" : "ejb-jar-bnd"),
               NAMESPACE_EJB_BND_XMI,
               XML_VERSION_MAPPINGS_10_12, 12);
    }

    @Override
    public EJBJarBndType parse() throws ParseException {
        super.parseRootElement();

        return (EJBJarBndType) rootParsable;
    }

    @Override
    protected EJBJarBndType createRootParsable() throws ParseException {
        return (EJBJarBndType) super.createRootParsable();
    }

    @Override
    protected EJBJarBndType createRoot() {
        return new EJBJarBndType( getDeploymentDescriptorPath(), isXMI() );
    }
}
