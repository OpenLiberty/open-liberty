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

import jain.protocol.ip.sip.header.NameAddressHeader;

public class FromToAddressImpl extends AddressImpl {
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -205267488174013471L;	
	
    /**
     * Construct a new Adderss from the given Jain Sip Name Address Header.
     * @param nameAddressHeader Name Address Header that the address will be 
     * taken from. 
     * @pre nameAddressHeader != null
     */
    protected FromToAddressImpl(NameAddressHeader nameAddressHeader)
    {
    	super(nameAddressHeader);
    }
    
	/**
     * We don't allow changing the TAG paramter
     * 
     * @exception IllegalStateException
     */
	@Override
	public void setTag(String tagValue) {
    	throw new IllegalStateException(
    			"This Address is used in a System header context where it cannot be modified");
	}

	/**
	 * We don't allow changing the TAG paramter
	 * 
	 * @exception IllegalStateException
	 */
	@Override
	public void setParameter(String name, String value) {
		if (AddressImpl.TAG.equals(name)) {
	    	throw new IllegalStateException(
				"This Address is used in a System header context where it cannot be modified");			
		}
		super.setValue(value);
	}
    
    

}
