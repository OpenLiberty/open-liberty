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
 * This interface represents the Organization general-header.
 * The OrganizationHeader conveys the name of the organization
 * to which the entity issuing the Request or Response
 * belongs. It may also be inserted by proxies at the boundary of an
 * organization. It may be used by client software to filter calls.
 *
 * @version 1.0
 *
 */
public interface OrganizationHeader extends Header
{
    
    /**
     * Sets organization of OrganizationHeader
     * @param <var>organization</var> organization
     * @throws IllegalArgumentException if organization is null
     * @throws SipParseException if organization is not accepted by implementation
     */
    public void setOrganization(String organization)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets organization of OrganizationHeader
     * @return organization of OrganizationHeader
     */
    public String getOrganization();
    
    ////////////////////////////////////////////////////////////
    
    /**
     * Name of OrganizationHeader
     */
    public final static String name = "Organization";
}
