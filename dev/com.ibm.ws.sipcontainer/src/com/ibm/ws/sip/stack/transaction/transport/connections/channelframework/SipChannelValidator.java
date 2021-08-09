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
package com.ibm.ws.sip.stack.transaction.transport.connections.channelframework;

//TODO Liberty
//import com.ibm.websphere.validation.base.config.MOFValidator;
//import com.ibm.wsspi.channel.impl.BaseChannelTypeValidator;

/**
 * @author ran
 */
public class SipChannelValidator /*TODO Liberty extends BaseChannelTypeValidator*/
{
	/**
	 * constructor
	 */
	public SipChannelValidator(/*TODO Liberty MOFValidator parentValidator*/) {
		/*super(parentValidator);*/
	}

	/**
	 * @see com.ibm.websphere.validation.base.config.WebSphereDelegateValidator#getLocalBundleID()
	 */
	protected String getLocalBundleID() {
		return null;
	}

	/**
	 * @see com.ibm.websphere.validation.base.config.WebSphereDelegateValidator#getLocalTraceName()
	 */
	protected String getLocalTraceName() {
		return "SipContainer";
	}
}
