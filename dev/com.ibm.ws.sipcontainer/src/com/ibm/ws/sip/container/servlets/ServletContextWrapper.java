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
package com.ibm.ws.sip.container.servlets;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;

/**
 * @author Amir Perlman, Jan 2, 2004
 * 
 * Wraps the actuals implementation of the Servlet Context in order to replace
 * Sip Request/Response into Http Request/Response that are familiar to the
 * wrapped implemention,e.g Websphere
 */
public class ServletContextWrapper implements ServletContext
{
    /**
     * The actual implementation of the Servlet Context
     */
    private ServletContext m_impl;
    

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(ServletContextWrapper.class);

    /**
     * Construct a Servlet Context Wrapper for the specified implemention of
     * Servlet Context.
     * 
     * @param impl
     */
    public ServletContextWrapper(ServletContext impl)
    {
        m_impl = impl;
    }


    /**
     * @see javax.servlet.ServletContext#addFilter(java.lang.String, java.lang.Class)
     */
    public Dynamic addFilter(String arg0, Class<? extends Filter> arg1)
			throws IllegalArgumentException, IllegalStateException {
		return m_impl.addFilter(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#addFilter(java.lang.String, javax.servlet.Filter)
	 */
	public Dynamic addFilter(String arg0, Filter arg1)
			throws IllegalArgumentException, IllegalStateException {
		return m_impl.addFilter(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#addFilter(java.lang.String, java.lang.String)
	 */
	public Dynamic addFilter(String arg0, String arg1)
			throws IllegalArgumentException, IllegalStateException {
		return m_impl.addFilter(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#addListener(java.lang.Class)
	 */
	public void addListener(Class<? extends EventListener> arg0) {
		m_impl.addListener(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#addListener(java.lang.String)
	 */
	public void addListener(String arg0) {
		m_impl.addListener(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#addListener(T)
	 */
	public <T extends EventListener> void addListener(T arg0) {
		m_impl.addListener(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#addServlet(java.lang.String, java.lang.Class)
	 */
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Class<? extends Servlet> arg1) throws IllegalArgumentException,
			IllegalStateException {
		return m_impl.addServlet(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#addServlet(java.lang.String, javax.servlet.Servlet)
	 */
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Servlet arg1) throws IllegalArgumentException,
			IllegalStateException {
		return m_impl.addServlet(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#addServlet(java.lang.String, java.lang.String)
	 */
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			String arg1) throws IllegalArgumentException, IllegalStateException {
		return m_impl.addServlet(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#createFilter(java.lang.Class)
	 */
	public <T extends Filter> T createFilter(Class<T> arg0)
			throws ServletException {
		return m_impl.createFilter(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#createListener(java.lang.Class)
	 */
	public <T extends EventListener> T createListener(Class<T> arg0)
			throws ServletException {
		return m_impl.createListener(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#createServlet(java.lang.Class)
	 */
	public <T extends Servlet> T createServlet(Class<T> arg0)
			throws ServletException {
		return m_impl.createServlet(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#declareRoles(java.lang.String[])
	 */
	public void declareRoles(String... arg0) {
		m_impl.declareRoles(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getClassLoader()
	 */
	public ClassLoader getClassLoader() {
		return m_impl.getClassLoader();
	}

	/**
	 * @see javax.servlet.ServletContext#getDefaultSessionTrackingModes()
	 */
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return m_impl.getDefaultSessionTrackingModes();
	}

	/**
	 * @see javax.servlet.ServletContext#getEffectiveMajorVersion()
	 */
	public int getEffectiveMajorVersion() throws UnsupportedOperationException {
		return m_impl.getEffectiveMajorVersion();
	}

	/**
	 * @see javax.servlet.ServletContext#getEffectiveMinorVersion()
	 */
	public int getEffectiveMinorVersion() throws UnsupportedOperationException {
		return m_impl.getEffectiveMinorVersion();
	}

	/**
	 * @see javax.servlet.ServletContext#getEffectiveSessionTrackingModes()
	 */
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return m_impl.getEffectiveSessionTrackingModes();
	}

	/**
	 * @see javax.servlet.ServletContext#getFilterRegistration(java.lang.String)
	 */
	public FilterRegistration getFilterRegistration(String arg0) {
		return m_impl.getFilterRegistration(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getFilterRegistrations()
	 */
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return m_impl.getFilterRegistrations();
	}

	/**
	 * @see javax.servlet.ServletContext#getJspConfigDescriptor()
	 */
	public JspConfigDescriptor getJspConfigDescriptor() {
		return m_impl.getJspConfigDescriptor();
	}

	/**
	 * @see javax.servlet.ServletContext#getServletRegistration(java.lang.String)
	 */
	public ServletRegistration getServletRegistration(String arg0) {
		return m_impl.getServletRegistration(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getServletRegistrations()
	 */
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return m_impl.getServletRegistrations();
	}

	/**
	 * @see javax.servlet.ServletContext#getSessionCookieConfig()
	 */
	public SessionCookieConfig getSessionCookieConfig() {
		return m_impl.getSessionCookieConfig();
	}

	/**
	 * @see javax.servlet.ServletContext#setInitParameter(java.lang.String, java.lang.String)
	 */
	public boolean setInitParameter(String arg0, String arg1) {
		return m_impl.setInitParameter(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#setSessionTrackingModes(java.util.Set)
	 */
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
		m_impl.setSessionTrackingModes(arg0);
	}

    /**
     * @see javax.servlet.ServletContext#getContext(java.lang.String)
     */
    public ServletContext getContext(String arg0)
    {
        //JSR 116 Section 4.3
        //As SIP URIs do not have a notion of paths, the following
        // ServletContext methods have no meaning for SIP-only servlet
        // applications containers and must return null:
        //		    ServletContext getContext(String uripath);
        //			String getRealPath(String path);
        //			RequestDispatcher getRequestDispatcher(String path);
       
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion()
    {
        return m_impl.getMajorVersion();
    }

    /**
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion()
    {
    	//Current version of JSR 116 is based on SIP Servlet specification 2.3
    	//and not the 2.4 version which is supported by the Web Container in WAS 6.1 
        return 3;
    }

    /**
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String arg0)
    {
        return m_impl.getMimeType(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    public Set getResourcePaths(String arg0)
    {
        return m_impl.getResourcePaths(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    public URL getResource(String arg0) throws MalformedURLException
    {
        return m_impl.getResource(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String arg0)
    {
        return m_impl.getResourceAsStream(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String arg0)
    {
        //JSR 116 Section 4.3
        //As SIP URIs do not have a notion of paths, the following
        // ServletContext methods have no meaning for SIP-only servlet
        // applications containers and must return null:
        //		    ServletContext getContext(String uripath);
        //			String getRealPath(String path);
        //			RequestDispatcher getRequestDispatcher(String path);
       
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    public RequestDispatcher getNamedDispatcher(String servletName)
    {
    	RequestDispatcher rd = m_impl.getNamedDispatcher(servletName);
        return ( rd == null) ? null :  new RequestDispatcherWrapper(rd,servletName);
    }

    /**
     * @see javax.servlet.ServletContext#getServlet(java.lang.String)
     * @deprecated
     */
    public Servlet getServlet(String arg0) throws ServletException
    {
        return m_impl.getServlet(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getServlets()
     * @deprecated
     */
    public Enumeration getServlets()
    {
        return m_impl.getServlets();
    }

    /**
     * @see javax.servlet.ServletContext#getServletNames()
     * @deprecated
     */
    public Enumeration getServletNames()
    {
        return m_impl.getServletNames();
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.String)
     */
    public void log(String arg0)
    {
        m_impl.log(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.Exception,
     *      java.lang.String)
     * @deprecated
     */
    public void log(Exception arg0, String arg1)
    {
        m_impl.log(arg0, arg1);
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.String,
     *      java.lang.Throwable)
     */
    public void log(String arg0, Throwable arg1)
    {
        m_impl.log(arg0, arg1);
    }

    /**
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String arg0)
    {
        //JSR 116 Section 4.3
        //As SIP URIs do not have a notion of paths, the following
        // ServletContext methods have no meaning for SIP-only servlet
        // applications containers and must return null:
        //		    ServletContext getContext(String uripath);
        //			String getRealPath(String path);
        //			RequestDispatcher getRequestDispatcher(String path);
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo()
    {
        return m_impl.getServerInfo();
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String arg0)
    {
        return m_impl.getInitParameter(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames()
    {
        return m_impl.getInitParameterNames();
    }

    /**
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String arg0)
    {
    	if (SipServlet.APP_NAME_ATTRIBUTE.equals(arg0)) {
    		return m_impl.getServletContextName();
    	}
    	
       	if (arg0.equals(SipServlet.OUTBOUND_INTERFACES) == true)
    	{
			if (c_logger.isTraceDebugEnabled())
			{
			     c_logger.traceDebug(
			        this,
			        "getAttribute",
			     	"Looking for OutboundInterface list");
			}

			List<SipURI> outboundIfList = SipProxyInfo.getInstance().getOutboundInterfaceList();
			if (outboundIfList != null)
			{
				if (c_logger.isTraceDebugEnabled())
				{
				     c_logger.traceDebug(
				        this,
				        "getAttribute",
				     	"proxyInfo contains OutboundInterface list: " + Arrays.toString(outboundIfList.toArray()));
				     
				}
				
				return outboundIfList;
			}
			else
			{
				if (c_logger.isTraceDebugEnabled())
				{
				     c_logger.traceDebug(
				        this,
				        "getAttribute",
				     	"proxyInfo does not contain OutboundInterface list");
				}
			}
    	}

        return m_impl.getAttribute(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        return m_impl.getAttributeNames();
    }

   
    /**
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    public void setAttribute(String arg0, Object arg1)
    {
    	// BLP: Not sure I understand why Anat is returning here when the outbound interface attribute
    	//	is being set.
        if(arg0.equalsIgnoreCase(SipServlet.SIP_FACTORY)||
                arg0.equalsIgnoreCase(SipServlet.SUPPORTED)||
                arg0.equalsIgnoreCase(SipServlet.TIMER_SERVICE)||
                arg0.equalsIgnoreCase(ServletConfigWrapper.ATTRIBUTE_100_REL)||
                arg0.equalsIgnoreCase(SipServlet.SIP_SESSIONS_UTIL)||
                arg0.equalsIgnoreCase(SipServlet.OUTBOUND_INTERFACES) ||
                arg0.equalsIgnoreCase(SipServlet.APP_NAME_ATTRIBUTE) ||
                arg0.equalsIgnoreCase(SipServlet.DOMAIN_RESOLVER_ATTRIBUTE) ){
            return;
        }
        m_impl.setAttribute(arg0, arg1);
    }

    /**
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String arg0)
    {
        m_impl.removeAttribute(arg0);
    }

    /**
     * @see javax.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName()
    {
    	SipAppDesc appDesc = SipContainer.getInstance().getSipApp(m_impl.getServletContextName());
    	if (appDesc != null) {
    		return appDesc.getDisplayName();
    	}    		
        return m_impl.getServletContextName();
    }

    /**
     * @see javax.servlet.ServletContext#getContextPath()
     */
    public String getContextPath()
    {
        return m_impl.getContextPath();
    }
}
