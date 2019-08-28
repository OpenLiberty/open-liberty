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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import com.ibm.ws.jain.protocol.ip.sip.header.NameAddressHeaderImpl;

/**
 * implementation of the P-Preferred-Identity header per rfc 3325
 * 
 * PPreferredID = "P-Preferred-Identity" HCOLON PPreferredID-value
 *                   *(COMMA PPreferredID-value)
 * PPreferredID-value = name-addr / addr-spec
 * 
 * @author ran
 */
public class PPreferredIdentityHeaderImpl extends NameAddressHeaderImpl
	implements PPreferredIdentityHeader
{
	/**
	 * constructor
	 */
	public PPreferredIdentityHeaderImpl() {
		super();
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * determines whether or not this header can have nested values
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isNested()
	 */
	public boolean isNested() {
		return true;
	}
}
