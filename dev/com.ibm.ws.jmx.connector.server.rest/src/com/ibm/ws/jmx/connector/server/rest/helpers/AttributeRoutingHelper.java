/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.util.List;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public interface AttributeRoutingHelper {

    void getAttributes(RESTRequest request, RESTResponse response, String objectName, List<String> queryAttributes, boolean isLegacy);

    void getAttribute(RESTRequest request, RESTResponse response, String objectName, String attributeName, boolean isLegacy);

    void setAttributes(RESTRequest request, RESTResponse response, boolean isLegacy);

    void setAttribute(RESTRequest request, RESTResponse response, boolean isLegacy);

    void deleteAttributes(RESTRequest request, RESTResponse response, boolean isLegacy);

    void deleteAttribute(RESTRequest request, RESTResponse response, boolean isLegacy);

}
