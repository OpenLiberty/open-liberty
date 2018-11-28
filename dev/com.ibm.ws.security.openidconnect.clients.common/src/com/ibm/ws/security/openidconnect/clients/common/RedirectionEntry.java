/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to communicate with the common RedirectionProcessor.
 */
public interface RedirectionEntry {

    ConvergedClientConfig getConvergedClientConfig(HttpServletRequest request, String clientId);

    void handleNoState(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
