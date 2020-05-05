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

package com.ibm.ws.security.acme.docker.pebble;

import com.ibm.ws.security.acme.docker.AbstractCADriver;

/**
 * Driver to start a Pebble ACME CA Server environment from within a stand-alone
 * JVM.
 */
public class PebbleDriver extends AbstractCADriver {

	public static void main(String[] args) throws Exception {
		System.out.println("\n\n");

		PebbleDriver driver = new PebbleDriver();

		/*
		 * Start the Pebble environment.
		 */
		driver.initialize();

		/*
		 * Wait until the process has been cancelled via ctrl-c.
		 */
		driver.printBanner();
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		driver.stop();
	}

	@Override
	protected void initialize() {
		/*
		 * Startup the pebble server.
		 */
		System.out.println("Starting Pebble ACME CA server environment...");
		caContainer = new PebbleContainer();
	}
}
