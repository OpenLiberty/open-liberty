/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer31.osgi.webapp;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpUpgradeHandler;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.http.channel.h2internal.H2UpgradeHandler;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.servlet.H2Handler;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.webcontainer31.facade.ServletContextFacade31;
import com.ibm.ws.webcontainer31.osgi.listener.RegisterEventListenerProvider;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.session.IHttpSessionContext31;
import com.ibm.ws.webcontainer31.upgrade.H2HandlerImpl;
import com.ibm.ws.webcontainer31.upgrade.H2UpgradeHandlerWrapper;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.ReferenceContext;


/**
 */
public class WebApp31 extends com.ibm.ws.webcontainer.osgi.webapp.WebApp
{
    private final static TraceComponent tc = Tr.register(WebApp31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer31.osgi.webapp.WebApp31";
    
    private static final TraceNLS servlet31NLS = TraceNLS.getTraceNLS(WebApp31.class, "com.ibm.ws.webcontainer31.resources.Messages");
    
    private static final Class[] validListenerClasses = new Class[]{javax.servlet.ServletContextAttributeListener.class, javax.servlet.ServletRequestListener.class, 
        javax.servlet.ServletRequestAttributeListener.class, javax.servlet.http.HttpSessionListener.class, javax.servlet.http.HttpSessionAttributeListener.class, 
        javax.servlet.http.HttpSessionIdListener.class, javax.servlet.ServletContextListener.class};

    private WsocHandler wsocServHandler = null;
    
    /**
     * Constructor.
     * 
     * @param name
     * @param parent
     * @param warDir
     */
    public WebApp31(WebAppConfiguration webAppConfig,
                  ClassLoader moduleLoader,
                  ReferenceContext referenceContext,
                  MetaDataService metaDataService,
                  J2EENameFactory j2eeNameFactory,
                  ManagedObjectService managedObjectService) {
        super(webAppConfig, moduleLoader, referenceContext, metaDataService, j2eeNameFactory, managedObjectService);
    }

    public <T extends HttpUpgradeHandler> T createHttpUpgradeHandler(Class<T> classToCreate) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createHttpUpgradeHandler called for class: " + classToCreate);
        }


        T upgradeHandler = null;
        
