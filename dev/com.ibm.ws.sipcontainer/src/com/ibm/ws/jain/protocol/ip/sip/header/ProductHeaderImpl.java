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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ProductHeader;
import jain.protocol.ip.sip.header.ServerHeader;
import jain.protocol.ip.sip.header.UserAgentHeader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Product header implementation.
 * 
 * @author Assaf Azaria.
 * 
 * @see ServerHeader
 * @see UserAgentHeader
 */
public abstract class ProductHeaderImpl extends HeaderImpl
    implements ProductHeader
{

    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -8979717630459169774L;

    /**
     * The list of products. 
     */
    protected List m_products = new ArrayList(2);

    /**
     * @throws SipParseException
     */
    public ProductHeaderImpl() {
        super();
    }

	
	//
	// Methods.
	//
	
    /**
    * Set products of ProductHeader
    * (Note that the Objects in the List must be Strings)
    * @param <var>products</var> products
    * @throws IllegalArgumentException if products is null, empty, or contains
    * any null elements, or contains any non-String objects
    * @throws SipParseException if any element of products is not accepted by implementation
    */
    public void setProducts(List products)
        throws IllegalArgumentException, SipParseException
    {
        if (products == null)
        {
            throw new IllegalArgumentException("Null Product List");
        }
        if (products.isEmpty())
        {
            throw new IllegalArgumentException("Empty Product List");
        }
        
        Iterator iterator = products.iterator();
        String product = null;
        while (iterator.hasNext())
        {
            try
            {
                product = (String)iterator.next();
            }
            catch (ClassCastException e)
            {
                throw new SipParseException("Product is not a String", "");
            }
            if (product == null)
            {
                throw new SipParseException("Null Product", "");
            }
        }
        
        m_products.clear();
        m_products.addAll(products);
    }

    /**
	* Gets products of ProductHeader
	* (Note that the Objects returned by the Iterator are Strings)
	* @return products of ProductHeader
	*/
    public Iterator getProducts()
    {
    	return m_products.iterator();
    }
    
	/**
	 * Get the encoded list of products. 
	 */
	public void encodeProducts(CharsBuffer ret)
	{
		for(int i = 0; i < m_products.size(); i++) 
		{
			ret.append(m_products.get(i));
			
			if(i < (m_products.size() - 1)) 
			{
				ret.append(SLASH);
			}
		}
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_products.clear();
		
        // Product token is a list of tokens separeted by slash.
        while (true) {
            m_products.add(parser.nextToken(SLASH));
            if (parser.LA(1) != SLASH) { 
            	break;
            } 
            parser.match(SLASH);
        }
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof ProductHeaderImpl)) {
			return false;
		}
		ProductHeaderImpl o = (ProductHeaderImpl)other;
		
		if (m_products == null) {
			if (o.m_products == null) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_products == null) {
				return false;
			}
			else {
				return m_products.equals(o.m_products);
			}
		}
	}
	
	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		ProductHeaderImpl ret = (ProductHeaderImpl)super.clone(); 
		if (m_products != null)
		{
			ret.m_products = (List)((ArrayList)m_products).clone();
		} 
		return ret;
	}
}
