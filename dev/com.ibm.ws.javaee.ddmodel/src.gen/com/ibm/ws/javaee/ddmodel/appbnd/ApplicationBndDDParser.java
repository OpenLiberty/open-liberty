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
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationBndDDParser extends DDParserBndExt {
    public ApplicationBndDDParser(Container ddRootContainer, Entry ddEntry, boolean xmi) throws DDParser.ParseException {
        super(ddRootContainer, ddEntry, Application.class, xmi);
    }

    @Override
    public ApplicationBndType parse() throws ParseException {
        super.parseRootElement();
        return (ApplicationBndType) rootParsable;
    }

    @Override
    protected ApplicationBndType createRootParsable() throws ParseException {
        boolean isXMI = isXMI();

        if ( !isXMI ) {
            if ( "application-bnd".equals(rootElementLocalName)) {
                return createXMLRootParsable();
            }
        } else {
            if ( "ApplicationBinding".equals(rootElementLocalName) ) {
                return createXMIRootParsable();
            }
        }
        throw new ParseException( invalidRootElement() );
    }

    private ApplicationBndType createXMLRootParsable() throws ParseException {
        int ddVersion;

        String versionAttr = getAttributeValue("", "version");
        if ( versionAttr != null ) {
            if ( "1.0".equals(versionAttr) ) {
                ddVersion = 10;
            } else if ( "1.1".equals(versionAttr) ) {
                ddVersion = 11;
            } else if ( "1.2".equals(versionAttr) ) {
                ddVersion = 12;
            } else {
                throw new ParseException( unsupportedDescriptorVersion(versionAttr) );
            }
        } else {
            ddVersion = 12;
        }

        if ( namespace == null ) {
            namespace = NAMESPACE_IBM_JAVAEE;
        } else if ( !namespace.equals(NAMESPACE_IBM_JAVAEE) ) {
            warning( incorrectDescriptorNamespace(namespace, NAMESPACE_IBM_JAVAEE) );
            namespace = NAMESPACE_IBM_JAVAEE;            
        }
        idNamespace = null;
        version = ddVersion;

        return new ApplicationBndType(
                getDeploymentDescriptorPath(),
                !ApplicationBndType.IS_XMI );
    }

    private ApplicationBndType createXMIRootParsable() {
        if ( (namespace != null) && !namespace.equals(NAMESPACE_APP_BND_XMI) ) {
            warning( incorrectDescriptorNamespace(namespace, NAMESPACE_APP_BND_XMI) );
        }
        
        // This is correct: Parsing expects the default namespace to be null.        
        namespace = null;
        idNamespace = "http://www.omg.org/XMI";
        version = 9;

        return new ApplicationBndType(
                getDeploymentDescriptorPath(),
                ApplicationBndType.IS_XMI );
    }
}
