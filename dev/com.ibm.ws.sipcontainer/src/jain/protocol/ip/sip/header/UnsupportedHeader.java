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
package jain.protocol.ip.sip.header;

/**
 * This interface represents the Unsupported response-header.
 * UnsupportedHeaders list the features not
 * supported by the server. See RequireHeader for more information.
 *
 * @see RequireHeader
 *
 * @version 1.0
 *
 */
public interface UnsupportedHeader extends OptionTagHeader
{
    
    /**
     * Name of UnsupportedHeader
     */
    public final static String name = "Unsupported";
}
