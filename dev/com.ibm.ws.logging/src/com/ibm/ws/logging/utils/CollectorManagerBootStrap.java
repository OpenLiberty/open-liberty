/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.utils;

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.wsspi.collector.manager.Handler;

/**
 *
 */
public interface CollectorManagerBootStrap {

    public LogSource getLogSource();

    public TraceSource getTraceSource();

    public BufferManagerImpl getLogConduit();

    public BufferManagerImpl getTraceConduit();

    public Handler getLogHandler();

    public void setHandler(Handler handler);
}
