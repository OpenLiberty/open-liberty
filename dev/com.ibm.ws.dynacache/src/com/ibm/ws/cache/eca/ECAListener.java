/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.eca;

import java.net.ServerSocket;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


// Created by MD18759

public class ECAListener {
	private static TraceComponent tc = Tr.register(ECAListener.class,
			"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

		private static boolean once = false;
	private static boolean accepting = true;

	ServerSocket serverSock;

	public ECAListener(int s) {

		if (tc.isEntryEnabled())
			Tr.entry(tc, "socket : " + s);

		try {
			serverSock = new ServerSocket(s);
		} catch (java.io.IOException e) {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "IOException " + e);
		}

		if (tc.isEntryEnabled())
			Tr.exit(tc, "constructor");
	}

	public ECAConnection accept() {
		if (tc.isEntryEnabled())
			Tr.entry(tc, "accept");

		try {
			
			if (tc.isDebugEnabled())
				Tr.debug(tc, "accept server socket");
			return new ECAConnection(serverSock.accept());
			
		} catch (java.io.IOException e) {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "accept IOException");
		} finally {
			if (tc.isEntryEnabled())
				Tr.exit(tc, "accept");
		}

		return null;
	}

	public static boolean isAccepting() {
		if (tc.isEntryEnabled())
			Tr.entry(tc, "isAccepting");
		if (accepting) {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "accepting");

			if (!once)
				once = true;
			else
				accepting = false;
		}

		if (tc.isEntryEnabled())
			Tr.exit(tc, "isAccepting " + accepting);

		return accepting;
	}
}
