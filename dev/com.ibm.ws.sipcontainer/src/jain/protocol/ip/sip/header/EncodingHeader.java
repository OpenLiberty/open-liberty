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
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

/**
 * <p>
 * This interface represents any header that contains an encoding value.
 * It is the super-interface of AcceptEncodingHeader and
 * ContentEncodingHeader.
 * </p>
 *
 * @see AcceptEncodingHeader
 * @see ContentEncodingHeader
 *
 * @version 1.0
 *
 */
public interface EncodingHeader extends Header
{
    
    /**
     * Sets the encoding of EncodingHeader
     * @param <var>encoding</var> encoding
     * @throws IllegalArgumentException if encoding is null
     * @throws SipParseException if encoding is not accepted by implementation
     */
    public void setEncoding(String encoding)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets the encoding of EncodingHeader
     * @return encoding of EncodingHeader
     */
    public String getEncoding();
}
