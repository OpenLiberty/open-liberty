/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

public interface WebservicesBnd extends DeploymentDescriptor {
    String WEB_XML_BND_URI = "WEB-INF/ibm-ws-bnd.xml";
    String EJB_XML_BND_URI = "META-INF/ibm-ws-bnd.xml";

    String VERSION_ATTRIBUTE_NAME = "version";

    String WEBSERVICE_DESCRIPTION_ELEMENT_NAME = "webservice-description";
    String WEBSERVICE_ENDPOINT_PROPERTIES_ELEMENT_NAME = "webservice-endpoint-properties";
    String WEBSERVICE_ENDPOINT_ELEMENT_NAME = "webservice-endpoint";
    String HTTP_PUBLISHING_ELEMENT_NAME = "http-publishing";
    String SERVICE_REF_ELEMENT_NAME = "service-ref";

    List<ServiceRef> getServiceRefs();
    ServiceRef getServiceRef(String serviceRefName, String componentName);

    HttpPublishing getHttpPublishing();

    List<WebserviceDescription> getWebserviceDescriptions();
    WebserviceDescription getWebserviceDescription(String webserviceDescriptionName);

    Map<String, String> getWebserviceEndpointProperties();

    List<WebserviceEndpoint> getWebserviceEndpoints();
    WebserviceEndpoint getWebserviceEndpoint(String portComponentName);
}
