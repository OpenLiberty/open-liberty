/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class ServerUtils {
	private final static Class<?> c = ServerUtils.class;
	
	/**
	 * Stops the specified server with any expected error/warning strings.  If an exception is thrown
	 * while stopping the server, it is output in the test client logs and will return a null ProgramOutput.
	 */
	static ProgramOutput stopServer(LibertyServer server, String... expectedFailuresRegExps) {
		final String method = "stopServer";
		if (server == null) {
			Log.info(c, method, "server is null");
			return null;
		}
		if (!server.isStarted()) {
			Log.info(c, method, "server " + server.getServerName() + " is already stopped");
			return null;
		}
		
		ProgramOutput po;
		try {
			po = server.stopServer(expectedFailuresRegExps);
		} catch (Exception ex) {
			ex.printStackTrace();
			po = null;
		}
		return po;
	}
}
