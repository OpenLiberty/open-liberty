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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Assya Azrieli, September 20, 2004
 *
 * Implementation of the Address API for System headers 
 */
public class SystemAddressImpl extends ContactSystemAddressImpl
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -205267488174013471L;

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SystemAddressImpl.class);

    /**
     * Construct a new Adderss from the given Jain Sip Name Address Header.
     * @param nameAddressHeader Name Address Header that the address will be 
     * taken from. 
     * @pre nameAddressHeader != null
     */
    protected SystemAddressImpl(NameAddressHeader nameAddressHeader)
    {
    	super(nameAddressHeader);
    }
  
    /**
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
                	rUri = new SystemTelURLImpl(jainUri);
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
    @Override
	public void setURI(URI uri)
    {
    	throw new IllegalStateException("This Address is used in a System " +
    			"header context where it cannot be modified");
    }
    
    /**
     * @see com.ibm.ws.sip.container.servlets.ParameterableImpl#setParameter
     * 	(java.lang.String, java.lang.String)
     */
    @Override
	public void setParameter(String name, String value){
    	throw new IllegalStateException("This Address is used in a System " +
			"header context where it cannot be modified");
    }
    
    /**
     * @see com.ibm.ws.sip.container.servlets.ParameterableImpl#setValue(java.lang.String)
	 */
	@Override
	public void setValue(String value) throws IllegalStateException {
		throw new IllegalStateException("This Address is used in a System " +
		"header context where it cannot be modified");
	}
	
    /**
     * @see javax.servlet.sip.AddressImpl#setExpires(int)
     */
    @Override
	public void setExpires(int seconds)
    {
    	throw new IllegalStateException("This Address is used in a System " +
		"header context where it cannot be modified");
    }

    
    /**
     * @see javax.servlet.sip.AddressImpl#setQ(float)
     */
    @Override
	public void setQ(float q)
    {
    	throw new IllegalStateException("This Address is used in a System " +
		"header context where it cannot be modified");
    }
}
