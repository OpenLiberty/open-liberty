/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

public class AccessLogRemoteUser extends AccessLogData {

	public AccessLogRemoteUser() {
		super("%u");
	}

	@Override
	public boolean set(StringBuilder accessLogEntry,
			HttpResponseMessage response, HttpRequestMessage request, Object data) {

		String remoteUser = null;
		if(request != null){
			remoteUser = request.getHeader(HttpHeaderKeys.HDR_$WSRU).asString();
		}

		if(remoteUser != null){
			accessLogEntry.append(remoteUser);
		} else {
			accessLogEntry.append("-");
		}

		return true;
	}
}
