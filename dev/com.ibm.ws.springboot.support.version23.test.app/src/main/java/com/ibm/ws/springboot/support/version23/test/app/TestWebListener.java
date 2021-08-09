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
package com.ibm.ws.springboot.support.version23.test.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
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
