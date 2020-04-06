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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.NameAddressHeader;

/**
 * The Path header as defined in RFC 3327:
 * 
 * Path = "Path" HCOLON path-value *( COMMA path-value )
 * path-value = name-addr *( SEMI rr-param )
 * 
 * @author ran
 */
public interface PathHeader extends NameAddressHeader
{
    /**
     * header name
     */
    public static final String name = "Path";
}
