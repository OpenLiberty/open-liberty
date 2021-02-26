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
package com.ibm.ws.javaee.ddmodel.jsf;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

final class FacesConfigDDParser extends DDParser {

    public FacesConfigDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
        super(ddRootContainer, ddEntry);
        this.FacesBundleLoadedVersion = 20;
    }

    /**
     * @param containerToAdapt
     * @param ddEntry
     * @param version
     * @throws ParseException
     */
    public FacesConfigDDParser(Container ddRootContainer, Entry ddEntry, int version) throws ParseException {
        super(ddRootContainer, ddEntry);
        this.FacesBundleLoadedVersion = version;
    }

    FacesConfig parse() throws ParseException {
        super.parseRootElement();
        return (FacesConfig) rootParsable;
    }

    private static final String FACES_CONFIG_DTD_PUBLIC_ID_10 = "-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.0//EN";
    private static final String FACES_CONFIG_DTD_PUBLIC_ID_11 = "-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.1//EN";
    private final int FacesBundleLoadedVersion;

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"faces-config".equals(rootElementLocalName)) {
            throw new ParseException(invalidRootElement());
        }
        String vers = getAttributeValue("", "version");
        if (vers == null) {
            if (namespace == null && dtdPublicId != null) {
                if (FACES_CONFIG_DTD_PUBLIC_ID_10.equals(dtdPublicId)) {
                    version = 10;
                    return new FacesConfigType(getDeploymentDescriptorPath());
                }
                if (FACES_CONFIG_DTD_PUBLIC_ID_11.equals(dtdPublicId)) {
                    version = 11;
                    return new FacesConfigType(getDeploymentDescriptorPath());
                }
            }
            throw new ParseException(unknownDeploymentDescriptorVersion());
        }
        if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
            if ("1.2".equals(vers)) {
                // javaee 5 only
                version = 12;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
            if ("2.0".equals(vers)) {
                // javaee 6 only
                version = 20;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
            if ("2.1".equals(vers)) {
                // javaee 6 only
                version = 21;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
        } else if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
            //  Don't allow a faces-config.xml with version="2.2" if the jsf-2.2 feature is not enabled.
            if ((this.FacesBundleLoadedVersion >= 22) && "2.2".equals(vers)) {
                // javaee 7 only
                version = 22;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
            //  Don't allow a faces-config.xml with version="2.3" if the jsf-2.3 feature is not enabled.
            if ((this.FacesBundleLoadedVersion >= 23) && "2.3".equals(vers)) {
                // javaee 8 only
                version = 23;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
        } else if ("https://jakarta.ee/xml/ns/jakartaee".contentEquals(namespace)) {
            // Don't allow a faces-config.xml with a version="3.0" if the faces-3.0 feature is not enabled.
            if ((this.FacesBundleLoadedVersion >= 30) && "3.0".contentEquals(vers)) {
                // Jakarta 9 only
                version = 30;
                return new FacesConfigType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }

    /**
     * @return the facesBundleLoadedVersion
     */
    public int getFacesBundleLoadedVersion() {
        return FacesBundleLoadedVersion;
    }
}