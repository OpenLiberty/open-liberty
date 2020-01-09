/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util.wlm;

import com.ibm.ws.sip.container.util.wlm.impl.SipContainerWLMHooksImpl;

/**
 * The SIP container implements this, to enable
 * a client to access the SipContainerWLMHooks implementation
 * 
 * @author dror yaffe
 * 
 */
public class SipContainerWLMHooksFactory {

	/**
	 * get the SipContainerWLMHooks implementation
	 */
	public static SipContainerWLMHooks getSipContainerHooks(){
		return SipContainerWLMHooksImpl.getSipContainerWLMHooksInstance();
	}
}
