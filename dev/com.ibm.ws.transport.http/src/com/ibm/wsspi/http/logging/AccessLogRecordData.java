/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.logging;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public interface AccessLogRecordData {

    HttpRequestMessage getRequest();

    HttpResponseMessage getResponse();

    long getTimestamp();

    String getVersion();

    String getUserId();

    String getRemoteAddress();

    long getBytesWritten();

    long getStartTime();

    long getElapsedTime();

    String getLocalIP();

    String getLocalPort();

    String getRemotePort();
}
