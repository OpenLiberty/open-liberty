/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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

@DDRootElement(name = "application-bnd",
               versions = { @DDVersion(versionString = "1.0", version = 10, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
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
    String XML_BND_NAME = "META-INF/ibm-application-bnd.xml";
    String XMI_BND_NAME = "META-INF/ibm-application-bnd.xmi";

    int VERSION_1_2 = 12;
    int VERSION_1_3 = 13;
    int VERSION_1_4 = 14;

    int[] VERSIONS = { VERSION_1_2, VERSION_1_3, VERSION_1_4 };

    String VERSION_1_2_STR = "1.2";
    String VERSION_1_3_STR = "1.3";
    String VERSION_1_4_STR = "1.4";

    String [] VERSION_STRS = { VERSION_1_2_STR, VERSION_1_3_STR, VERSION_1_4_STR };

    @LibertyNotInUse
    @DDAttribute(name = "version", type = DDAttributeType.String)
    @DDXMIVersionAttribute
    String getVersion();

    @DDElement(name = "security-role")
    // XMI handled by custom class generator code
    List<SecurityRole> getSecurityRoles();

    @LibertyNotInUse
    @DDElement(name = "profile")
    List<Profile> getProfiles();

    @LibertyNotInUse
    @DDElement(name = "jaspi-ref")
    JASPIRef getJASPIRef();
}
