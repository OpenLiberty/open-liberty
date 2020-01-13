/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.Header;

/**
 * The Content language header. 
 * 
 * @author Assaf Azaria, May 2003.
 */
public interface ContentLanguageHeader extends Header
{
	/**
	 * Name of ContentLanguage Header.
	 */
	public final static String name = "Content-Language";
    
    /**
	 * Sets language tag of ContentLaguageHeader
	 * @param <var>languageTag</var> language tag
	 * @throws IllegalArgumentException if languageTag is null
	 */
    public void setLanguageTag(String languageTag)
    	throws IllegalArgumentException;

    /**
     * Gets langauge tag of ContentLaguageHeader
     * @return language tag of ContentLaguageHeader
     */
    public String getLanguageTag();
}
