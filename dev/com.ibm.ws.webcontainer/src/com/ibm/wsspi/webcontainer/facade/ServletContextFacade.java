/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.facade;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.ibm.websphere.servlet.container.WebContainer;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.webcontainer.async.AsyncRequestDispatcher;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.WebAppFilterManager;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * @author asisin
 *
 * Facade wrapping the WebApp when returning a context to the user. This will 
 * prevent users from exploiting public methods in WebApp which were intended
 * for internal use only.
 */
@SuppressWarnings("unchecked")
public class ServletContextFacade implements IServletContextExtended
{
	protected IServletContext context;
	
	public ServletContextFacade(IServletContext context)
	{
		this.context = context;
	}
	
	/**
	 * Gets the IFilterConfig object for this context or creates
     * one if it doesn't exist.
	 * @param id
	 * @return
	 */
        public com.ibm.websphere.servlet.filter.IFilterConfig getFilterConfig(String id){
        	return context.getFilterConfig(id);
        }
    
        /**
         * Adds a filter against a specified mapping into this context
         * @param mapping
         * @param config
         */
        public void addMappingFilter(String mapping, com.ibm.websphere.servlet.filter.IFilterConfig config){
        	context.addMappingFilter(mapping, config);
        }


     public IServletContext getIServletContext () {
          return this.context;
     }
     
