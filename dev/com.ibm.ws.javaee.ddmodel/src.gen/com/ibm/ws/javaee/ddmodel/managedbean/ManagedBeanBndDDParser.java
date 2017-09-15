/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.managedbean;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ManagedBeanBndDDParser extends DDParser {
    public ManagedBeanBndDDParser(Container ddRootContainer, Entry ddEntry) throws DDParser.ParseException {
        super(ddRootContainer, ddEntry);
    }

    public com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd parse() throws ParseException {
        super.parseRootElement();
        return (com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd) rootParsable;
    }

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if ("managed-bean-bnd".equals(rootElementLocalName)) {
            return createXMLRootParsable();
        }
        throw new ParseException(invalidRootElement());
    }

    private ParsableElement createXMLRootParsable() throws ParseException {
        if (namespace == null) {
            throw new ParseException(missingDeploymentDescriptorNamespace());
        }
        String versionString = getAttributeValue("", "version");
        if (versionString == null) {
            throw new ParseException(missingDeploymentDescriptorVersion());
        }
        if ("http://websphere.ibm.com/xml/ns/javaee".equals(namespace)) {
            if ("1.0".equals(versionString)) {
                version = 10;
                return new com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanBndType(getDeploymentDescriptorPath());
            }
            if ("1.1".equals(versionString)) {
                version = 11;
                return new com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanBndType(getDeploymentDescriptorPath());
            }
            throw new ParseException(invalidDeploymentDescriptorVersion(versionString));
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(versionString));
    }
}
