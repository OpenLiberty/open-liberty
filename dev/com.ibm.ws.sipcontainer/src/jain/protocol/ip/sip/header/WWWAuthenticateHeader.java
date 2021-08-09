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
 * This interface represents the WWW-Authenticate response-header.
 * At least one WWWAuthenticateHeader must be included in UNAUTHORIZED
 * Responses. The header value consists of a
 * challenge that indicates the authentication scheme(s) and
 * parameters applicable to the RequestURI.
 *
 * @see AuthorizationHeader
 *
 * @version 1.0
 *
 */
public interface WWWAuthenticateHeader extends SecurityHeader
{
    
    /**
     * Name of WWWAuthenticateHeader
     */
    public final static String name = "WWW-Authenticate";
}
