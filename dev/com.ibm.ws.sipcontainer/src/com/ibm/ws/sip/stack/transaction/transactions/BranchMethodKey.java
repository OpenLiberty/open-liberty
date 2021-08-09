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
package com.ibm.ws.sip.stack.transaction.transactions;

/**
 * unique transaction identifier,
 * consisting of the combination of:
 * 1. request method, and
 * 2. top via branch
 * 
 * @author ran
 */
public class BranchMethodKey 
{
	/** the request method */
	private String m_method;
	
	/** branch from the top via header */
	private String m_branch;
	
	/** the  sent-by host */
	private String m_host;
	
	/** branch sent-by port  */
	private int m_port;
	/**
	 * constructor
	 */
	BranchMethodKey(String method, String branch, String host, int port) {
		m_method = method;
		m_branch = branch;
		m_host = host;
		m_port = port;
	}
	
	/**
	 * calculates the hash code of this transaction identifier
	 */
	public int hashCode() {
		
		int result = 1;
		result = 31 * result + m_method.hashCode();
		result = 31 * result + m_branch.hashCode();
		
		result = 31 * result + m_host.hashCode();
		result = 31 * result + (int)m_port;
				
		return result;
	}
	
	/**
	 * @return the request method
	 */
	public String getMethod() {
		return m_method;
	}
	
	/**
	 * @return branch from the top via header
	 */
	public String getBranch() {
		return m_branch;
	}
	
	/**
	 * compares this transaction identifier with another
	 */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		
		if (!(other instanceof BranchMethodKey)) {
			return false;
		}
		
		BranchMethodKey o = (BranchMethodKey)other;
				
		boolean rc = m_method.equalsIgnoreCase(o.m_method) &&
				m_branch.equalsIgnoreCase(o.m_branch);
		
		rc = rc && m_host.equalsIgnoreCase(o.m_host)&&
				(m_port == o.m_port ? true : false);
		return rc;		
	}
	
	/**
	 * string representation
	 */
	public String toString() {
		return m_method + m_branch;
	}
}
