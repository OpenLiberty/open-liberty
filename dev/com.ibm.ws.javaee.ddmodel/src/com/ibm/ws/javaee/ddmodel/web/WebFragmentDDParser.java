/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.web.common.WebFragmentType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

/**
 *
 */
public class WebFragmentDDParser extends DDParser {

    private final int maxVersion;

    public WebFragmentDDParser(Container ddRootContainer, Entry ddEntry, int version) throws ParseException {
        super(ddRootContainer, ddEntry);
        trimSimpleContentAsRequiredByServletSpec = true;
        this.maxVersion = version;
    }

    WebFragment parse() throws ParseException {
        super.parseRootElement();
        return (WebFragment) rootParsable;
    }

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"web-fragment".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");
        if (vers == null) {
            throw new ParseException(missingDeploymentDescriptorVersion());
        }

        if (maxVersion == 50)
            runtimeVersion = 90;
        else if (maxVersion == 40)
            runtimeVersion = 80;
        else if (maxVersion == 31)
            runtimeVersion = 70;
        else
            runtimeVersion = 60; //Servlet-3.0 is the earliest Liberty runtime spec.

        if ("3.0".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_3_0;
                eePlatformVersion = 60;
                return new WebFragmentType(getDeploymentDescriptorPath());
            }
        } else if (maxVersion >= 31 && "3.1".equals(vers)) {
            if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_3_1;
                eePlatformVersion = 70;
                return new WebFragmentType(getDeploymentDescriptorPath());
            }
        } else if ((maxVersion >= 40) && "4.0".equals(vers)) {
            if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_4_0;
                eePlatformVersion = 80;
                return new WebFragmentType(getDeploymentDescriptorPath());
            }
        } else if ((maxVersion >= 50) && "5.0".equals(vers)) {
            if ("https://jakarta.ee/xml/ns/jakartaee".equals(namespace)) {
                version = WebApp.VERSION_5_0;
                eePlatformVersion = 80;
                return new WebFragmentType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }
}
