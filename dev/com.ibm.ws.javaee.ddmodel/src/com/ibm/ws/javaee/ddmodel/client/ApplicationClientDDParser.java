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
package com.ibm.ws.javaee.ddmodel.client;

import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationClientDDParser extends DDParser {
    private final int maxVersion;

    public ApplicationClientDDParser(Container ddRootContainer, Entry ddEntry, int maxVersion) throws ParseException {
        super(ddRootContainer, ddEntry);
        this.maxVersion = maxVersion;
    }

    ApplicationClient parse() throws ParseException {
        super.parseRootElement();
        return (ApplicationClient) rootParsable;
    }

    private static final String APPCLIENT_DTD_PUBLIC_ID_12 = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN";
    private static final String APPCLIENT_DTD_PUBLIC_ID_13 = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"application-client".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");
        if (vers == null) {
            if (namespace == null && dtdPublicId != null) {
                if (APPCLIENT_DTD_PUBLIC_ID_12.equals(dtdPublicId)) {
                    version = ApplicationClient.VERSION_1_2;
                    eePlatformVersion = 12;
                    return new ApplicationClientType(getDeploymentDescriptorPath());
                }
                if (APPCLIENT_DTD_PUBLIC_ID_13.equals(dtdPublicId)) {
                    version = ApplicationClient.VERSION_1_3;
                    eePlatformVersion = 13;
                    return new ApplicationClientType(getDeploymentDescriptorPath());
                }
            }
            throw new ParseException(unknownDeploymentDescriptorVersion());
        }
        if ("1.4".equals(vers)) {
            if ("http://java.sun.com/xml/ns/j2ee".equals(namespace)) {
                version = ApplicationClient.VERSION_1_4;
                eePlatformVersion = 14;
                return new ApplicationClientType(getDeploymentDescriptorPath());
            }
        }
        else if ("5".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = ApplicationClient.VERSION_5;
                eePlatformVersion = 50;
                return new ApplicationClientType(getDeploymentDescriptorPath());
            }
        }
        else if ("6".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = ApplicationClient.VERSION_6;
                eePlatformVersion = 60;
                return new ApplicationClientType(getDeploymentDescriptorPath());
            }
        }
        else if ("7".equals(vers)) {
        	if (maxVersion >= ApplicationClient.VERSION_7) { 
        		if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
        			version = ApplicationClient.VERSION_7;
        			eePlatformVersion = 70;
        			return new ApplicationClientType(getDeploymentDescriptorPath());
        		}
        	}
        }
        else if ("8".equals(vers)) {
        	if (maxVersion >= ApplicationClient.VERSION_8) {
        		if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
        			version = ApplicationClient.VERSION_8;
        			eePlatformVersion = 80;
        			return new ApplicationClientType(getDeploymentDescriptorPath());
        		}
        	}
        }
        else if ("9".equals(vers)) {
            if (maxVersion >= ApplicationClient.VERSION_9) {
                if ("https://jakarta.ee/xml/ns/jakartaee".equals(namespace)) {
                    version = ApplicationClient.VERSION_9;
                    eePlatformVersion = 90;
                    return new ApplicationClientType(getDeploymentDescriptorPath());
                }
            }
        }

        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }
}
