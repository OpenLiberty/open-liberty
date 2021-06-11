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
package com.ibm.ws.javaee.ddmodel.managedbean;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ManagedBeanBndDDParser extends DDParser {
    public ManagedBeanBndDDParser(Container ddRootContainer, Entry ddEntry) throws DDParser.ParseException {
        super(ddRootContainer, ddEntry);
    }

    @Override
    public ManagedBeanBndType parse() throws ParseException {
        super.parseRootElement();
        return (ManagedBeanBndType) rootParsable;
    }

    @Override
    protected ManagedBeanBndType createRootParsable() throws ParseException {
        if ("managed-bean-bnd".equals(rootElementLocalName)) {
            return createXMLRootParsable();
        } else {
            throw new ParseException(invalidRootElement());
        }
    }

    private ManagedBeanBndType createXMLRootParsable() throws ParseException {
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
                return new ManagedBeanBndType(getDeploymentDescriptorPath());
            }
            if ("1.1".equals(versionString)) {
                version = 11;
                return new ManagedBeanBndType(getDeploymentDescriptorPath());
            }
            throw new ParseException(invalidDeploymentDescriptorVersion(versionString));
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(versionString));
    }

    @Override
    protected VersionData[] getVersionData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void validateRootElementName() throws ParseException {
        // TODO Auto-generated method stub
    }

    @Override
    protected ManagedBeanBndType createRootElement() {
        // TODO Auto-generated method stub
        return null;
    }
}
