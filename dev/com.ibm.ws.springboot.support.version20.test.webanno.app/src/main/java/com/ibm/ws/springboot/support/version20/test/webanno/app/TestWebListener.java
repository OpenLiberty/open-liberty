/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.version20.test.webanno.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.springframework.stereotype.Component;

@Component
public class TestWebListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent e) {
		// nothing
	}

	@Override
	public void contextInitialized(ServletContextEvent e) {
		e.getServletContext().setAttribute(TestApplication.TEST_ATTR, "PASSED");
	}

}
