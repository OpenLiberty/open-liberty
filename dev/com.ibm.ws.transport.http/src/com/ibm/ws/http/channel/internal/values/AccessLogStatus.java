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

public class AccessLogStatus extends AccessLogData {

	public AccessLogStatus() {
		super("%s");
		// %s
		// Status Code
	}

	@Override
	public boolean set(StringBuilder accessLogEntry,
			HttpResponseMessage response, HttpRequestMessage request, Object data) {
		int statusCode = 0;
		if(response != null){
			statusCode = response.getStatusCodeAsInt();
		}

		if(statusCode != 0){
			accessLogEntry.append(statusCode);
		} else {
			accessLogEntry.append("-");
		}
		return true;
	}
}
