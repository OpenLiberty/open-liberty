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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import com.ibm.ws.jain.protocol.ip.sip.header.NameAddressHeaderImpl;

/**
 * implementation of the P-Asserted-Identity header per rfc 3325
 * 
 * PAssertedID = "P-Asserted-Identity" HCOLON PAssertedID-value
 *                 *(COMMA PAssertedID-value)
 * PAssertedID-value = name-addr / addr-spec
 * 
 * @author ran
 */
public class PAssertedIdentityHeaderImpl extends NameAddressHeaderImpl
	implements PAssertedIdentityHeader
{
	/**
	 * constructor
	 */
	public PAssertedIdentityHeaderImpl() {
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
