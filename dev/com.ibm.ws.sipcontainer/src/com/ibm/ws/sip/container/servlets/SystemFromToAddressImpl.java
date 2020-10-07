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

import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.NameAddressHeader;

import javax.servlet.sip.URI;

public class SystemFromToAddressImpl extends FromToAddressImpl {
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -205267488174013471L;
    /**
     * Construct a new Adderss from the given Jain Sip Name Address Header.
     * @param nameAddressHeader Name Address Header that the address will be 
     * taken from. 
     * @pre nameAddressHeader != null
     */	
	protected SystemFromToAddressImpl(NameAddressHeader nameAddressHeader) {
		super(nameAddressHeader);
	}
	
    /**
     * returnes an unmutable URI
     * 
     * @see javax.servlet.sip.Address#getURI()
     */
    @Override
    public URI getURI()
    {
        URI rUri = null;

        // ... "This method will return null for wildcard addresses" 
        if ((null != _parametersHeader) && (!isWildcard()))
        {
            jain.protocol.ip.sip.address.URI jainUri =
            		((NameAddressHeader)_parametersHeader).getNameAddress().getAddress();
            if (null != jainUri)
            {
                String scheme = jainUri.getScheme();
                if (SipURIImpl.isSchemeSupported(scheme))
                {
                	rUri = new SystemSipURIImpl((SipURL)jainUri);
                }
                else  if (TelURLImpl.isSchemeSupported(scheme)){
                	rUri = new TelURLImpl(jainUri);
                }
                else
                {
                    rUri = new URIImpl(jainUri);
                }
            }
        }
        return rUri;

    }
    
    /**
     * In this Class only user part MAY be modified
     * @see javax.servlet.sip.Address#setURI(URI)
     */
    public void setURI(URI uri)
    {
		throw new IllegalStateException("This Address is used in a System " +
				"header context where it cannot be modified");
    }    
    
	/**
	 * @see com.ibm.ws.sip.container.servlets.ParameterableImpl#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name)
    {
		/*
		 * JSR 289: For the From and To headers all parts of the headers except the tags 
		 * (tag parameter) can be modified as described in 4.1.2 The From and To Header Fields.
		 */
		if (AddressImpl.TAG.equals(name)) {
	    	throw new IllegalStateException(
				"This Address is used in a System header context where it cannot be modified");			
		}
		super.removeParameter(name);
    }

    /**
     * JSR 289 4.1.2:
     * Containers that need to support [RFC 2543] MUST NOT allow modification of the From and To headers 
     * as that RFC requires the entire URI for dialog identification. Container support for Connected 
     * Identity [RFC 4916] is optional in this specification and is indicated by the presence of 
     * the "from-change" option tag in the javax.servlet.sip.supported list. (see 3.2 Extensions Supported).
     * @see com.ibm.ws.sip.container.servlets.ParameterableImpl#setValue(java.lang.String)
	 */
	public void setValue(String value) throws IllegalStateException {
		throw new IllegalStateException("Cannot modify this system header");
	}
	
	/**
     * @see javax.servlet.sip.AddressImpl#setDisplayName(String)
     */
    @Override
	public void setDisplayName(String name) {
    	throw new IllegalStateException("This Address is used in a From/To System " +
		"header context where it cannot be modified");
    }
}
