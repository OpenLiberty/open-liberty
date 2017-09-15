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

package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

public interface WebservicesBnd extends DeploymentDescriptor {
    static final String WEB_XML_BND_URI = "WEB-INF/ibm-ws-bnd.xml";

    static final String EJB_XML_BND_URI = "META-INF/ibm-ws-bnd.xml";

    public static String VERSION_ATTRIBUTE_NAME = "version";

    public static String WEBSERVICE_DESCRIPTION_ELEMENT_NAME = "webservice-description";

    public static String WEBSERVICE_ENDPOINT_PROPERTIES_ELEMENT_NAME = "webservice-endpoint-properties";

    public static String WEBSERVICE_ENDPOINT_ELEMENT_NAME = "webservice-endpoint";

    public static String HTTP_PUBLISHING_ELEMENT_NAME = "http-publishing";

    public static String SERVICE_REF_ELEMENT_NAME = "service-ref";

    /**
     * @return &lt;service-ref> as a list
     */
    public List<ServiceRef> getServiceRefs();

    /**
     * @param serviceRefName
     * @param componentName
     * @return ServiceRef object specified by the serviceRefName
     */
    public ServiceRef getServiceRef(String serviceRefName, String componentName);

    /**
     * @return &lt;http-publishing>, or null if unspecified
     */
    public HttpPublishing getHttpPublishing();

    /**
     * @return &lt;webservice-description> as a list
     */
    public List<WebserviceDescription> getWebserviceDescriptions();

    /**
     * @param webserviceDescriptionName
     * @return WebserviceDescription object specified by the serviceName
     */
    public WebserviceDescription getWebserviceDescription(String webserviceDescriptionName);

    /**
     * @return all attributes defined in the &lt;webservice-endpoint-properties> element
     */
    public Map<String, String> getWebserviceEndpointProperties();

    /**
     * @return &lt;webservice-endpoint> as a list
     */
    public List<WebserviceEndpoint> getWebserviceEndpoints();

    /**
     * @param portComponentName
     * @return WebserviceEndpoint object specified by the portComponentName
     */
    public WebserviceEndpoint getWebserviceEndpoint(String portComponentName);

}
