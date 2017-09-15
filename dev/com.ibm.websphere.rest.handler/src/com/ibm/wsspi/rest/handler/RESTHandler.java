/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler;

import java.io.IOException;

/**
 * <p>This SPI allows other bundles to register themselves as listeners for a certain URL sub-root, which is
 * defined by specifying an OSGi property with key = RESTHandler.PROPERTY_REST_HANDLER_ROOT and value = URL.</p>
 * 
 * <p>The registered URL will be attached to the core REST Handler framework context root, which is /ibm/api. </p>
 * 
 * <p>As an example: if a bundle has an OSGi component implementing the RESTHandler interface and with a
 * RESTHandler.PROPERTY_REST_HANDLER_ROOT=/myCustomRoot, then that OSGi component will be called everytime
 * a new HTTPs request comes into:</p>
 * 
 * https://&lt;hostname&gt;:&lt;https_port&gt;/ibm/api/myCustomRoot<br>
 * 
 * <p>The feature that controls the REST Handler framework is "restHandler-1.0", but it is protected, therefore it must
 * be enabled by another feature (ie: by the feature that contains the bundle that implements RESTHandler interface).</p>
 * 
 * @ibm-spi
 */
public interface RESTHandler {

    /**
     * OSGi property used to define the sub-root that this rest handler listens to, starting with a slash.
     * 
     * <ul>
     * <li>Required property.
     * <li>Multiple instances of this property may be specified in the same handler.</li>
     * <li>Variables may be defined for any segment of these URLs, enclosed by {}.</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_ROOT = "com.ibm.wsspi.rest.handler.root";

    /**
     * OSGi property used to define the context root that this rest handler contributes to, starting with a slash.
     * 
     * <ul>
     * <li>Optional property.
     * <li>Multiple instances of this property may be specified in the same handler.</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_CONTEXT_ROOT = "com.ibm.wsspi.rest.handler.context.root";

    /**
     * The value of the default context root.
     */
    public static final String PROPERTY_REST_HANDLER_DEFAULT_CONTEXT_ROOT = "/ibm/api";

    /**
     * OSGi property used to specify whether or not this RESTHandler will implement its own authorization code.
     * 
     * <ul>
     * <li>Optional property.</li>
     * <li>Possible values are "true" or "false" and default is "false".</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_CUSTOM_SECURITY = "com.ibm.wsspi.rest.handler.custom.security";

    /**
     * OSGi property used to specify whether or not this RESTHandler will implement its own routing code.
     * 
     * <ul>
     * <li>Optional property.</li>
     * <li>Possible values are "true" or "false" and default is "false".</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_CUSTOM_ROUTING = "com.ibm.wsspi.rest.handler.custom.routing";

    /**
     * OSGi property used to specify whether or not this RESTHandler will implement its own Cross Origin Resource Sharing code.
     * 
     * <ul>
     * <li>Optional property.</li>
     * <li>Possible values are "true" or "false" and default is "false".</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_CUSTOM_CORS = "com.ibm.wsspi.rest.handler.custom.cors";

    /**
     * OSGi property used to specify whether or not this RESTHandler should be hidden from the top-level ibm/api query of available APIs.
     * 
     * <ul>
     * <li>Optional property.</li>
     * <li>Possible values are "true" or "false" and default is "false".</li>
     * </ul>
     */
    public static final String PROPERTY_REST_HANDLER_HIDDEN_API = "com.ibm.wsspi.rest.handler.hidden.api";

    /**
     * This method gets called for every URL request that came in through the RESTHandlerContainer and matched the registered paths of this handler.
     * 
     * @param request encapsulates the artifacts for the HTTP request
     * @param response encapsulates the artifacts for the HTTP response
     * @throws IOException if an I/O exception occurred.
     */
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException;

}
