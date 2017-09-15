/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejbext;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDConstants;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDVersion;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIVersionAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyModule;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;ejb-jar-ext>.
 */
@DDRootElement(name = "ejb-jar-ext",
               versions = {
                            @DDVersion(versionString = "1.0", version = 10, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.1", version = 11, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
               })
@DDIdAttribute
@DDXMIRootElement(name = "EJBJarExtension",
                  namespace = "ejbext.xmi",
                  version = 9,
                  primaryDDType = EJBJar.class,
                  primaryDDVersions = { "1.1", "2.0", "2.1" },
                  refElementName = "ejbJar")
@LibertyModule
public interface EJBJarExt extends DeploymentDescriptor {

    /**
     * @return &lt;session> and &lt;message-driven>, or an empty list if unspecified
     */
    @DDChoiceElements({
                        @DDChoiceElement(name = "session", type = Session.class),
                        @DDChoiceElement(name = "message-driven", type = MessageDriven.class),
    })
    @DDXMIElement(name = "ejbExtensions", types = { Session.class, MessageDriven.class })
    List<EnterpriseBean> getEnterpriseBeans();

    /**
     * @return &lt;version>="..." attribute value is required
     */
    @LibertyNotInUse
    @DDAttribute(name = "version", type = DDAttributeType.String)
    @DDXMIVersionAttribute
    String getVersion();

}
