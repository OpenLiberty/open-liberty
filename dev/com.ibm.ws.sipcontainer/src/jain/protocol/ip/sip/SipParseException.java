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
package jain.protocol.ip.sip;

/**
 * This exception is thrown by an implementation if it cannot parse any given value
 * correctly. The value that is unable to be parsed correctly can be accessed using the
 * getUnparsable method.
 *
 * @version 1.0
 */
public class SipParseException extends SipException
{
    
    /**
     * Constructs new SipParseException based on specified unparsable value
     * @param <var>unspasable</var> unparsable value to base SipParseException on
     */
    public SipParseException(String unparsable) 
    {
        this.unparsable = unparsable;
    }
    
    /**
     * Gets unparsable value of SipParseException
     * @return unparsable value of SipParseException
     */
    public String getUnparsable()
    {
        return unparsable;
    }
    
    /**
     * Constructs new SipParseException based on specified unparsable value and a message
     * @param <var>msg</var> message of this SipParseException.
     * @param <var>unspasable</var> unparsable value to base SipParseException on
     */
    public SipParseException(String message, String unparsable) 
    {
        super(message);
        this.unparsable = unparsable;
    }
    private String unparsable = null;
}
