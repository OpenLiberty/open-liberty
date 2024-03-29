/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
package com.ibm.ws.sip.container.was.extension;

import com.ibm.wsspi.webcontainer.servlet.GenericServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * A servlet wrapper that executes siplets. 
 */
public class SipServletWrapper extends GenericServletWrapper {

	/**
	 * Ctor
	 * @param parent
	 * @throws Exception
	 */
	public SipServletWrapper(IServletContext parent) throws Exception {
		super(parent);		
	}
}
