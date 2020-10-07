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
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author Assya Azrieli, September 20, 2004
 *
 * Implementation of the Address API for Contact System headers 
 */
public class ContactSystemAddressImpl extends AddressImpl
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -205267488174013471L;

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ContactSystemAddressImpl.class);

    /**
     * Construct a new Adderss from the given Jain Sip Name Address Header.
     * @param nameAddressHeader Name Address Header that the address will be 
     * taken from. 
     * @pre nameAddressHeader != null
     */
    protected ContactSystemAddressImpl(NameAddressHeader nameAddressHeader)
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
                	rUri = new ContactSystemSipURIImpl((SipURL)jainUri);
                }
                else  if (TelURLImpl.isSchemeSupported(scheme)){
                	rUri = new SystemTelURLImpl(jainUri);
                }
                else
                {
                    rUri = new URIImpl(jainUri);
                }
                if(c_logger.isTraceEntryExitEnabled()){
        			c_logger.traceExit(ContactSystemAddressImpl.class.getName(), 
        			"getURI",rUri.getClass().getName()+ "@" + Integer.toHexString(rUri.hashCode()));
        		}
            } else {
            	if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getURI", "jainUri is null");
				}
            }
        }
        
        return rUri;

    }
  
    /**
     * In this Class only user part MAY be modified
     * @see javax.servlet.sip.AddressImpl#setURI(URI)
     */
    @Override
	public void setURI(URI uri)
    {
    	if(uri instanceof SipURIImpl){
    		
    		SipURIImpl sipUri = (SipURIImpl)uri;
    		String userPart = sipUri.getUser();

    		if(userPart!= null){
    			((SipURIImpl) getURI()).setUser(userPart);
    		}
    	}
    }
    
    /**
     * @see javax.servlet.sip.AddressImpl#setDisplayName(String)
     */
    @Override
	public void setDisplayName(String name){
    	if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ALLOW_SETTING_SYSTEM_CONTACT_DISPLAY_NAME)){
    		super.setDisplayName(name);
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setDisplayName", "setting display name for contact header is allowed, display-name=" + name);
			}
    	} else {
    		throw new IllegalStateException("This Address is used in a Contact System " +
    		"header context where it cannot be modified");
    	}
    }
}