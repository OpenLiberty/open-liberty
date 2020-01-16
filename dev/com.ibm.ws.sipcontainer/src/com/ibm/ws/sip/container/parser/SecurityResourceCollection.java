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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SecurityResourceCollection {

	/**
	 * parent constraint
	 */
	private SecurityConstraint m_secConstraint = null;
	
	/**
	 * the reource name
	 */
	private String m_name=null;
	
	/**
	 * List of SipServletDesc
	 */
	private List m_siplets = new LinkedList();
	
	/**
	 * List of String
	 */
	private List m_methods = new LinkedList();
	
	/**
	 * @param secCon
	 */
	public SecurityResourceCollection( SecurityConstraint secCon) {
		m_secConstraint = secCon;
		m_secConstraint.addResourceCollections(this);
	}
	
	/**
	 * @return Returns the resource name.
	 */
	public String getResourceName() {
		return m_name;
	}
	/**
	 * @param m_name The resource name to set.
	 */
	public void setResourceName(String m_name) {
		this.m_name = m_name;
	}
	/**
	 * @return Returns the m_secConstraint.
	 */
	public SecurityConstraint getSecurityConstraint() {
		return m_secConstraint;
	}
	/**
	 * @param constraint The m_secConstraint to set.
	 */
	public void setSecurityConstraint(SecurityConstraint constraint) {
		m_secConstraint = constraint;
	}
	/**
	 * add siplet
	 * @param siplet
	 */
	public void addSiplet(SipServletDesc siplet){
		m_siplets.add(siplet);
	}
	
	/**
	 * return siplets
	 * @return
	 */
	public List getSiplets(){
		return m_siplets;
	}
	
	/**
	 * add method
	 * @param method
	 */
	public void addMethod(String method){
		m_methods.add(method);
	}
	
	/**
	 * return List methods
	 * @return
	 */
	public List getMethods(){
		return m_methods;
	}
	
	/**
	 * 
	 * @param method
	 * @return
	 */
	public boolean isMethodInResource(String method){
		
		for (Iterator iter = m_methods.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if(element.equals(method)){
				return true;
			}
			
		}
		return false;
	}
	
}
