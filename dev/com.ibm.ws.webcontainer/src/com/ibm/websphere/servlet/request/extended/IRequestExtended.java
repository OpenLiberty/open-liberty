/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.request.extended;

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
}
