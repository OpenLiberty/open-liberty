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
package com.ibm.ws.jaxws;

import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

public class JaxWsConstants {

    public static final String WEB_SERVICE_ANNOTATION_NAME = "javax.jws.WebService";

    public static final String WEB_SERVICE_PROVIDER_ANNOTATION_NAME = "javax.xml.ws.WebServiceProvider";

    public static final String DEFAULT_SERVER_CATALOG_WEB_LOCATION = "WEB-INF/jax-ws-catalog.xml";

    public static final String DEFAULT_SERVER_CATALOG_EJB_LOCATION = "META-INF/jax-ws-catalog.xml";

    public static final String DEFAULT_CLIENT_CATALOG_LOCATION = "META-INF/jax-ws-catalog.xml";

    public static final String HANDLER_CHAIN_ANNOTATION_NAME = "javax.jws.HandlerChain";

    public static final String MTOM_ANNOTATION_NAME = "javax.xml.ws.soap.MTOM";

    public static final String ADDRESSING_ANNOTATION_NAME = "javax.xml.ws.soap.Addressing";

    public static final String RESPECT_BINDING_ANNOTATION_NAME = "javax.xml.ws.RespectBinding";

    public static final String BINDING_TYPE_ANNOTATION_NAME = "javax.xml.ws.BindingType";

    public static final String SERVICE_MODE_ANNOTATION_NAME = "javax.xml.ws.ServiceMode";

    public static final String SOAP11_HTTP_TOKEN = "##SOAP11_HTTP";

    public static final String SOAP12_HTTP_TOKEN = "##SOAP12_HTTP";

    public static final String SOAP11_HTTP_MTOM_TOKEN = "##SOAP11_HTTP_MTOM";

    public static final String SOAP12_HTTP_MTOM_TOKEN = "##SOAP12_HTTP_MTOM";

    public static final String XML_HTTP_TOKEN = "##XML_HTTP";

    public static final String SOAP11HTTP_BINDING = SOAPBinding.SOAP11HTTP_BINDING;

    public static final String SOAP11HTTP_MTOM_BINDING = SOAPBinding.SOAP11HTTP_MTOM_BINDING;

    public static final String SOAP12HTTP_BINDING = SOAPBinding.SOAP12HTTP_BINDING;

    public static final String SOAP12HTTP_MTOM_BINDING = SOAPBinding.SOAP12HTTP_MTOM_BINDING;

    public static final String HTTP_BINDING = HTTPBinding.HTTP_BINDING;

    public static final String CXF_XML_FORMAT_BINDING = "http://cxf.apache.org/bindings/xformat";
    /**
     * Identify the web endpoint publisher type
     */
    public static final String WEB_ENDPOINT_PUBLISHER_TYPE = "WEB";

    public static final String ENDPOINT_INFO_BUILDER = "ENDPOINT_INFO_BUILDER";

    public static final String ENDPOINT_INFO_BUILDER_CONTEXT = "ENDPOINT_INFO_BUILDER_CONTEXT";

    public static final String UNKNOWN_NAMESPACE = "http://unknown.namespace/";

    /**
     * Pre-defined phases for EndpointInfoConfigurator
     */
    public static final String PROCESS_ANNOATION_PHASE = "processAnnotaion";

    public static final String PROCESS_DESCRIPTOR_PHASE = "processDescriptor";

    /**
     * The key for the map of servletName and servletClass of EJB in WAR configured in web.xml
     */
    public static final String SERVLET_NAME_CLASS_PAIRS_FOR_EJBSINWAR = "servletNameClassPairsForEJBsInWAR";

    /**
     * The endPointInfo context environment entry keys
     */
    public static final String ENV_ATTRIBUTE_ENDPOINT_BEAN_NAME = "endpointBeanNameInEnv";

    public static final String ENV_ATTRIBUTE_ENDPOINT_SERVLET_NAME = "endpointServletNameInEnv";

    /**
     * The service factory pids
     */
    public static final String HTTP_CONDUITS_SERVICE_FACTORY_PID = "org.apache.cxf.http.conduits";

    /**
     * The prefix of CXF properties
     */
    public static final String HTTP_CONDUIT_PREFIX = "http.conduit.";

    /**
     * annotation attribute constant values
     */
    public static final String ENDPOINTINTERFACE_ATTRIBUTE = "endpointInterface";

    public static final String TARGETNAMESPACE_ATTRIBUTE = "targetNamespace";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String SERVICENAME_ATTRIBUTE = "serviceName";

    public static final String SERVICENAME_ATTRIBUTE_SUFFIX = "Service";

    public static final String PORTNAME_ATTRIBUTE = "portName";

    public static final String PORTNAME_ATTRIBUTE_SUFFIX = "Port";

    public static final String WSDLLOCATION_ATTRIBUTE = "wsdlLocation";

    public static final String ENABLE_lOGGINGINOUTINTERCEPTOR = "enableLoggingInOutInterceptor";
}
