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
 * This interface represents the Subject request-header.
 * The SubjectHeader is intended to provide a summary, or to
 * indicate the nature, of the call, allowing call filtering
 * without having to parse the session description. (Also, the
 * session description does not have to use the
 * same subject indication as the invitation.)
 *
 * @version 1.0
 *
 */
public interface SubjectHeader extends Header
{
    
    /**
     * Sets subject of SubjectHeader
     * @param <var>subject</var> subject
     * @throws IllegalArgumentException if subject is null
     * @throws SipParseException if subject is not accepted by implementation
     */
    public void setSubject(String subject)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets subject of SubjectHeader
     * @return subject of SubjectHeader
     */
    public String getSubject();
    
    //////////////////////////////////////////////////////////
    
    /**
     * Name of SubjectHeader
     */
    public final static String name = "Subject";
}
