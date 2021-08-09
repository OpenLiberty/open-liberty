/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.managedbeans;

import java.util.Set;

import com.ibm.ws.jaxrs20.metadata.EndpointInfo;

public class ManagedBeanEndpointInfo extends EndpointInfo {

	public ManagedBeanEndpointInfo(String servletName, String servletClassName,
			String servletMappingUrl, String appClassName, String appPath,
			Set<String> providerAndPathClassNames) {
		super(servletName, servletClassName, servletMappingUrl, appClassName, appPath,
				providerAndPathClassNames);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
}
