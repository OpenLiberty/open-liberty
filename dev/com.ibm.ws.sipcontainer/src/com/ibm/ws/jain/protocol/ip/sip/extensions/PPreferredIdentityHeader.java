/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.NameAddressHeader;

/**
 * the P-Preferred-Identity header per rfc 3325
 * 
 * @author ran
 */
public interface PPreferredIdentityHeader extends NameAddressHeader
{
    /**
     * header name
     */
    public final static String name = "P-Preferred-Identity";
}
