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

/**
 * This interface represents the Accept-Language request-header.
 * AcceptLanguageHeader can be used in a Request to allow
 * the client to indicate to the server in which language it would
 * prefer to receive reason phrases, session descriptions or status
 * responses carried as message bodies. The q-value is used in a similar
 * manner to AcceptHeader to indicate degrees of preference.
 *
 * @see AcceptHeader
 *
 * @version 1.0
 *
 */
public interface AcceptLanguageHeader extends Header
{
    
    /**
     * Gets the q-value of language-range in AcceptLanguageHeader
     * (returns negative float if no q-value exists)
     * @return the q-value of language-range in AcceptLanguageHeader
     */
    public float getQValue();
    
    /**
     * Indicates whether or not a q-value exists in AcceptLanguageHeader
     * @return boolean to indicate whether or not a q-value exists in AcceptLanguageHeader
     */
    public boolean hasQValue();
    
    /**
     * Sets the language-range of AcceptLanguageHeader
     * @param <var>languageRange</var> language-range of AcceptLanguageHeader
     * @throws IllegalArgumentException if languageRange is null
     * @throws SipParseException if languageRange is not accepted by implementation
     */
    public void setLanguageRange(String languageRange)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets q-value for media-range in AcceptLanguageHeader
     * Q-values allow the user to indicate the relative degree of
     * preference for that language-range, using the qvalue scale from 0 to 1.
     * (If no q-value is present, the language-range should be treated as having a q-value of 1.)
     * @param <var>qValue</var> q-value
     * @throws SipParseException if qValue is not accepted by implementation
     */
    public void setQValue(float qValue)
                 throws SipParseException;
    
    /**
     * Removes q-value from AcceptLanguageHeader (if it exists)
     */
    public void removeQValue();
    
    /**
     * Gets the language-range of AcceptLanguageHeader
     * @return language-range of AcceptLanguageHeader
     */
    public String getLanguageRange();
    
    /**
     * Name of AcceptLanguageHeader
     */
    public final static String name = "Accept-Language";
}
