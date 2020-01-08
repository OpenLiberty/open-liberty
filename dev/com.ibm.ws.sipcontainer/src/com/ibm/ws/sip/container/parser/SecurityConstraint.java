/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.parser;

import java.util.LinkedList;
import java.util.List;

public class SecurityConstraint {
	/**
	 * the constraint display name
	 */
	private String m_displayName = null;
	
	/**
	 * resource collections
	 */
	private List m_resourceCollections = new LinkedList();
	
	/**
	 * mark if this constraint requries proxy-authentication
	 */
	private boolean m_isProxyAuthenticate=false;
	
	
	/**
	 * 
	 */
	public SecurityConstraint() {
	}
	
	
	/**
	 * @return Returns the displayName.
	 */
	public String getDisplayName() {
		return m_displayName;
	}
	/**
	 * @param name The m_displayName to set.
	 */
	public void setDisplayName(String name) {
		m_displayName = name;
	}
	/**
	 * @return Returns the isProxyAuthenticate.
	 */
	public boolean isProxyAuthenticate() {
		return m_isProxyAuthenticate;
	}
	/**
	 * @param proxyAuthenticate The m_isProxyAuthenticate to set.
	 */
	public void setProxyAuthenticate(boolean proxyAuthenticate) {
		m_isProxyAuthenticate = proxyAuthenticate;
	}
	/**
	 * @return Returns the m_resourceCollections.
	 */
	public List getResourceCollections() {
		return m_resourceCollections;
	}
	/**
	 * @param collection
	 */
	public void addResourceCollections(SecurityResourceCollection collection) {
		m_resourceCollections.add(collection);
	}
}
