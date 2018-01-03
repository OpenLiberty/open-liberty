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
package com.ibm.ws.jaxrs20;

public class JaxRsConstants {

    public static final String WEB_SERVICE_ANNOTATION_NAME = "javax.jws.WebService";

    public static final String CLASSlOADINGSERVICE_REFERENCE_NAME = "classLoadingService";
    public static final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";
    public static final String PROVIDERfACTORY_REFERENCE_NAME = "jaxRsProviderFactoryService";
    public static final String EXECUTOR_REFERENCE_NAME = "executorService";
    public static final String SCHEDULED_EXECUTOR_REFERENCE_NAME = "scheduledExecutorService";
    public static final String JAXRS_PROVIDER_REGISTER_REFERENCE_NAME = "jaxrsProviderRegister";

//     */
    public static final String WEB_ENDPOINT_PUBLISHER_TYPE = "WEB";
//
//    public static final String ENDPOINT_INFO_BUILDER = "ENDPOINT_INFO_BUILDER";
//
    public static final String ENDPOINT_INFO_BUILDER_CONTEXT = "ENDPOINT_INFO_BUILDER_CONTEXT";

    public static final String SERVLET_NAME_CLASS_PAIRS_FOR_EJBSINWAR = "servletNameClassPairsForEJBsInWAR";
//
//    /**
//     * The endPointInfo context environment entry keys
//     */
    public static final String ENV_ATTRIBUTE_ENDPOINT_BEAN_NAME = "endpointBeanNameInEnv";
//
    public static final String ENV_ATTRIBUTE_ENDPOINT_SERVLET_NAME = "endpointServletNameInEnv";

    public static final String ENDPOINT_LIST_BEANCUSTOMIZER = "ENDPOINT_LIST_BEANCUSTOMIZER";
    public static final String ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ = "ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ";
    public static final String COLLABORATOR = "collaborator";

    public static final String JAXRS_CONTAINER_FILTER_DISABLED_KEY = "com.ibm.ws.jaxrs.container.filter.disabled";
    public static final Boolean JAXRS_CONTAINER_FILTER_DISABLED = Boolean.valueOf(System.getProperty(JAXRS_CONTAINER_FILTER_DISABLED_KEY));

    public static final String JAXRS_APPLICATION_PARAM = "javax.ws.rs.Application";
    public static final String APPLICATION_ROOT_CLASS_NAME = "javax.ws.rs.core.Application";
    public static final String LIBERTY_JAXRS_SERVLET_CLASS_NAME = "com.ibm.websphere.jaxrs.server.IBMRestServlet";
    public static final String PROVIDER_CACHE_ALLOWED = "org.apache.cxf.jaxrs.provider.cache.allowed";
    public static final String PROVIDER_CACHE_CHECK_ALL = "org.apache.cxf.jaxrs.provider.cache.checkAllCandidates";
}
