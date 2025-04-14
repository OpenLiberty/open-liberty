/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

public class WebXmlNameSpace implements NamespaceContext {

	public String getNamespaceURI(String prefix) {
        if(prefix == null)
            throw new IllegalArgumentException("Unknown namespace prefix - " + prefix, null);
        if(prefix.equals(""))
            return "http://java.sun.com/xml/ns/javaee";
        if(prefix.equals("javaee"))
            return "http://java.sun.com/xml/ns/javaee";
		return "";
	}

	/**
	 * Dummy implementation not in use
	 */
	public String getPrefix(String namespace) {
		return null;
	}

	/**
	 * Dummy implementation not in use
	 */
	public Iterator getPrefixes(String s) {
		return null;
	}


	

}
