/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public interface FileTransferRoutingHelper {

    String processSymbolicRoutingPath(String path, String targetHost, String targetServer, String targetUserDir, ServerPath symbolToResolve);

    void routedDeleteInternal(FileTransferHelper helper, RESTRequest request, String filePath, boolean recursiveDelete);

    void routedUploadInternal(FileTransferHelper helper, RESTRequest request, String filePath, boolean expansion, boolean legacyFileTransfer);

    void routedDownloadInternal(FileTransferHelper helper, RESTRequest request, RESTResponse response, String filePath, boolean legacyFileTransfer);

}