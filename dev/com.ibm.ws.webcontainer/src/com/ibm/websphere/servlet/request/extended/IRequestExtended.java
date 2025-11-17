/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.websphere.servlet.request.extended;

import java.net.Socket;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.util.ThreadPool;
import com.ibm.wsspi.http.HttpInboundConnection;

/**
 *  RTC 160610. Contains methods moved from com.ibm.websphere.servlet.request.IRequest 
 *  which should not be spi.
 */
public interface IRequestExtended extends IRequest {

    public ThreadPool getThreadPool();
    
    public HttpInboundConnection getHttpInboundConnection();
    
    public Socket getRequestSocket();
}
