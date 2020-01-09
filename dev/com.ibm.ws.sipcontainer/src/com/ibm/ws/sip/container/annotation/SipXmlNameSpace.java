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
package com.ibm.ws.sip.container.annotation;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

public class SipXmlNameSpace implements NamespaceContext {

	public String getNamespaceURI(String prefix) {
        if(prefix == null)
            throw new IllegalArgumentException("Unknown namespace prefix - " + prefix, null);
        if(prefix.equals(""))
            return "http://www.jcp.org/xml/ns/sipservlet";
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
		// TODO Auto-generated method stub
		return null;
	}


	

}
