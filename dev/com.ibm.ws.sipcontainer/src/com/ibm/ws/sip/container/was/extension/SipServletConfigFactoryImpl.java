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
package com.ibm.ws.sip.container.was.extension;

import javax.servlet.ServletConfig;

import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

/**
 * The ServletConfig factory creating SipServletConfig wrappers for the ServletConfig
 * @author Nitzan
 */
public class SipServletConfigFactoryImpl implements SipServletConfigFactory {

	/**Singleton instance*/
	private static SipServletConfigFactory instance = new SipServletConfigFactoryImpl();
	/**
	 * static getter for factory singleton
	 * @return
	 */
	public static SipServletConfigFactory getInstance(){
		return instance;
	}
	
	/**
	 * private Ctor
	 */
	private SipServletConfigFactoryImpl(){
		
	}
	
	/**
	 * @see com.ibm.ws.sip.container.was.extension.SipServletConfigFactory#createSipServletConfig(javax.servlet.ServletConfig)
	 */
	public IServletConfig createSipServletConfig(ServletConfig impl) {
		return new SipServletConfig(impl);
	}

}
