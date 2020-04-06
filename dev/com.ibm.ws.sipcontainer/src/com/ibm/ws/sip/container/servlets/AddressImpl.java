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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Feb 17, 2003
 *
 * Implementation of the Address API. 
 */
public class AddressImpl extends ParameterableImpl implements Address
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -205267488174013471L;

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(AddressImpl.class);

    /**
     * Definition for the "q" parameter
     */
    private static final String Q = "q";

    /**
     * Definition for the "tag" parameter
     */
    public static final String TAG = "tag";

    /**
     * Definition for the "expires" parameter
     */
    private static final String EXPIRES = "expires";

    /**
     * Construct a new Adderss from the given Jain Sip Name Address Header.
     * @param nameAddressHeader Name Address Header that the address will be 
     * taken from. 
     * @pre nameAddressHeader != null
     */
    protected AddressImpl(NameAddressHeader nameAddressHeader)
    {
    	super(nameAddressHeader);
    }
    
    
    /**
     * Helper method that provide clone of the header "as is" if the
     * @param isSecured
     * @return
     */
    public Object clone(boolean isProtectedInstance)
    {
    	if(isProtectedInstance){
    		return super.clone();
    	}
    	
//    	 The cloned instance should be able to been modified by the application.
    	// This is the reason that we should retrieve a not "System" instance
    	AddressImpl cloned = new AddressImpl((NameAddressHeader)_parametersHeader.clone());
    	
    	// set new copy of SipURI to prevent current URI modification
    	setURIcopy(cloned);
 
    	/*
    	 * jwl - Setting this tag to null, caused us to fail the JSR289 TCK tests (testClone001).
    	 * Evidently, the tag needs to remain set.
    	 */
//    	cloned.setTag(null);
        return cloned;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
    {
    	return clone(false);
    }

    /**
     * Set new copy of SipURI to prevent current URI modification
     * @param cloned
     */
    private void setURIcopy(AddressImpl cloned) {

        // ... "This method will return null for wildcard addresses" 
        if ((null != _parametersHeader) && (!isWildcard()))
        {
        	URI rUri = null;
            jain.protocol.ip.sip.address.URI jainUri =
            		((NameAddressHeader)_parametersHeader).getNameAddress().getAddress();
            if (null != jainUri)
            {
                String scheme = jainUri.getScheme();
                if (SipURIImpl.isSchemeSupported(scheme))
                {
                	rUri = new SipURIImpl((SipURL)jainUri.clone());
                }
                else if (TelURLImpl.isSchemeSupported(scheme)){
                	rUri = new TelURLImpl(jainUri);
                }
                else
                {
                    rUri = new URIImpl((jain.protocol.ip.sip.address.URI)jainUri.clone());
                }
            }

            // set the new copy of URI to the header
            if(rUri!=null){
            	cloned.setURI(rUri);
            }
        }
	}

	/**
     * @see javax.servlet.sip.Address#getDisplayName()
     */
    public String getDisplayName()
    {
        String displayName = null;
        if (null != _parametersHeader)
        {
            displayName = ((NameAddressHeader)_parametersHeader).getNameAddress().getDisplayName();

        }
        return displayName;
    }

    /**
     * @see javax.servlet.sip.Address#getExpires()
     */
    public int getExpires()
    {
        int rc = -1;
        String expires = getParameter(EXPIRES);
        if (null != expires)
        {
            try
            {
                rc = Integer.parseInt(expires);
                
                if(rc < 0)
                {
                	// 3261-8.3
                    // Malformed values SHOULD be treated as equivalent to 3600
                    rc = 3600;

                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { expires };
                        c_logger.error(
                            "error.get.expires",
                            Situation.SITUATION_REQUEST,
                            args);
                    }

                }
            }
            catch (NumberFormatException e)
            {
                // 3261-8.3
                // Malformed values SHOULD be treated as equivalent to 3600
                rc = 3600;

                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { expires };
                    c_logger.error(
                        "error.get.expires",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
        }

        return rc;
    }
    

    /**
     * @see javax.servlet.sip.Address#getQ()
     */
    public float getQ()
    {
        float rValue = -1;
        String value = getParameter(Q);

        if (null != value)
        {
            rValue = Float.parseFloat(value);
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.Address#getURI()
     */
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
                	rUri = new SipURIImpl((SipURL)jainUri);
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
     * @see javax.servlet.sip.Address#isWildcard()
     */
    public boolean isWildcard()
    {
    	if (_parametersHeader instanceof ContactHeader) {
    		ContactHeader contact = (ContactHeader)_parametersHeader;
    		return contact.isWildCard();
    	}
        return false;
    }

    
    /**
     * @see javax.servlet.sip.Address#setDisplayName(String)
     */
    public void setDisplayName(String name)
    {
        try
        {
        	((NameAddressHeader)_parametersHeader).getNameAddress().setDisplayName(name);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.set.display.name",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.set.display.name",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * @see javax.servlet.sip.Address#setExpires(int)
     */
    public void setExpires(int seconds)
    {
        if (seconds < 0)
        {
            removeParameter(EXPIRES);
        }
        else
        {
            setParameter(EXPIRES, Integer.toString(seconds));
            
        }
    }

    
    /**
     * @see javax.servlet.sip.Address#setQ(float)
     */
    public void setQ(float q)
    {
        if (q == -1)
        {
            removeParameter(Q);
        }
        else if (q >= 0 && q <= 1)
        {
        	setParameter(Q, Float.toString(q));
        }
        else
        {
            throw new IllegalArgumentException("Illegal Q Value");
        }

    }

    /**
     * @see javax.servlet.sip.Address#setURI(URI)
     * @pre uri != null
     */
    public void setURI(URI uri)
    {
        try
        {
            jain.protocol.ip.sip.address.URI jainURI = ((URIImpl)uri).getJainURI();
            ((NameAddressHeader)_parametersHeader).getNameAddress().setAddress(jainURI);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { uri };
                c_logger.error(
                    "error.set.uri",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * Gets the tag associated with this address. 
     * @return The tag if available otherwise null. 
     */
    public String getTag()
    {
        return getParameter(TAG);
    }

    /**
     * Sets the tag associated with the address.
     *  
     */
    public void setTag(String tagValue)
    {
        if (tagValue == null)
        {
            removeParameter(TAG);
        }
        else
        {
            setParameter(TAG, tagValue);
        }
    }

      
    /**
     * Get the internal Jain Name Address Header wrapped by this object. 
     * @return
     */
    public NameAddressHeader getNameAddressHeader()
    {
        return ((NameAddressHeader)_parametersHeader);
    }
    
    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
       boolean rc = false;
       if (obj instanceof AddressImpl) {
    	   AddressImpl other = (AddressImpl) obj;
    	   SipURL urlObj = (SipURL)other.getNameAddressHeader().getNameAddress().getAddress();
    	   SipURL localURI = (SipURL)getNameAddressHeader().getNameAddress().getAddress();
    	   
    	   
           if (localURI.getPort() == urlObj.getPort() && 
           	( (localURI.getUserName() != null && localURI.getUserName().equals(urlObj.getUserName())) ||
           	  (localURI.getUserName() == null && urlObj.getUserName() == null)) &&
           	  localURI.getHost().equalsIgnoreCase(urlObj.getHost()) &&
           	  compareParameters(other)) 
           {
           		rc = true;
           }
       }
        return rc;
    }
    
    /**
	 * this method is used internally to remove the tag form the FromToAddress, it is not
	 * exposed in the API, customers should not use it
	 */
	public void removeTag(){
		super.removeParameter(AddressImpl.TAG);
	}
}