/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.version15.test.war.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class TestJSPApplication extends SpringBootServletInitializer {

	// Test that this never gets called
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		throw new RuntimeException("Should never call the SpringBootServletInitializer");
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TestJSPApplication.class, args);
	}

}
