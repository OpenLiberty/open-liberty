/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.appbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.commonbnd.JASPIRef;
import com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDConstants;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDVersion;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttributes;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIVersionAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;application-bnd>.
 */
@DDRootElement(name = "application-bnd",
               versions = {
                            @DDVersion(versionString = "1.0", version = 10, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.1", version = 11, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.2", version = 12, namespace = DDConstants.WEBSPHERE_EE_NS_URI)
               })
@DDIdAttribute
@DDXMIRootElement(name = "ApplicationBinding",
                  namespace = "applicationbnd.xmi",
                  version = 9,
                  primaryDDType = Application.class,
                  primaryDDVersions = { "1.2", "1.3", "1.4" },
                  refElementName = "application")
@DDXMIIgnoredAttributes(@DDXMIIgnoredAttribute(name = "appName", type = DDAttributeType.String))
public interface ApplicationBnd extends DeploymentDescriptor, RefBindingsGroup {

    static final String XML_BND_NAME = "META-INF/ibm-application-bnd.xml";
    static final String XMI_BND_NAME = "META-INF/ibm-application-bnd.xmi";

    /**
     * @return version="..." attribute value
     */
    @LibertyNotInUse
    @DDAttribute(name = "version", type = DDAttributeType.String)
    @DDXMIVersionAttribute
    String getVersion();

    /**
     * @return &lt;security-role> as a read-only list
     */
    @DDElement(name = "security-role")
    // XMI handled by custom class generator code
    List<SecurityRole> getSecurityRoles();

    /**
     * @return &lt;profile> as a read-only list
     */
    @LibertyNotInUse
    @DDElement(name = "profile")
    List<Profile> getProfiles();

    /**
     * @return &lt;jaspi-ref>, or null if unspecified
     */
    @LibertyNotInUse
    @DDElement(name = "jaspi-ref")
    JASPIRef getJASPIRef();

}
