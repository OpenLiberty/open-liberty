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
 * <p>
 * This interface represents any header that contains an option tag value.
 * It is the super-interface of ProxyRequireHeader, RequireHeader and
 * UnsupportedHeader.
 * </p>
 *
 * @see ProxyRequireHeader
 * @see RequireHeader
 * @see UnsupportedHeader
 *
 * @version 1.0
 *
 */
public interface OptionTagHeader extends Header
{
    
    /**
     * Sets option tag of OptionTagHeader
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public void setOptionTag(String optionTag)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets option tag of OptionTagHeader
     * @return option tag of OptionTagHeader
     */
    public String getOptionTag();
}
