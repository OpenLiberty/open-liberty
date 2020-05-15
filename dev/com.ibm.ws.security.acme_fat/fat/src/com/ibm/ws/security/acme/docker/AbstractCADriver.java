/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.docker;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class that represents a ACME CA server driver.
 */
public abstract class AbstractCADriver {

	protected CAContainer caContainer;

	static {
		// TODO Should support remote docker via Consul?
		System.setProperty("global.consulServerList", "");
	}

	/**
	 * Print a banner with connection related information.
	 */
	protected void printBanner() {

		Set<String> uris = new HashSet<String>();
		uris.add(caContainer.getAcmeDirectoryURI(false));
		uris.add(caContainer.getAcmeDirectoryURI(true));

		StringBuffer banner = new StringBuffer();
		banner.append("\n\n\n");
		banner.append("***********************************************************************\n");
		banner.append("*\n");
		banner.append("*\n");
		banner.append("* ACME CA Server Directory URI(s):\n");
		for (String uri : uris) {
			banner.append("*      " + uri + "\n");
		}
		banner.append("* HTTP port: " + caContainer.getHttpPort() + "\n");
		banner.append("*\n");
		banner.append("*\n");
		banner.append("***********************************************************************\n");
		banner.append("\n\n\n");
		banner.append("Use 'ctrl-c' to terminate execution...");

		System.out.println(banner.toString());
		System.out.flush();
	}

	/**
	 * Start the ACME CA server environment.
	 */
	protected abstract void initialize() throws Exception;

	/**
	 * Stop the ACME CA server environment.
	 */
	protected void stop() {
		System.out.println("Stopping ACME CA Server environment...");
		if (caContainer != null) {
			caContainer.stop();
		}
	}
}