        if (!isCDIEnabled()) {
            Exception ex = null;
            try {
                Constructor<T> ctr;
                ctr = classToCreate.getConstructor((Class<?>[])null);
                upgradeHandler = (T) ctr.newInstance((Object[])null);
            } catch (SecurityException e) {
                ex = e;
            } catch (NoSuchMethodException e) {
                ex = e;
            } catch (IllegalArgumentException e) {
                ex = e;
            } catch (InstantiationException e) {
                ex = e;
            } catch (IllegalAccessException e) {
                ex = e;
            } catch (InvocationTargetException e) {
                ex = e;
            } catch (RuntimeException e) {
                ex = e;
            } finally {
                if (ex!=null) {
                    Tr.error(tc, "failed.to.create.httpupgradehandler", classToCreate!=null?classToCreate.getName():null);
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.webcontainer31.webapp.WebApp.createHttpUpgradeHandler", "151", this);
                    throw new ServletException(ex);
                }
            }
        } else {
        // better for performance not to attempt CDI if it is not enabled.
            try {
                ManagedObject mo = injectAndPostConstruct(classToCreate);
                upgradeHandler = (T)mo.getObject();
                cdiContexts.put(upgradeHandler, mo);
            } catch (InjectionException e) {
                Tr.error(tc, "failed.to.create.httpupgradehandler", classToCreate!=null?classToCreate.getName():null);
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer31.webapp.WebApp.createHttpUpgradeHandler", "151", this);
                throw new ServletException(e);
            }
        }
        return upgradeHandler;
    }

    public boolean isCDIEnabled() {
        return config.isJCDIEnabled();
    }
    
    @Override
    public String getVirtualServerName() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
            		servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
            		new Object[] {"getVirtualServerName", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941            
        }
        
        String virtualServerName = config.getVirtualHostName();
        return virtualServerName;
    }
    
    @Override
    public ServletContext getFacade() {
        if (this.facade == null)
            this.facade = new ServletContextFacade31(this);

        return this.facade;
    }
        
    @Override
    public void declareRoles(String... arg0) {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"declareRoles", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        super.declareRoles(arg0);
    }

   /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getClassLoader", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }        
        return super.getClassLoader();
    }
    
    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getEffectiveMajorVersion", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getEffectiveMinorVersion", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getEffectiveMinorVersion();
    }    
    
    @Override
    public FilterRegistration getFilterRegistration(String arg0) {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getFilterRegistration", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getFilterRegistration(arg0);
    }

    @Override
    public Map<String, FilterRegistration> getFilterRegistrations() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getFilterRegistrations", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getFilterRegistrations();
    }
    
    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getServletRegistration", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ServletRegistration> getServletRegistrations() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    servlet31NLS.getString("Unsupported.op.from.servlet.context.listener.31"),
                    new Object[] {"getServletRegistrations", lastProgAddListenerInitialized, getApplicationName()}));  // 130165, PI41941
        }
        return super.getServletRegistrations();
    }   
    
    /*
     * (non-Javadoc)
     * 
     * New method added for HttpSessionIdListener, called from WebContainer
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#addHttpSessionIdListener
     * (javax.servlet.http.HttpSessionIdListener)
     */
    public void addHttpSessionIdListener(HttpSessionIdListener listener) throws SecurityException {
        this.addHttpSessionIdListener(listener, true);
    }

    /*
     * New method added for HttpSessionIdListener
     */
    private void addHttpSessionIdListener(HttpSessionIdListener listener, boolean securityCheckNeeded) throws SecurityException {
        if (securityCheckNeeded) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(perm);
            }
        }
        
        ((IHttpSessionContext31)this.getSessionContext()).addHttpSessionIdListener(listener, name);
    }
    
    protected boolean isHttpSessionIdListener(Object listener){
        boolean retVal = false;
        if (listener instanceof HttpSessionIdListener) {
            retVal = true;
        }
        return retVal;
    }
    
    /*
     * (non-Javadoc)
     * The is called from loadLifecycleListeners
     * @see com.ibm.ws.webcontainer.osgi.webapp.WebApp#checkForSessionIdListenerAndAdd(java.lang.Object)
     */
    @Override
    protected void checkForSessionIdListenerAndAdd(Object listener){
        // Added for Servlet 3.1 support
        if (isHttpSessionIdListener(listener)) {
            // add to the HttpSessionIdListener list
            ((IHttpSessionContext31)this.sessionCtx).addHttpSessionIdListener((javax.servlet.http.HttpSessionIdListener) listener, name);
            this.sessionIdListeners.add(listener);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * Had to override so that we can add logic for HttpSessionIdListeners in Servlet 3.1
     * I think this method must be override due to the WebContainer.getSessionIdListeners call
     * @see com.ibm.ws.webcontainer.webapp.WebApp#registerGlobalWebAppListeners()
     */
    @Override
    protected void registerGlobalWebAppListeners() {
        
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "registerGlobalWebAppListeners");
       
        // Notify PreEventListenerPorviders because the contract is that they 
        // go first before global listeners
        RegisterEventListenerProvider.notifyPreEventListenerProviders(this);
                    
        
        super.registerGlobalWebAppListeners();
        
        // Add the Servlet 3.1 specific code here.
        List sIdListeners = WebContainer.getSessionIdListeners(isSystemApp());
        try {
            for (int i = 0; i < sIdListeners.size(); i++) {
                sessionIdListeners.add(i, sIdListeners.get(i));
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added the following HttpSessionIdListeners: " + sessionIdListeners.toString());
            }
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "1853", this);
            logError("Failed to load global session listener: " + th);
        }
        
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "registerGlobalWebAppListeners");

    }
    
    /*
     * Need to override this method because we need access to HttpSessionIdListener.
     * (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.WebApp#commonAddListener(java.lang.String, java.util.EventListener, java.lang.Class)
     */
    @Override
    public void commonAddListener(String listenerClassName, EventListener listener,
                                  Class<? extends EventListener> listenerClass) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "commonAddListener : listenerClassName = " + listenerClassName  
                                                             + ", listener = " + (listener==null ? "null" : listener.getClass().getName()) 
                                                             + ", listenerClass = " + (listenerClass==null ? "null" : listenerClass.getName()));
        if (initialized) {
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
        }
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(nls.getString("Unsupported.op.from.servlet.context.listener"));
        }
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
            return;
        }
        Class className = listenerClass;
        try {
            if (listenerClassName != null) {
                className = Class.forName(listenerClassName, true, this.getClassLoader());
            } else if (listener != null) {
                className = listener.getClass();
            }
            Class[] validListenerClasses = new Class[7];
            validListenerClasses[0] = javax.servlet.ServletContextAttributeListener.class;
            validListenerClasses[1] = javax.servlet.ServletRequestListener.class;
            validListenerClasses[2] = javax.servlet.ServletRequestAttributeListener.class;
            validListenerClasses[3] = javax.servlet.http.HttpSessionListener.class;
            validListenerClasses[4] = javax.servlet.http.HttpSessionAttributeListener.class;
            validListenerClasses[5] = javax.servlet.http.HttpSessionIdListener.class;


                //if this was called from ServletContainerInitializer#onStartup, then the listener can implement ServletContextListener
            if (canAddServletContextListener) { // changed to protected scope in parent
                validListenerClasses[6] = javax.servlet.ServletContextListener.class;
            } else {
                if ((javax.servlet.ServletContextListener.class).isAssignableFrom(className)) {
                    throw new IllegalArgumentException(nls.getString("Error.adding.ServletContextListener"));
                }
            }
            boolean valid = false;
            Set classesSet = new HashSet();
            for (Class c : validListenerClasses) {
                if (c != null && c.isAssignableFrom(className)) {
                    valid = true;
                    classesSet.add(c);
                }
            }
            if (!valid) {
                throw new IllegalArgumentException(nls.getString("Invalid.Listener"));
            }

            if (listener == null) {
                //processDynamicInjectionMetaData(null, className);

                // Class or className was passed, need to create an instance
                // Injection done here
                try {
                    listener = createListener(className);
                } catch (Exception e) {
                        logger.logp(Level.SEVERE, CLASS_NAME, "commonAddListener", "exception.occurred.while.creating.listener.instance", new Object[] {
                                className, e });
                }
            }
            // add it to the end of the ordered list of listeners
            if (classesSet.contains(javax.servlet.ServletContextAttributeListener.class)) {
                this.servletContextLAttrListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.ServletRequestListener.class)) {
                this.servletRequestListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.ServletRequestAttributeListener.class)) {
                this.servletRequestLAttrListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.http.HttpSessionListener.class)) {
                    this.sessionListeners.add(listener);//add to this list in case we need to do a preDestroy
                    // No need to to HttpSessionListeners if the sessionContext is null because
                    // it will be added when the session context is created.
                    if (this.getSessionContext()!=null)
                        this.addHttpSessionListener((HttpSessionListener) listener, false);
            }
            if (classesSet.contains(javax.servlet.http.HttpSessionAttributeListener.class)) {
                        sessionAttrListeners.add(listener);//add to this list in case we need to do a preDestroy
                this.addHttpSessionAttributeListener((HttpSessionAttributeListener) listener);
            }
            // Added for Servlet 3.1 HttpSessionIdListener support
            if (classesSet.contains(javax.servlet.http.HttpSessionIdListener.class)) {
                sessionIdListeners.add(listener);//add to this list in case we need to do a preDestroy
                this.addHttpSessionIdListener((HttpSessionIdListener) listener);
            }
            if (classesSet.contains(javax.servlet.ServletContextListener.class)) {
                this.servletContextListeners.add(listener);
            }
        } catch (ClassNotFoundException e) {
            // This method can be called from addListener(String) which calls this method with a null
            // value for listenerClass, in turn if a ClassNotFoundException occurs the className is also null 
            // as className is set to = listenerClass before calling Class.forName which can throw the ClassNotFoundException.
            // Due to className having the possibility of being null we can't log className.getName() as an NPE will occur. Also
            // we should be logging listenerClassName as that is the listenerClassName that could not be found.
            logger.logp(Level.SEVERE, CLASS_NAME, "commonAddListener", 
                        "exception.occurred.while.adding.listener", new Object[] {listenerClassName});
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "commomAddListener");
        
    }
    
    
    /*
     * 
     * For Servlet 3.1 the minor version should be 1.
     * 
     * @see com.ibm.ws.webcontainer.webapp.WebApp#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return 1;
    }
    
    
    public String getServerInfo()
    {
        
      return WebContainer.getServerInfoFromBundle();
    }

   /*
     * (non-Javadoc)
     * @see com.ibm.ws.webcontainer.osgi.webapp.WebApp#createListener(java.lang.Class)
     */
    @Override
    public <T extends EventListener> T  createListener(Class<T> classToCreate) throws ServletException {        
        boolean valid = false;
        
        T listener = super.createListener(classToCreate);
        
        for (Class c : validListenerClasses) {
            if (c != null && c.isAssignableFrom(classToCreate)) {
                valid = true;
            }
        }
        
        if (!valid) {
            throw new IllegalArgumentException(nls.getString("exception.occurred.while.creating.listener.instance"));
        }
        
        return listener;
    }
    
    /**
     * This method should only be used to create an AsyncListener as the WebApp31.createListener no longer accepts AsyncListeners
     * as per the specification.  This method is currently called from AsyncContext31Impl.createListener.
     * 
     * @param classToCreate
     * @return
     * @throws ServletException
     */
    public <T extends EventListener> T createAsyncListener(Class<T> classToCreate) throws ServletException {
        return super.createListener(classToCreate); 
    }
    
    public void notifyServletContextCreated() throws Throwable {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "notifyServletContextCreated"); 
        
        super.notifyServletContextCreated();
        
        // Notify postEventListenerProviders because the contract is that they 
        // go last after any programmatically added listeners adeded by ServletContextListeners
        RegisterEventListenerProvider.notifyPostEventListenerProviders(this);
       
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "notifyServletContextCreated"); 
        
    }
    
    public void handleRequest(ServletRequest request, ServletResponse response) throws Exception {
    
        //defect 168286 - need to call beginContext so CDI 1.2 can get the ComponentMetaData properly on their end.
        ComponentMetaDataAccessorImpl cmdai= null;  
        if(config.isJCDIEnabled()) {
              cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
              cmdai.beginContext(getModuleMetaData().getCollaboratorComponentMetaData());
        }
        
        super.handleRequest(request, response);
        
        //defect 168286 - end the context after the request for CDI 1.2
        if(cmdai != null) {
            cmdai.endContext();
        }
    }

    /**
     * Return an H2Handler
     */
    public H2Handler getH2Handler() {
        return new H2HandlerImpl();
    }   
  
    //register websocket handler  
    public void registerWebSocketHandler(WsocHandler wsocServHandler) {
        this.wsocServHandler = wsocServHandler;
    }
    
    //get websocket handler
    public WsocHandler getWebSocketHandler() {
        return this.wsocServHandler;
    } 
}
