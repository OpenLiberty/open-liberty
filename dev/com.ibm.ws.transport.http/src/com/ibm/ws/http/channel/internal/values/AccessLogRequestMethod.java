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

public class AccessLogRequestMethod extends AccessLogData {

	public AccessLogRequestMethod() {
		super("%m");
		// %m
		// The request method
	}

	@Override
	public boolean set(StringBuilder accessLogEntry,
			HttpResponseMessage response, HttpRequestMessage request, Object data) {
		String requestMethod = null;
		if(request != null){
			requestMethod = request.getMethod();
		}

		if(requestMethod != null){
			accessLogEntry.append(requestMethod);
		} else {
			accessLogEntry.append("-");
		}

		return true;
	}

}
