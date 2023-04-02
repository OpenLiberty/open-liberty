/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.common;

/**
 * Represents an XML qualified name. Examples:
 * 
 * <ol>
 * <li>&lt;service-name-pattern xmlns:ns1="http://test.ibm.com">ns1:EchoService&lt;/service-name-pattern>
 * <ul>
 * <li>getNamespaceURI() returns "http://test.ibm.com"
 * <li>getLocalPart() returns "EchoService"
 * </ul>
 * </li>
 * <li>&lt;service-name-pattern xmlns:ns1="http://test.ibm.com">ns1:EchoService*&lt;/service-name-pattern>
 * <ul>
 * <li>getNamespaceURI() returns "http://test.ibm.com"
 * <li>getLocalPart() returns "EchoService*"
 * </ul>
 * </li>
 * <li>&lt;service-name-pattern>EchoService&lt;/service-name-pattern>
 * <ul>
 * <li>getNamespaceURI() returns null
 * <li>getLocalPart() returns "EchoService"
 * </ul>
 * </li>
 * <li>&lt;service-name-pattern>*&lt;/service-name-pattern>
 * <ul>
 * <li>getNamespaceURI() returns null
 * <li>getLocalPart() returns "*"
 * </ul>
 * </li>
 * </ol>
 * 
 * @see javax.xml.namespace.QName
 */
public interface QName
{
    /**
     * @return the namespace URI associated with the QName prefix, or null if
     *         this QName does not have a prefix
     */
    String getNamespaceURI();

    /**
     * @return the local name
     */
    String getLocalPart();
}
