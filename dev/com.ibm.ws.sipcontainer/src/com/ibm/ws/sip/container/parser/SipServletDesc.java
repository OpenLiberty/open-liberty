/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.parser;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.sip.container.rules.Condition;

/**
 * @author Amir Perlman, Jun 24, 2003
 *
 * Describes a Sip Servlet. Contains information about the Siplet's properties 
 * and invocation rules. 
 */
public class SipServletDesc
{
	
	/**
	 * The SIP Application associated with this siplet. 
	 */
	private SipAppDesc m_sipApp;
	 
	/**
	 * Siplet's name. 
	 */
	private String m_className;

    /**
     * Siplet's class name. 
     */
    private String m_name;
    
    
    /**
     * The siplet's load on startup value.
     */
    private int m_servletLoadOnStartup;
    
    /**
     * Indicator for knowing whether the siplet has load-on-startup tag in sip.xml or in annotations.
     */
    private boolean m_hasLoadOnStartupTag;
    
    /**
     * Map of initial parameters <init-param> for the siplet.
     */
    private Map<String,String> m_initParams = new LinkedHashMap<String,String>();

    /**
     * The condition for triggering this siplet. 
     */
    private Condition m_condition; 
	
    /**
     * List of Security Resource Collections the Siplets belongs to
     */
    private List m_securityResourceCollections = new LinkedList();
    
    /**
     * JSR 289 defines one servlet as main servlet of the application,
     * all the request should be proceed by that  servlet. 
     * Difrential from JSR 116 that defines set for rules for each servlet. 
     */
    private boolean isMainServlet = false;
    

	/**
	 * Construct a new Sip Servlet configuration description. 
	 * @param name
	 * @param className
	 */
    public SipServletDesc(SipAppDesc app, String name, String className)
	{
		m_sipApp = app; 
		m_name = name; 
		m_className = className; 	
	}
    
    /**
	 * Construct a new Sip Servlet configuration description.
	 * @param app
	 * @param name
	 * @param className
	 * @param servletLoadOnStartup
	 * @param hasLoadOnStartupTag
	 * @param isMainServlet - JSR 289 spec
	 */
    public SipServletDesc(SipAppDesc app, String name, String className, boolean mainServlet, int servletLoadOnStartup, boolean hasLoadOnStartupTag)
	{
		m_sipApp = app; 
		m_name = name; 
		m_className = className;
		m_servletLoadOnStartup = servletLoadOnStartup;
		m_hasLoadOnStartupTag = hasLoadOnStartupTag;
		isMainServlet = mainServlet;
	}
    
    /**
     * Ctor for servlets in sip.xml
     * @param app
     * @param name
     * @param className
     * @param servletLoadOnStartup
     * @param hasLoadOnStartupTag
     */
    public SipServletDesc(SipAppDesc app, String name, String className, int servletLoadOnStartup, boolean hasLoadOnStartupTag)
	{
		m_sipApp = app; 
		m_name = name; 
		m_className = className;
		m_servletLoadOnStartup = servletLoadOnStartup;
		m_hasLoadOnStartupTag = hasLoadOnStartupTag;
	}
	
	/**
	 * Get the Siplet's name. 
	 * @return String
	 */
	public String getName()
	{
		return m_name; 
	}
	
	/**
	 * Get the Siplet's class name. 
	 * @return String
	 */
	public String getClassName()
	{
		return m_className; 
	}
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
		StringBuffer b = new StringBuffer(64);
		b.append(m_name);
		b.append("(");
		b.append(m_className);
		b.append(")");
        
        return  b.toString();
    }
	
	/**
	 * Gets the triggering condition associated with this siplet. 
	 * @return Condition
	 */
	public Condition getTriggeringRule()
	{
		return m_condition; 	
	}
	
	/**
	 * Sets the triggering condition associated with this siplet.
	 * @param condition
	 */
	public void setTriggeringCondition(Condition condition)
	{
		m_condition = condition; 
	}
    
    /**
     * Sip Servlet Descriptor are considered equal if they have a matching
     * name and class definition. 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        boolean rc = false; 
        if(obj instanceof SipServletDesc)
        {
        	SipServletDesc other = (SipServletDesc) obj; 
        	
        	if(other.m_className.equals(m_className) &&
        	   other.m_name.equals(m_name))
    	   	{
    	   		rc = true; 	
    	   	}
        
        }
        
		return rc;
    }

    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return m_name.hashCode() ^ m_className.hashCode();
    }

    
    /**
     * Get the SIP Application associated with this siplet. 
     * @return
     */
    public SipAppDesc getSipApp()
    {
        return m_sipApp;
    }

    /**
     * add security resource collection
     * @param collection
     */
    public void addSecurityResourceCollection(SecurityResourceCollection collection){
    	m_securityResourceCollections.add(collection);
    }

    /**
     * Get servlet load on startup status
     * @return
     */
    public boolean isServletLoadOnStartup() {
    	return m_servletLoadOnStartup>=0 ? true:false;

	}
    
    /**
     * Get servlet's load on startup value.
     * 
     * @return
     */
    public int getServletLoadOnStartup() {
    	return m_servletLoadOnStartup;
    }
    
    /**
     * Setter for load on startup.
     * 
     * @param servletLoadOnStartup the value to set.
     */
    public void setServletLoadOnStartup(int servletLoadOnStartup) {
    	// if web.xml has load-on-startup tag and sip.xml/annotations does not have.
    	if (!m_hasLoadOnStartupTag) {
    		m_hasLoadOnStartupTag = true;
    		m_servletLoadOnStartup = servletLoadOnStartup;
    	}
    }
    
    /**
     * Gets the servlet's initial parameters.
     * @return
     */
    public Map<String,String> getInitParams() {
    	return m_initParams;
    }
    
    /**
     * Sets initial parameter.
     * 
     * @param paramName the initial parameter's name
     * @param paramValue the initial parameter's value
     */
    public void setInitParam(String paramName, String paramValue) {
    	m_initParams.put(paramName, paramValue);
    }

    /**
     * Get the security resource collections
     * @return
     */
    public List getSecurityResourceCollections(){
    	return m_securityResourceCollections;
    }

    /**
     * Getter for isMainServlet
     * @return
     */
	public boolean isMainServlet() {
		return isMainServlet;
	}

	/**
	 * Setter for isMainServlet 
	 * @param isMainServlet
	 */
	public void setMainServlet(boolean isMainServlet) {
		this.isMainServlet = isMainServlet;
	}
    
    
}
