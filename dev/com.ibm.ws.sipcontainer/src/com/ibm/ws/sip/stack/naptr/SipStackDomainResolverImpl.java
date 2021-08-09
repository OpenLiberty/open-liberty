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
package com.ibm.ws.sip.stack.naptr;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.internalapi.SipStackDomainResolver;

public class SipStackDomainResolverImpl {
	
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipStackDomainResolverImpl.class);
	
	
	protected static SipStackDomainResolver _domainResolver = null;
	
	public static void setInstance(SipStackDomainResolver resolver) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("setting stack instance of domain resolver");
		}
		
		_domainResolver = resolver;
	}

	
	public static SipStackDomainResolver getInstance() {
		return _domainResolver;
	}

}
