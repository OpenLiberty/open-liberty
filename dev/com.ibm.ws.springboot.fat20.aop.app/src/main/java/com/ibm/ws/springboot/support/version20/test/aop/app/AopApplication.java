/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.aop.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.weaving.AspectJWeavingEnabler;

@SpringBootApplication(excludeName = { "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration" })
public class AopApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(AopApplication.class, AspectJWeavingEnabler.class, AopLoadTimeWeavingConfig.class);
	}

	public static void main(String[] args) {
		// @EnableLoadTimeWeaving annotation doesn't work reliably. Therefore using a
		// workaround which involves making sure that DefaultContextLoadTimeWeaver.class
		// and AspectJWeavingEnabler.class are registered before the application context
		// is refreshed.
		// Reference:https://github.com/spring-projects/spring-framework/issues/29609#issuecomment-1607715665
		SpringApplication.run(
				new Class[] { AopApplication.class, AspectJWeavingEnabler.class, AopLoadTimeWeavingConfig.class },
				args);
	}
}
