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

package com.ibm.ws.security.acme.docker.boulder;

import com.ibm.ws.security.acme.docker.AbstractCADriver;

/**
 * Driver to start a Boulder ACME CA Server environment from within a
 * stand-alone JVM.
 */
public class BoulderDriver extends AbstractCADriver {

	public static void main(String[] args) throws Exception {
		System.out.println("\n\n");

		BoulderDriver driver = new BoulderDriver();

		/*
		 * Start the Boulder environment.
		 */
		driver.initialize();

		/*
		 * Sometimes the trace flushes late from the BoulderContainer
		 */
		Thread.sleep(500);
		/*
		 * Wait until the process has been cancelled via ctrl-c.
		 */
		driver.printBanner(true);
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
		 * Startup the Boulder server.
		 */
		System.out.println("Starting Boulder ACME CA server environment...");
		caContainer = new BoulderContainer();
	}
}
