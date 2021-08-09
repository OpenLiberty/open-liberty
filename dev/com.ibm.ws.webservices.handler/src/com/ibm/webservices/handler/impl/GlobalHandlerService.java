/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.webservices.handler.impl;

import java.util.List;

import com.ibm.wsspi.webservices.handler.Handler;

/**
 *
 */
public interface GlobalHandlerService {

    public List<Handler> getJAXWSServerSideInFlowGlobalHandlers();

    public List<Handler> getJAXWSServerSideOutFlowGlobalHandlers();

    public List<Handler> getJAXWSClientSideInFlowGlobalHandlers();

    public List<Handler> getJAXWSClientSideOutFlowGlobalHandlers();

    public List<Handler> getJAXRSServerSideInFlowGlobalHandlers();

    public List<Handler> getJAXRSServerSideOutFlowGlobalHandlers();

    public List<Handler> getJAXRSClientSideInFlowGlobalHandlers();

    public List<Handler> getJAXRSClientSideOutFlowGlobalHandlers();

    public boolean getSaajFlag();

}
