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

public class AccessLogURLPath extends AccessLogData {

	public AccessLogURLPath() {
		super("%U");
		// %U
		// The URL path requested, not including any query string.
	}

	@Override
	public boolean set(StringBuilder accessLogEntry,
			HttpResponseMessage response, HttpRequestMessage request, Object data) {
		String urlPath = null;

		if(request != null){
			urlPath = request.getRequestURI();
		}

		if(urlPath != null){
			accessLogEntry.append(urlPath);
		} else {
			accessLogEntry.append("-");
		}

		return true;
	}

}
