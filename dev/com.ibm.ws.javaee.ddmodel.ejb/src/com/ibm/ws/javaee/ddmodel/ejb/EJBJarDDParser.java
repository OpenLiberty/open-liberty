/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class EJBJarDDParser extends DDParser {
    private final int maxVersion;

    public EJBJarDDParser(Container ddRootContainer, Entry ddEntry, int maxVersion) throws ParseException {
        super(ddRootContainer, ddEntry);
        this.maxVersion = maxVersion;
    }

    EJBJar parse() throws ParseException {
        super.parseRootElement();
        return (EJBJar) rootParsable;
    }

    private static final String EJBJAR_DTD_PUBLIC_ID_11 = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    private static final String EJBJAR_DTD_PUBLIC_ID_20 = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"ejb-jar".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");
        if (vers == null) {
            if (namespace == null && dtdPublicId != null) {
                if (EJBJAR_DTD_PUBLIC_ID_11.equals(dtdPublicId)) {
                    version = EJBJar.VERSION_1_1;
                    eePlatformVersion = 12;
                    return new EJBJarType(getDeploymentDescriptorPath());
                }
                if (EJBJAR_DTD_PUBLIC_ID_20.equals(dtdPublicId)) {
                    version = EJBJar.VERSION_2_0;
                    eePlatformVersion = 13;
                    return new EJBJarType(getDeploymentDescriptorPath());
                }
            }
            throw new ParseException(unknownDeploymentDescriptorVersion());
        }
        if ("2.1".equals(vers)) {
            if ("http://java.sun.com/xml/ns/j2ee".equals(namespace)) {
                version = EJBJar.VERSION_2_1;
                eePlatformVersion = 14;
                return new EJBJarType(getDeploymentDescriptorPath());
            }
        }
        else if ("3.0".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = EJBJar.VERSION_3_0;
                eePlatformVersion = 50;
                return new EJBJarType(getDeploymentDescriptorPath());
            }
        }
        else if ("3.1".equals(vers)) {
            if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                version = EJBJar.VERSION_3_1;
                eePlatformVersion = 60;
                return new EJBJarType(getDeploymentDescriptorPath());
            }
        }
        else if (maxVersion >= EJBJar.VERSION_3_2 && "3.2".equals(vers)) {
            if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                version = EJBJar.VERSION_3_2;
                eePlatformVersion = 70;
                return new EJBJarType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }
}
