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
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * This interface represents a header that contains product information.
 * It is the super-interface of ServerHeader and UserAgentHeader.
 * </p>
 *
 * @see ServerHeader
 * @see UserAgentHeader
 *
 * @version 1.0
 *
 */
public interface ProductHeader extends Header
{
    
    /**
     * Set products of ProductHeader
     * (Note that the Objects in the List must be Strings)
     * @param <var>products</var> products
     * @throws IllegalArgumentException if products is null, empty, or contains
     * any null elements, or contains any non-String objects
     * @throws SipParseException if any element of products is not accepted by implementation
     */
    public void setProducts(List products)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets products of ProductHeader
     * (Note that the Objects returned by the Iterator are Strings)
     * @return products of ProductHeader
     */
    public Iterator getProducts();
}
