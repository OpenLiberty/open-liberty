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

/**
 * This interface represents the User-Agent general-header.
 * A UserAgentHeader contains information about the
 * user agent originating the Request. This is for
 * statistical purposes, the tracing of protocol violations,
 * and automated recognition of user agents for the sake of
 * tailoring Responses to avoid particular user
 * agent limitations. User agents should include this header
 * with Requests. Requests can contain multiple
 * UserAgentHeaders identifying the agent and any subproducts
 * which form a significant part of the user agent. By
 * convention, the UserAgentHeaders are listed in order of
 * their significance for identifying the application. Similar
 * security considerations apply for in ServerHeaders.
 *
 * @see ServerHeader
 *
 * @version 1.0
 *
 */
public interface UserAgentHeader extends ProductHeader
{
    
    /**
     * Name of UserAgentHeader
     */
    public final static String name = "User-Agent";
}
