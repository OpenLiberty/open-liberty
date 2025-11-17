/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.fat40.multicontext.app.part0;

import com.ibm.ws.springboot.fat40.multicontext.app.part00.ServletConfig1;
import com.ibm.ws.springboot.fat40.multicontext.app.part01.ServletConfig2;

import org.springframework.boot.WebApplicationType;

import org.springframework.boot.builder.SpringApplicationBuilder;

public class TestApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder().parent(Config.class)
			.web(WebApplicationType.NONE)
			.child(ServletConfig1.class)
			.web(WebApplicationType.SERVLET)
			.sibling(ServletConfig2.class)
			.web(WebApplicationType.SERVLET)
			.run(args);
	}

}
