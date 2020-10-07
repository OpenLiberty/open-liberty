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
 * The Refer-To header, as defined in 3515-2.1:
 * 
 * Refer-To = ("Refer-To" / "r") HCOLON ( name-addr / addr-spec ) * (SEMI generic-param)
 * 
 * @author ran
 */
public interface ReferToHeader extends NameAddressHeader
{
    /**
     * header name
     */
    public final static String name = "Refer-To";
}
