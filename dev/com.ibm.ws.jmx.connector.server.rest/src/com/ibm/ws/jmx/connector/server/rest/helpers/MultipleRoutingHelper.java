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

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 *
 */
public interface MultipleRoutingHelper {

    String multipleDeleteInternal(RESTRequest request, String targetPath, boolean recursive) throws IOException;

    String multipleUploadInternal(RESTRequest request, String targetPath, boolean expand, boolean local) throws IOException;

    String getTaskProperty(String taskID, String property);

    String getTaskProperties(String taskID);

    String getAllStatus(Set<Entry<String, List<String>>> filter);

    String getStatus(String taskID);

    String getHosts(String taskID);

    String getHostDetails(String taskID, String host);

}
