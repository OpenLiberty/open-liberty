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

import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogQueryString extends AccessLogData {

	public AccessLogQueryString() {
		super("%q");
		// %q
		// The query string (prepended with a ?)
	}

	@Override
	public boolean set(StringBuilder accessLogEntry,
			HttpResponseMessage response, HttpRequestMessage request, Object data) {

		String queryString = request.getQueryString();

		if(queryString != null){
			accessLogEntry.append("?");
			accessLogEntry.append(GenericUtils.nullOutPasswords(queryString, (byte)'&'));
		} else {
			accessLogEntry.append("-");
		}
		return true;
	}

}
