/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.permissions;

import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

final class PermissionsConfigDDParser extends DDParser {

    public PermissionsConfigDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
        super(ddRootContainer, ddEntry);
    }

    PermissionsConfig parse() throws ParseException {
        super.parseRootElement();
        return (PermissionsConfig) rootParsable;
    }

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"permissions".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");

        if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
            if ("7".equals(vers)) {
                /*
                 * The 7 permissions.xml schema version is used for JavaEE 7 and 8.
                 */
                version = 70;
                return new PermissionsConfigType(getDeploymentDescriptorPath());
            }
        } else if ("https://jakarta.ee/xml/ns/jakartaee".equals(namespace)) {
            if ("9".equals(vers)) {
                version = 90;
                return new PermissionsConfigType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }
}
