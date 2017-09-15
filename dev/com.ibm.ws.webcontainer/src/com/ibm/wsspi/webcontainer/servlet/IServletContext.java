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
package com.ibm.wsspi.webcontainer.servlet;

import java.util.EventListener;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;

import com.ibm.websphere.servlet.container.WebContainer;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.WebAppFilterManager;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * 
 * This is an extension to IBMServletContext which can be used to
 * 
 * @ibm-private-in-use
 */
public interface IServletContext extends com.ibm.websphere.servlet.context.IBMServletContext {
   
	/**
	 * Adds a lifecycle event listener to this context. The listner can be any
	 * of the standard listeners mandated by the Servlet 2.4 specification.
	 * 
	 * @param eventListener
	 */
	public void addLifecycleListener(EventListener eventListener);
	
	/**
	 * Removes an existing listener from this context's list of listeners.
	 * 
	 * NOTE: Listeners of the type HttpSessionListener && HttpSessionIdListener currently have no way of 
	 * being removed.
	 * @param eventListener
	 */
	public void removeLifeCycleListener(EventListener eventListener);
	
	/**
	 * Returns the configuration object associated with this context
	 * @return
	 */
	public WebAppConfig getWebAppConfig();
	
	/**
	 * Returns the web app filter manager associated with this context.
	 * 
	 * @return
	 */
	
	public WebAppFilterManager getFilterManager ();
	
	/**
	 * Returns whether or not filters are defined with this context.
	 * @return
	 */
	
	public boolean isFiltersDefined ();
	
	/**
	 * Convenience method that creates an IServletWrapper given the servlet config.
	 * 
	 * @param sconfig
	 * @return
	 * @throws Exception
	 */
	public IServletWrapper createServletWrapper(IServletConfig sconfig) throws Exception;
	
	/**
	 * Returns an iterator of all the targets currently loaded (not necessarily
	 * initialized) by this context. The target objects are all RequestProcessors,
	 * and may be either IServletWrappers or ExtensionProcessors
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Iterator targets();
	
	
	/**
	 * Returns the target that matches (according to the rules under servlet
	 * mappings in the servlet spec.) the given mapping.
	 * @param mapping
	 * @return
	 */
	public RequestProcessor getMappingTarget(String mapping);
	
	/**
	 * Adds a target against a specified mapping into this context
	 * @param mapping
	 * @param target
	 * @throws Exception
	 */
	public void addMappingTarget(String mapping, RequestProcessor target) throws Exception;
	
	public boolean containsTargetMapping(String mapping);
	
	/**
	 * Replaces the target for the specified mapping in this context
	 * @param mapping
	 * @param target
	 * @throws Exception
	 */
	public void replaceMappingTarget(String mapping, RequestProcessor target) throws Exception;
	
	/**
	 * Creates a IFilterConfig object for this context
	 * @param id
	 * @return
	 */
        public IFilterConfig createFilterConfig(String id);
    
        /**
         * Adds a filter against a specified mapping into this context
         * @param mapping
         * @param config
         */
        public void addMappingFilter(String mapping, IFilterConfig config);

        /**
         * Adds a filter against a specified servlet config into this context
         * @param sConfig
         * @param config
         */
        public void addMappingFilter(IServletConfig sConfig, IFilterConfig config);

	
	/**
	 * Returns the classLoader that this context used to load its resources
	 * @return
	 */
	public ClassLoader getClassLoader();
	
	/** 
	 * Called by components leveraging the webcontainer to set up the environments necessary
	 * (1) Namespace - to enable namespace lookups
	 * (2) Setup the classloader on the thread to be that of the WebApp.
	 * (3) Depending on the <i>transactional</i> parameter passed, setup transaction related environment
	 * @param transactional
	 */
	public void startEnvSetup(boolean transactional) throws Exception;
	
	/** 
	 * Called by components leveraging the webcontainer to tear down the environment that was setup
	 * by a previous call to startEnvSetup();
	 * (1) Namespace - to enable namespace lookups
	 * (2) Setup the classloader on the thread to be that of the WebApp.
	 * (3) Depending on the <i>transactional</i> parameter passed, setup transaction related environment
	 * @param transactional
	 */
	public void finishEnvSetup(boolean transactional) throws Exception;
	

    //  begin defect 293789: add ability for components to register ServletContextFactories
	/**
	 *  Used to indicate that a Feature is enabled for this context.
	 * @param feature
	 */
	public void addFeature(WebContainer.Feature feature);
    //  end defect 293789: add ability for components to register ServletContextFactories
	
	public boolean isFeatureEnabled(WebContainer.Feature feature);
    
    
    /**
     *  Called by components utilizing IServletContext (ie session) that provide access to a ServletContext object to applications.
     *  Since IServletContext provides access to webcontainer internals, components are allowed to only expose
     *  the facade object to applications and not the enhanced WebContainer ServletContext implementation IServletContext.
     * @return
     */
    public ServletContext getFacade();

    // RTC 160610. Moving this method to IServletContextExtended because
    // it should not be SPI.
    //public ICollaboratorHelper getCollaboratorHelper();
	
    //Begin PK31450
	public String getCommonTempDirectory ();
	//End PK31450
	
	public boolean isCachingEnabled();

	public IInvocationCollaborator[] getWebAppInvocationCollaborators();
	
	// RTC 160610. Moving this method to IServletContextExtended because
	// it should not be SPI.
	//public IHttpSessionContext getSessionContext();
	
	public void addToStartWeightList(IServletConfig sc);

    public boolean isInitialized();
    
    public abstract com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData getWebAppCmd();
    
    public Set getResourcePaths(String path, boolean searchMetaInf);
    
    public Container getModuleContainer();
}
