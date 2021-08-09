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
package com.ibm.ws.sip.container.osgi;

import javax.servlet.ServletConfig;

public interface ServletContextFactory {
	
	/**
	 * 
	 * @param context
	 * @return wraped context
	 */
	public ServletConfig wrapContext(ServletConfig context);

}