	/**
	 * @see javax.servlet.ServletContext#getContext(String)
	 */
	public ServletContext getContext(String arg0)
	{
		return context.getContext(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getMajorVersion()
	 */
	public int getMajorVersion()
	{
		return context.getMajorVersion();
	}

	/**
	 * @see javax.servlet.ServletContext#getMinorVersion()
	 */
	public int getMinorVersion()
	{
		return context.getMinorVersion();
	}

	/**
	 * @see javax.servlet.ServletContext#getMimeType(String)
	 */
	public String getMimeType(String arg0)
	{
		return context.getMimeType(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getResourcePaths(String)
	 */
	public Set getResourcePaths(String arg0)
	{
		return context.getResourcePaths(arg0);
	}
	
	public Set getResourcePaths(String arg0,boolean searchMetaInfResources)
	{
		return context.getResourcePaths(arg0,searchMetaInfResources);
	}


	/**
	 * @see javax.servlet.ServletContext#getResource(String)
	 */
	public URL getResource(String arg0) throws MalformedURLException
	{
		return context.getResource(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getResourceAsStream(String)
	 */
	public InputStream getResourceAsStream(String arg0)
	{
		return context.getResourceAsStream(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getRequestDispatcher(String)
	 */
	public RequestDispatcher getRequestDispatcher(String arg0)
	{
		return context.getRequestDispatcher(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getNamedDispatcher(String)
	 */
	public RequestDispatcher getNamedDispatcher(String arg0)
	{
		return context.getNamedDispatcher(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getServlet(String)
	 * @deprecated
	 */
	public Servlet getServlet(String arg0) throws ServletException
	{
		return context.getServlet(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getServlets()
	 * @deprecated
	 */
	public Enumeration getServlets()
	{
		return context.getServlets();
	}

	/**
	 * @see javax.servlet.ServletContext#getServletNames()
	 * @deprecated
	 */
	public Enumeration getServletNames()
	{
		return context.getServletNames();
	}

	/**
	 * @see javax.servlet.ServletContext#log(String)
	 */
	public void log(String arg0)
	{
		context.log(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#log(Exception, String)
	 * @deprecated
	 */
	public void log(Exception arg0, String arg1)
	{
		context.log(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#log(String, Throwable)
	 */
	public void log(String arg0, Throwable arg1)
	{
		context.log(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#getRealPath(String)
	 */
	public String getRealPath(String arg0)
	{
		return context.getRealPath(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getServerInfo()
	 */
	public String getServerInfo()
	{
		return context.getServerInfo();
	}

	/**
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 */
	public String getInitParameter(String arg0)
	{
		return context.getInitParameter(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 */
	public Enumeration getInitParameterNames()
	{
		return context.getInitParameterNames();
	}

	/**
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	public Object getAttribute(String arg0)
	{
		return context.getAttribute(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 */
	public Enumeration getAttributeNames()
	{
		return context.getAttributeNames();
	}

	/**
	 * @see javax.servlet.ServletContext#setAttribute(String, Object)
	 */
	public void setAttribute(String arg0, Object arg1)
	{
		context.setAttribute(arg0, arg1);
	}

	/**
	 * @see javax.servlet.ServletContext#removeAttribute(String)
	 */
	public void removeAttribute(String arg0)
	{
		context.removeAttribute(arg0);
	}

	/**
	 * @see javax.servlet.ServletContext#getServletContextName()
	 */
	public String getServletContextName()
	{
		return context.getServletContextName();
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#addDynamicServlet(java.lang.String, java.lang.String, java.lang.String, java.util.Properties)
	 */
	public void addDynamicServlet(
		String servletName,
		String servletClass,
		String mappingURI,
		Properties initParameters)
		throws ServletException, SecurityException
	{
		context.addDynamicServlet(servletName, servletClass, mappingURI, initParameters);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#addHttpSessionListener(javax.servlet.http.HttpSessionListener)
	 */
	public void addHttpSessionListener(HttpSessionListener listener) throws SecurityException
	{
		context.addHttpSessionListener(listener);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#fireSessionAttributeAdded(javax.servlet.http.HttpSessionBindingEvent)
	 */
	public void fireSessionAttributeAdded(HttpSessionBindingEvent event)
	{
		context.fireSessionAttributeAdded(event);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#fireSessionAttributeRemoved(javax.servlet.http.HttpSessionBindingEvent)
	 */
	public void fireSessionAttributeRemoved(HttpSessionBindingEvent event)
	{
		context.fireSessionAttributeRemoved(event);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#fireSessionAttributeReplaced(javax.servlet.http.HttpSessionBindingEvent)
	 */
	public void fireSessionAttributeReplaced(HttpSessionBindingEvent event)
	{
		context.fireSessionAttributeReplaced(event);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#fireSessionCreated(javax.servlet.http.HttpSessionEvent)
	 */
	public void fireSessionCreated(HttpSessionEvent event)
	{
		context.fireSessionCreated(event);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#fireSessionDestroyed(javax.servlet.http.HttpSessionEvent)
	 */
	public void fireSessionDestroyed(HttpSessionEvent event)
	{
		context.fireSessionDestroyed(event);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#getSessionTimeout()
	 */
	public int getSessionTimeout()
	{
		return context.getSessionTimeout();
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#isSessionTimeoutSet()
	 */
	public boolean isSessionTimeoutSet()
	{
		return context.isSessionTimeoutSet();
	}

	/**
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#loadServlet(java.lang.String)
	 * @deprecated
	 */
	public void loadServlet(String servletName) throws ServletException, SecurityException
	{
		context.loadServlet(servletName);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.context.IBMServletContext#removeDynamicServlet(java.lang.String)
	 */
	public void removeDynamicServlet(String servletName) throws SecurityException
	{
		context.removeDynamicServlet(servletName);
	}


	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#addLifecycleListener(java.util.EventListener)
	 */
	public void addLifecycleListener(EventListener eventListener)
	{
		context.addLifecycleListener(eventListener);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#createServletWrapper(com.ibm.ws.webcontainer.servlet.ServletConfig)
	 */
	public IServletWrapper createServletWrapper(IServletConfig sconfig) throws Exception
	{
		return context.createServletWrapper(sconfig);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#removeLifeCycleListener(java.util.EventListener)
	 */
	public void removeLifeCycleListener(EventListener eventListener)
	{
		context.removeLifeCycleListener(eventListener);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#getContextPath()
	 */
	public String getContextPath()
	{
		//PM47487
		String cp = context.getContextPath();
		if (WCCustomProperties.RETURN_DEFAULT_CONTEXT_PATH && cp.equals("/")){
			cp = "";
		}
		//PM47487
		return cp;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#targets()
	 */
	public Iterator targets()
	{
		return context.targets();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#addMappingTarget(java.lang.String, com.ibm.ws.webcontainer.core.RequestProcessor)
	 */
	public void addMappingTarget(String mapping, RequestProcessor target) throws Exception
	{
		context.addMappingTarget(mapping, target);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#getMappingTarget(java.lang.String)
	 */
	public RequestProcessor getMappingTarget(String mapping)
	{
		return context.getMappingTarget(mapping);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#getWebAppConfig()
	 */
	public WebAppConfig getWebAppConfig()
	{
		return context.getWebAppConfig();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#sendError(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.ibm.websphere.servlet.error.ServletErrorReport)
	 */
	public void sendError(HttpServletRequest request, HttpServletResponse response, ServletErrorReport e)
	{
		context.sendError(request, response, e);
	}

	public AsyncRequestDispatcher getAsyncRequestDispatcher(String path) {
		return context.getAsyncRequestDispatcher(path);
	}
	
	public void addFeature(WebContainer.Feature feature) {
		context.addFeature(feature);
	}
	
	public boolean isFeatureEnabled(WebContainer.Feature feature){
		return context.isFeatureEnabled(feature);
	}

	public void addMappingFilter(String mapping, IFilterConfig config) {
		context.addMappingFilter(mapping, config);
	}

	public void addMappingFilter(IServletConfig sConfig, IFilterConfig config) {
		context.addMappingFilter(sConfig, config);
	}

	public IFilterConfig createFilterConfig(String id) {
		return context.createFilterConfig(id);
	}

	public void finishEnvSetup(boolean transactional) throws Exception {
		context.finishEnvSetup(transactional);
	}

	public ClassLoader getClassLoader() {
		return context.getClassLoader();
	}

	public ICollaboratorHelper getCollaboratorHelper() {
		return ((IServletContextExtended) context).getCollaboratorHelper();
	}

	public ServletContext getFacade() {
		return context.getFacade();
	}

	public WebAppFilterManager getFilterManager() {
		return context.getFilterManager();
	}

	public boolean isFiltersDefined() {
		return context.isFiltersDefined();
	}

	public void replaceMappingTarget(String mapping, RequestProcessor target) throws Exception {
		context.replaceMappingTarget(mapping, target);
	}

	public void startEnvSetup(boolean transactional) throws Exception {
		context.startEnvSetup(transactional);
	}

	public String getCommonTempDirectory() {
		return context.getCommonTempDirectory();
	}

	public boolean isCachingEnabled() {
		return context.isCachingEnabled();
		
	}

	public IHttpSessionContext getSessionContext() {
		return ((IServletContextExtended) context).getSessionContext();
	}

	public IInvocationCollaborator[] getWebAppInvocationCollaborators() {
		return context.getWebAppInvocationCollaborators();
	}

	public Dynamic addFilter(String filterName,
			Class<? extends Filter> filterClass) throws IllegalStateException {
		return context.addFilter(filterName, filterClass);
	}

	public Dynamic addFilter(String filterName, Filter filter)
			throws IllegalStateException {
		return context.addFilter(filterName, filter);
	}

	public Dynamic addFilter(String filterName, String className)
			throws IllegalStateException {
		return context.addFilter(filterName, className);
	}

	public javax.servlet.ServletRegistration.Dynamic addServlet(
			String servletName, Class<? extends Servlet> servletClass) {
		return context.addServlet(servletName, servletClass);
	}

	public javax.servlet.ServletRegistration.Dynamic addServlet(
			String servletName, Servlet servlet) {
		return context.addServlet(servletName, servlet);
	}

	public javax.servlet.ServletRegistration.Dynamic addServlet(
			String servletName, String className) {
		return context.addServlet(servletName, className);
	}

	public <T extends Filter> T createFilter(Class<T> c)
			throws ServletException {
		return context.createFilter(c);
	}

	public <T extends Servlet> T createServlet(Class<T> c)
			throws ServletException {
		return context.createServlet(c);
	}

	public FilterRegistration getFilterRegistration(String filterName) {
		return context.getFilterRegistration(filterName);
	}

	public ServletRegistration getServletRegistration(String servletName) {
		return context.getServletRegistration(servletName);
	}

	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return context.getDefaultSessionTrackingModes();
	}

	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return context.getEffectiveSessionTrackingModes();
	}

	public SessionCookieConfig getSessionCookieConfig() {
		return context.getSessionCookieConfig();
	}

	public boolean setInitParameter(String name, String value)
			throws IllegalStateException, IllegalArgumentException {
		return context.setInitParameter(name, value);
	}

	public void setSessionTrackingModes(
			Set<SessionTrackingMode> sessionTrackingModes)
			throws IllegalStateException {
		context.setSessionTrackingModes(sessionTrackingModes);
	}

	
	public boolean containsTargetMapping(String mapping) {
		return context.containsTargetMapping(mapping);
	}

	
	public void addToStartWeightList(IServletConfig sc) {
		context.addToStartWeightList(sc);
	}
	
	public boolean isInitialized(){
	    return context.isInitialized();
	}

    public void addListener(Class<? extends EventListener> arg0) {
        context.addListener(arg0);
    }

    
    public void addListener(String arg0) {
        context.addListener(arg0);
    }

    public <T extends EventListener> void addListener(T arg0) {
        context.addListener(arg0);
    }

   
    public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
        return context.createListener(arg0);
    }

    
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return context.getFilterRegistrations();
    }

    
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return context.getServletRegistrations();
    }

    /* No longer used ... have to use getSessionCookieConfig and set the values on that
    public void setSessionCookieConfig(SessionCookieConfig arg0) {
        context.setSessionCookieConfig(arg0);
    }*/

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return context.getJspConfigDescriptor();
    }

    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {

        return context.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {

        return context.getEffectiveMinorVersion();
    }

	@Override
	public void declareRoles(String... arg0) {
		context.declareRoles(arg0);
	}

	@Override
	public Map<String, ? extends javax.servlet.ServletRegistration.Dynamic> getDynamicServletRegistrations() {
		return context.getDynamicServletRegistrations();
	}

	@Override
	public WebComponentMetaData getWebAppCmd() {
		return context.getWebAppCmd();
	}

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#getModuleContainer()
     */
    @Override
    public Container getModuleContainer() {
        //should not be called on the Facade object
        return null;
    }

}
