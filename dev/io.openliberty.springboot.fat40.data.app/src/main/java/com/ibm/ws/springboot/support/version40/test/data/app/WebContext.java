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
package com.ibm.ws.springboot.support.version40.test.data.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebContext {

	@Autowired
	private TestPersistence testPersistence;


	@RequestMapping("/testPersistence")
	public String checkJNDI() {
		AccessingDataJpaApplication.doDataTest("WEB_CONTEXT", testPersistence);
		return "TESTED PERSISTENCE";
	}
}
