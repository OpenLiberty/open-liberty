/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.javaee.dd.ejbbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestination;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDChoiceElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDConstants;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDVersion;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttributes;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredElements;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIVersionAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyModule;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;ejb-jar-bnd>.
 */
@DDRootElement(name = "ejb-jar-bnd",
               versions = {
                            @DDVersion(versionString = "1.0", version = 10, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.1", version = 11, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.2", version = 12, namespace = DDConstants.WEBSPHERE_EE_NS_URI)
               })
@DDIdAttribute
@DDXMIRootElement(name = "EJBJarBinding",
                  namespace = "ejbbnd.xmi",
                  version = 9,
                  primaryDDType = EJBJar.class,
                  primaryDDVersions = { "1.1", "2.0", "2.1" },
                  refElementName = "ejbJar")
@DDXMIIgnoredAttributes(@DDXMIIgnoredAttribute(name = "currentBackendId", type = DDAttributeType.String))
@DDXMIIgnoredElements({ @DDXMIIgnoredElement(name = "defaultCMPConnectionFactory"), @DDXMIIgnoredElement(name = "defaultDatasource") })
@LibertyModule
public interface EJBJarBnd extends DeploymentDescriptor {

    /**
     * @return version="..." attribute value, required.
     */
    @LibertyNotInUse
    @DDAttribute(name = "version", type = DDAttributeType.String)
    @DDXMIVersionAttribute
    String getVersion();

    /**
     * @return &lt;session> and &lt;message-driven>, or empty list if unspecified
     */
    @DDChoiceElements({
                        @DDChoiceElement(name = "session", type = Session.class),
                        @DDChoiceElement(name = "message-driven", type = MessageDriven.class)
    })
    @DDXMIElement(name = "ejbBindings",
                  defaultType = Session.class,
                  types = MessageDriven.class)
    List<EnterpriseBean> getEnterpriseBeans();

    /**
     * @return &lt;interceptor>, or empty list if unspecified
     */
    @DDElement(name = "interceptor")
    // No XMI metadata: this element was not supported prior to EE 5.
    List<Interceptor> getInterceptors();

    /**
     * @return &lt;message-destination>, or empty list if unspecified
     */
    @DDElement(name = "message-destination")
    List<MessageDestination> getMessageDestinations();

}
