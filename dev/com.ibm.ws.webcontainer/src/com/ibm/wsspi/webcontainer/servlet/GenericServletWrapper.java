/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 
 * A convenience class that implements IServletWrapper. This class must be 
 * subclassed if IServletWrapper implementations want to leverage the webcontainer's
 * handling of the following:
 * 		-> Servlet lifecycle management
 * 		-> Event generation and firing
 * 		-> Request handling leveraging sessions and security
 *  
 * Subclasses usually override the handleRequest() method wherein they implement
 * logic specific to the container they are part of, and at the end call on
 * super.handleRequest() which then handles the actual dispatch to the servlet
 * and invokes the Servlet's service() method.
 * 
 * @since WAS6.0
 * @ibm-private-in-use
 */
public abstract class GenericServletWrapper implements IServletWrapper
{
	/**
	 * The IServletContext interface thats exposed to the subclasses of this class
	 */
	protected IServletContext context;
	
	/**
	 * The IServletConfig interface associated with this IServletWrapper instance
	 */
	protected IServletConfig servletConfig;
	
	private IServletWrapper wrapper;

	/**
	 * Public constructor. This contructor must be invoked from within the 
	 * constructor of the subclass passing in the IServletContext so that the
	 * parent object can be constructed correctly.
	 * 
	 * @param parent The IServletContext that this IServletWrapper will be a part of
	 */
	public GenericServletWrapper(IServletContext parent) throws Exception
	{
		context = parent;
	    wrapper = parent.createServletWrapper(null);
	}

	/**
	 * Method that handles the initialization of this IServletWrapper instance.
	 * Subclasses must call this by invoking super.initialize(config), so that
	 * the underlying target Servlet can be setup and initialized by calling its
	 * init() method (if specified in the config as loadAtStartUp).
	 * 
	 * @param config the IServletConfig associated with this IServletWrapper
	 */
	public void initialize(IServletConfig config) throws Exception
	{
		this.servletConfig = config;
		wrapper.initialize(config);
	}

	/**
	 * Method that processes the request, and ultimately invokes the service() on the
	 * Servlet target. Subclasses may override this method to put in additional 
	 * logic (eg., reload/retranslation logic in the case of JSP containers). Subclasses
	 * must call this method by invoking super.handleRequest() if they want 
	 * the webcontainer to handle initialization and servicing of the target in 
	 * a proper way.
	 * 
	 * An example scenario:
	 * 
	 * <tt>
	 * class SimpleJSPServletWrapper extends GenericServletWrapper
	 * {
	 * ...
	 * 		public void handleRequest(ServletRequest req, ServletResponse res)
	 * 		{
	 * 			String jspFile = getFileFromRequest(req); // get the JSP target
	 * 			if (hasFileChangedOnDisk(jspFile))
	 * 			{
	 * 				prepareForReload();
	 * 				
	 * 				// invalidate the target and targetClassLoader
	 * 				setTargetClassLoader(null);
	 * 				setTarget(null);
	 * 				JSPServlet jsp = compileJSP(jspFile);
	 * 				ClassLoader loader = getLoaderForServlet(jsp);
	 * 				setTarget(jsp.getClassName());
	 * 				setTargetClassLoader(loader);
	 * 			}
	 * 
	 * 			super.handleRequest(req, res);
	 * 		}
	 * ...
	 * }
	 * </tt>
	 * 				
	 */
	public void handleRequest(ServletRequest req, ServletResponse res) throws Exception
	{
		wrapper.handleRequest(req, res);
	}
	
	/**
	 * Gracefully invalidates the target by overseeing its lifecycle (destroy())
	 * This method must be called before the target is invalidated for reload.
	 * 
	 */
	public void prepareForReload()
	{
		wrapper.prepareForReload();
	}

	/**
	 * Returns the servlet name of the target
	 */
	public String getServletName()
	{
		return wrapper.getServletName();
	}

	/**
	 * Returns the ServletConfig associated with the target
	 */
	public IServletConfig getServletConfig()
	{
		return wrapper.getServletConfig();
	}
	
	// PK27620
	
	public ServletContext getServletContext () {
		return this.context;
	}
	
	/**
	 * Instructs the underlying implementation to use the supplied class loader
	 * to instantiate the target instance. Calling this method with a null, 
	 * accompanied with a setTarget(null) will result in the current classloader
	 * being destroyed and garbage collected along with the target instance. 
	 */
	public void setTargetClassLoader(ClassLoader loader)
	{
		wrapper.setTargetClassLoader(loader);
	}

	/**
	 * Returns the target Servlet instance
	 */
	public Servlet getTarget()
	{
		return wrapper.getTarget();
	}

	/**
	 * Returns the current classloader which loaded (or will, in the future, load) 
	 * the target.
	 */
	public ClassLoader getTargetClassLoader()
	{
		return wrapper.getTargetClassLoader();
	}

        // PK27620

	protected IServletWrapper getWrapper () {
	     return this.wrapper;
	}

	/**
	 * Sets the target for this IServletWrapper.
	 */
	public void setTarget(Servlet target)
	{
		wrapper.setTarget(target);
	}

	/**
	 * Add a listener which will listen to the invalidation events for this
	 * wrapper instance. The invalidate() event will be fired only when the 
	 * wrapper itself (not the target) is about to be destroyed by the parent
	 * container.
	 */
	public void addServletReferenceListener(ServletReferenceListener listener)
	{
		wrapper.addServletReferenceListener(listener);
	}

	/**
	 * This method will be called by the webcontainer's reaper mechanism which
	 * polls for the access times of the wrappers in its datastructures, and 
	 * invalidates them if they have been inactive for a preconfigured amount of
	 * time. Wrapper subclasses that do not want to be 'reaped' may override this
	 * method by returning the current system time.
	 */
    public long getLastAccessTime() {
        return wrapper.getLastAccessTime();
    }
    
    /**
     * This method will be invoked when the parent container wishes to destroy 
     * this IServletWrapper instance. Thew subclasses may override this method
     * to implement any resource clean up, but must invoke this method by calling
     * super.destroy() inorder to correctly destroy the underlying target.
     */
    public void destroy() {
        wrapper.destroy();
    }
    
    public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        wrapper.service(request, response);
    }

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletWrapper#setParent(com.ibm.wsspi.webcontainer.servlet.IServletContext)
	 */
	public void setParent(IServletContext parent)
	{
		wrapper.setParent(parent);
	}

	// begin 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer    
	/**
	 * Returns whether the requested wrapper resource exists.
	 */
	public boolean isAvailable (){
		return true;
	}
	// end 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer    

	// Begin PK27620
	
	public void nameSpacePostInvoke () {
	}
	
	public void nameSpacePreInvoke () {
	}
	
	// End PK27620
	
    public String getName(){
    	return "GenericServletWrapper";
    }
    
    public boolean isInternal(){
    	return false;
    }
    
    public void loadOnStartupCheck() throws Exception{
    	wrapper.loadOnStartupCheck();
    }
    
    public void load() throws Exception{
    	wrapper.load();
    }
    
    //modifies the target for SingleThreadModel & caching
    public void modifyTarget(Servlet s) {
        //do nothing here
    }
}
