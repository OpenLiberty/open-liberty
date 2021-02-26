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
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.web.common.WebAppType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

/**
 *
 */
public class WebAppDDParser extends DDParser {
    private final int maxVersion;

    public WebAppDDParser(Container ddRootContainer, Entry ddEntry, int version) throws ParseException {
        super(ddRootContainer, ddEntry);
        trimSimpleContentAsRequiredByServletSpec = true;
        this.maxVersion = version;
    }

    WebApp parse() throws ParseException {
        super.parseRootElement();
        return (WebApp) rootParsable;
    }

    private static final String WEBAPP_DTD_PUBLIC_ID_22 = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    private static final String WEBAPP_DTD_PUBLIC_ID_23 = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"web-app".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");
        if (vers == null) {
            if (namespace == null && dtdPublicId != null) {
                if (WEBAPP_DTD_PUBLIC_ID_22.equals(dtdPublicId)) {
                    version = 22;
                    eePlatformVersion = 12;
                    return new WebAppType(getDeploymentDescriptorPath());
                }
                if (WEBAPP_DTD_PUBLIC_ID_23.equals(dtdPublicId)) {
                    version = 23;
                    eePlatformVersion = 13;
                    return new WebAppType(getDeploymentDescriptorPath());
                }
            }
            throw new ParseException(unknownDeploymentDescriptorVersion());
        }

        if (maxVersion == 50)
            runtimeVersion = 90;
        else if (maxVersion == 40)
            runtimeVersion = 80;
        else if (maxVersion == 31)
            runtimeVersion = 70;
        else
            runtimeVersion = 60; //Servlet-3.0 is the earliest Liberty runtime spec.

        if ("2.4".equals(vers)) {
            if ("http://java.sun.com/xml/ns/j2ee".equals(namespace)) {
                version = 24;
                eePlatformVersion = 14;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        } else if ("2.5".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = 25;
                eePlatformVersion = 50;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        } else if ("3.0".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_3_0;
                eePlatformVersion = 60;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        } else if ((maxVersion >= 31) && "3.1".equals(vers)) {
            if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_3_1;
                eePlatformVersion = 70;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        } else if ((maxVersion >= 40) && "4.0".equals(vers)) {
            if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                version = WebApp.VERSION_4_0;
                eePlatformVersion = 80;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        } else if ((maxVersion >= 50) && "5.0".equals(vers)) {
            if ("https://jakarta.ee/xml/ns/jakartaee".equals(namespace)) {
                version = WebApp.VERSION_5_0;
                eePlatformVersion = 90;
                return new WebAppType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }

}
