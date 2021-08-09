/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class SystemTelURLImpl extends TelURLImpl {
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SystemTelURLImpl.class);

	

	public SystemTelURLImpl(jain.protocol.ip.sip.address.URI url) {
		super(url);
	}

	/**
	 * @see javax.servlet.sip.TelURL#removeParameter(String)
	 */
	@Override
	public void removeParameter(String name) {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeParameter", "parameter: " + name + 
				" This TelURI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
	}

	/**
	 * @see javax.servlet.sip.TelURL#setParameter(String, String)
	 */
	@Override
	public void setParameter(String name, String value) {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setParameter", "parameter: " + name + 
				" This TelURI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
	}

	
	/**
	 * @see javax.servlet.sip.TelURL#setPhoneNumber(String, String)
	 */
	@Override
	public void setPhoneNumber(String number, String phoneContext)
			throws IllegalArgumentException {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setPhoneNumber", "phone number: " + number + 
				" This TelURI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
	}

	/**
	 * @see javax.servlet.sip.TelURL#setPhoneNumber(String)
	 */
	@Override
	public void setPhoneNumber(String number) throws IllegalArgumentException {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setPhoneNumber", "phone number: " + number + 
				" This TelURI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
	}
}
