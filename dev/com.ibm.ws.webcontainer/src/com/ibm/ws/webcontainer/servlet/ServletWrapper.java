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
package com.ibm.ws.webcontainer.servlet;

import java.beans.Beans;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.filter.ChainedResponse;
import com.ibm.ws.container.Configuration;
import com.ibm.ws.container.Container;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.core.Command;
import com.ibm.ws.webcontainer.core.Request;
import com.ibm.ws.webcontainer.core.Response;
import com.ibm.ws.webcontainer.exception.WebContainerUnavailableException;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.util.ApplicationErrorUtils;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebApp.ANNOT_TYPE;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;
import com.ibm.ws.webcontainer.webapp.WebAppEventSource;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.ws.webcontainer.webapp.WebAppServletInvocationEvent;
import com.ibm.wsspi.http.channel.exception.WriteBeyondContentLengthException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.ClosedConnectionException;
import com.ibm.wsspi.webcontainer.IPlatformHelper;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorInvocationEnum;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.TxCollaboratorConfig;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.ServletReferenceListener;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

/**
 * @author asisin
 * 
 *         Base class for all targets that eventually get compiled into
 *         servlets. For Servlets, this class will get created by the
 *         WebExtensionProcessor, and for other targets it will really be a
 *         subclass of this wrapper which will get instantiated but the
 *         corresponding extension processor.
 */
@SuppressWarnings("unchecked")
public abstract class ServletWrapper extends GenericServlet implements RequestProcessor, Container, IServletWrapper, IServletWrapperInternal {
    private static final long serialVersionUID = -4479626085298397598L;
    private boolean notifyInvocationListeners;
    // states that a servlet can be in
    // Note: If a new state is added here it should be added to the getStateString() method as well
    protected final byte UNINITIALIZED_STATE = -1;
    protected final byte AVAILABLE_STATE = 0;
    protected final byte UNAVAILABLE_STATE = 1;
    protected final byte UNAVAILABLE_PERMANENTLY_STATE = 2;

    protected ICollaboratorHelper collabHelper;

    // number of threads currently executing the service method
    // this is primarily used for the check before the destroy() is called
    // LIBERTY Changed to an AtomicInteger
    private AtomicInteger nServicing = new AtomicInteger(0);

    private long lastAccessTime = 0;

    protected byte state = UNINITIALIZED_STATE;

    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.servlet.ServletWrapper";

    private static TraceNLS nls = TraceNLS.getTraceNLS(ServletWrapper.class, "com.ibm.ws.webcontainer.resources.Messages");

    protected com.ibm.wsspi.webcontainer.servlet.IServletConfig servletConfig;
    protected WebApp context;

    // the actual Servlet instance which will get loaded by the loadServlet()
    // method
    // 
    protected Servlet target;

    private List cacheWrappers = null;

    protected ClassLoader targetLoader;
    protected WebAppEventSource evtSource;

    private ServletEvent event;

    protected String unavailableMessage;
    private long unavailableUntil = -1;

    protected boolean isSTM = false;

    protected boolean internalServlet = false;

    protected IInvocationCollaborator[] webAppInvocationCollaborators;

    protected IPlatformHelper platformHelper;
    private IWebAppNameSpaceCollaborator webAppNameSpaceCollab;
    private IWebAppTransactionCollaborator txCollab;
    private IConnectionCollaborator connCollab;

    // PK58806
    private static boolean suppressServletExceptionLogging = WCCustomProperties.SUPPRESS_SERVLET_EXCEPTION_LOGGING;
    private static EnumSet<CollaboratorInvocationEnum> throwExceptionEnum = EnumSet.of(CollaboratorInvocationEnum.EXCEPTION);
    // PK76117
    private static boolean discernUnavailableServlet = WCCustomProperties.DISCERN_UNAVAILABLE_SERVLET;
    private static boolean reInitServletonInitUnavailableException = WCCustomProperties.REINIT_SERVLET_ON_INIT_UNAVAILABLE_EXCEPTION; //PM01373

    private static boolean destroyServletonServiceUnavailableException = WCCustomProperties.DESTROY_SERVLET_ON_SERVICE_UNAVAILABLE_EXCEPTION; //PM98245

    //private static boolean keySizeFromCipherMap = 
    //   Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.keysizefromciphermap", "true")).booleanValue();  //not exposed in Infocenter
    // 721610 (PM51389), PM92496  moved this property to SRTServletRequest

    private boolean servlet23 = false;

    // PK83258 Start
    private static Class[] PARAMS_HEAD_TRACE={HttpServletRequest.class,HttpServletResponse.class};
   
    private boolean defaultHeadMethodInUse=false;
    private boolean defaultTraceMethodInUse=false;
    private Boolean checkedForDefaultMethods=null;
    
    private static boolean defaultTraceRequestBehavior = WCCustomProperties.DEFAULT_TRACE_REQUEST_BEHAVIOR;
    
    private static boolean defaultHeadRequestBehavior = WCCustomProperties.DEFAULT_HEAD_REQUEST_BEHAVIOR;  
	// PK83258 End
    private static boolean useOriginalRequestState = WCCustomProperties.USE_ORIGINAL_REQUEST_STATE; //PM88028
    
    private boolean warningStatusSet = false;

    /*
     * The base lifecycle processing built into the BaseContainer will not apply
     * to this Container.
     */
    public ServletWrapper(IServletContext parent) {
        this.context = (WebApp) parent;
        servlet23 = context.isServlet23();
        this.evtSource = (WebAppEventSource) context.getServletContextEventSource();
        notifyInvocationListeners = evtSource.hasServletInvocationListeners();
        lastAccessTime = System.currentTimeMillis();
        collabHelper = context.getCollaboratorHelper();
        webAppNameSpaceCollab = collabHelper.getWebAppNameSpaceCollaborator();
        txCollab = collabHelper.getWebAppTransactionCollaborator();
        connCollab = collabHelper.getWebAppConnectionCollaborator();
        platformHelper = WebContainer.getWebContainer().getPlatformHelper();
    }

    public void setParent(IServletContext parent) {
        this.context = (WebApp) parent;
    }

    public synchronized void init(ServletConfig conf) throws ServletException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "init", "ServletWrapper enter init for servletName--> [" + getServletName() + "] , state -->["
                    + getStateString(state) + "]");
        /*
         * Check to see if someone else initizlized this while we were waiting
         * to get in here....
         */
        if (state != UNINITIALIZED_STATE) {
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //PM01373
				logger.exiting(CLASS_NAME,"init", "ServletWrapper exit init for servletName--> [" + getServletName() + "] , state -->[" + getStateString(state) +"]");
		
            return;
        }

        WebComponentMetaData cmd = null;
        cmd = ((com.ibm.wsspi.webcontainer.servlet.IServletConfig) conf).getMetaData();
        
        Object secObject;
        TxCollaboratorConfig txConfig;
        // IConnectionCollaboratorHelper connCollabHelper=
        // collabHelper.createConnectionCollaboratorHelper();
        
        //These preInvokes are nested inside other collaborator preInvokes when you are
        //not an init-on-startup servlet. This may or may not be bad, but this is how we have always done it.
// ALEX TODO        HandleList _connectionHandleList = new HandleList();  // Seems to just be a cookie across pre/postInvoke
        try {
            webAppNameSpaceCollab.preInvoke(cmd);
            // 246216
            // This should be called after the nameSpaceCollaborator as is
            // done
            // in handleRequest so preInvoke can reference the component
            // meta data
            secObject = collabHelper.getSecurityCollaborator().preInvoke(servletConfig.getServletName());

          collabHelper.doInvocationCollaboratorsPreInvoke(webAppInvocationCollaborators, (WebComponentMetaData)(getWebApp().getWebAppCmd()));

            // end LIDB549.21
            txConfig = txCollab.preInvoke(null, servlet23);

//            connCollab.preInvoke(_connectionHandleList, true);
        } catch (SecurityViolationException wse) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(wse, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", "227", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "init", "uncaught.init.exception.thrown.by.servlet", new Object[] { getServletName(),
                    getWebApp().getApplicationName(), wse });
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), wse);
            evtSource.onServletInitError(errorEvent);
            // evtSource.onServletFinishInit(errorEvent);
            evtSource.onServletUnloaded(errorEvent);
            throw new ServletException(nls.getString("preInvoke.Security.Exception", "preInvoke Security Exception"), wse);
        } catch (IOException ioe) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ioe, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", "248", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "init", "uncaught.init.exception.thrown.by.servlet", new Object[] { getServletName(),
                    getWebApp().getApplicationName(), ioe });
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), ioe);
            evtSource.onServletInitError(errorEvent);
            // evtSource.onServletFinishInit(errorEvent);
            evtSource.onServletUnloaded(errorEvent);
            throw new ServletException(nls.getString("Uncaught.initialization.exception.thrown.by.servlet",
                    "Uncaught initialization exception thrown by servlet"), ioe);
        } catch (Exception e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", "181", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "init", "uncaught.init.exception.thrown.by.servlet", new Object[] { getServletName(),
                    getWebApp().getApplicationName(), e });
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
            evtSource.onServletInitError(errorEvent);
            // evtSource.onServletFinishInit(errorEvent);
            evtSource.onServletUnloaded(errorEvent);
            throw new ServletException(nls.getString("Uncaught.initialization.exception.thrown.by.servlet",
                    "Uncaught initialization exception thrown by servlet"), e);
        }

        registerMBean();

        internalServlet = servletConfig.isInternal();

        ClassLoader origClassLoader = null;
        try {
            origClassLoader = ThreadContextHelper.getContextClassLoader();
            final ClassLoader warClassLoader = context.getClassLoader();
            if (warClassLoader != origClassLoader) {
                ThreadContextHelper.setClassLoader(warClassLoader);
            } else {
                origClassLoader = null;
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "init", "ClassLoader set to: " + warClassLoader.toString()); // PK26183

            evtSource.onServletStartInit(getServletEvent());
            target.init(conf);
            modifyTarget(target);
            logger.logp(Level.INFO, CLASS_NAME, "init", "[{0}].Initialization.successful", new Object[] { getServletName(),
                    this.context.getContextPath(), this.context.getApplicationName() });
            evtSource.onServletFinishInit(getServletEvent());
            evtSource.onServletAvailableForService(getServletEvent());
            setAvailable();

        } catch (UnavailableException ue) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.logp(Level.FINE, CLASS_NAME, "init", "unavailableException throw by --> [" + getServletName(), ue);
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         ue, 
                                                                         "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", 
                                                                         "259", 
                                                                         this);
            //PM01373 Start
            if(!reInitServletonInitUnavailableException){ //default to true in Liberty
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                    logger.logp(Level.FINE, CLASS_NAME,"init", " Custom property reInitServletonInitUnavailableException is not set");
                handleUnavailableException(ue, false); 
            }
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                    logger.logp(Level.FINE, CLASS_NAME,"init", " Custom property reInitServletonInitUnavailableException is set");
                handleUnavailableException(ue, true);			
            }
            //PM01373 End
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), ue);
            evtSource.onServletInitError(errorEvent);
            // Should not call this
            // evtSource.onServletFinishInit(errorEvent);
            //PM01373 Start
            if(reInitServletonInitUnavailableException){
                evtSource.onServletUnloaded(errorEvent); 
                this.deregisterMBean(); // Deregister
            }
            //PM01373 End
            //PM98245 Start
            if (destroyServletonServiceUnavailableException) {
                // save the UE is thrown from init.
                WebContainerRequestState UEfromInitRequestState = WebContainerRequestState.getInstance(true);
                UEfromInitRequestState.setAttribute("UEinInit", true);
            }//PM98245 End
            // PK76117 Start
            if (discernUnavailableServlet) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "init", "Create WebContainerUnavailableException");
                throw WebContainerUnavailableException.create(ue);
            }
            // PK76117 End
            throw ue;
        } catch (ServletException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", "172", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "init", "uncaught.init.exception.thrown.by.servlet", new Object[] { getServletName(),
                    getWebApp().getApplicationName(), e });
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
            evtSource.onServletInitError(errorEvent);
            // evtSource.onServletFinishInit(errorEvent);
            evtSource.onServletUnloaded(errorEvent);
            throw e;
        } catch (Throwable e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init", "181", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "init", "uncaught.init.exception.thrown.by.servlet", new Object[] { getServletName(),
                    getWebApp().getApplicationName(), e });
            ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
            evtSource.onServletInitError(errorEvent);
            // evtSource.onServletFinishInit(errorEvent);
            evtSource.onServletUnloaded(errorEvent);
            throw new ServletException(nls.getString("Uncaught.initialization.exception.thrown.by.servlet",
                    "Uncaught initialization exception thrown by servlet"), e);
        } finally {

            if (origClassLoader != null) {
                final ClassLoader fOrigClassLoader = origClassLoader;

                ThreadContextHelper.setClassLoader(fOrigClassLoader);
            }
/*            try {
                connCollab.postInvoke(_connectionHandleList, true);
            } catch (CSIException e) {
                // It is already added to cache..do we need to throw the
                // exception back
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.webapp.WebAppServletManager.addServlet",
                        "260", this);
            }*/
            try {
                txCollab.postInvoke(null, txConfig, servlet23);
            } catch (Exception e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init",
                        "268", this);
            }

            // begin LIDB549.21
            collabHelper.doInvocationCollaboratorsPostInvoke(webAppInvocationCollaborators, getWebApp().getWebAppCmd());
            // end LIDB549.21

            try {
                collabHelper.getSecurityCollaborator().postInvoke(secObject);
            } catch (Exception e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.init",
                        "325", this);
            }

            webAppNameSpaceCollab.postInvoke();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.exiting(CLASS_NAME, "init", "ServletWrapper exit init for servletName--> [" + getServletName() + "] , state -->[" + getStateString(state)
                        + "]");
        }

    }

    protected void registerMBean() {
    	//Should never call this in WAS
        if (logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "registerMBean", "executing method stub");
    }

    protected void deregisterMBean() {
    	//Should never call this in WAS
        if (logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "deregisterMBean", "executing method stub");
    }

    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        //PM88028 Start
        ServletRequest httpreq = null;
                
        if(useOriginalRequestState){
                
                WebContainerRequestState reqStateSaveRequestResponse = WebContainerRequestState.getInstance(false);
                if(reqStateSaveRequestResponse != null){                        
                        httpreq = (ServletRequest)reqStateSaveRequestResponse.getAttribute("unFilteredRequestObject");
                }       
        }
        
        if(httpreq == null)
        {
                httpreq = req;                  
        }
//      IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(req);       
        IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(httpreq); //PM88028 End
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) wasreq.getWebAppDispatcherContext();
        handleRequest(req, res, dispatchContext);
    }

    /**
     * @see com.ibm.ws.core.RequestProcessor#handleRequest(Request, Response)
     */
    public void handleRequest(ServletRequest req, ServletResponse res, WebAppDispatcherContext dispatchContext) throws Exception {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        // 569469
        // if (isTraceOn&&logger.isLoggable (Level.FINE)) //306998.15
        // logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "entry");
        if (isTraceOn && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME,"handleRequest " + this.toString()+ " ,request-> " +req+ " ,response-> "+res); //PM01373

        }

        // 569469
        HttpServletRequest httpRequest = null;
        IExtendedRequest wasreq = null;
        //PM50111 Start
        boolean filtersInvokedForRequest = false;
        WebContainerRequestState reqFilterState = WebContainerRequestState.getInstance(false);
        if(reqFilterState!= null){
            filtersInvokedForRequest = reqFilterState.isInvokedFilters();
            if(filtersInvokedForRequest) reqFilterState.setInvokedFilters(false); //set the local filter var and reset the requestState var

            if(useOriginalRequestState && filtersInvokedForRequest){                    
                httpRequest = (HttpServletRequest)reqFilterState.getAttribute("unFilteredRequestObject");
                //remove attribute
                reqFilterState.removeAttribute("unFilteredRequestObject");
                if(httpRequest != null){
                    wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(httpRequest);    
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                        logger.logp(Level.FINE,CLASS_NAME,"handleRequest ", " useOriginalRequestState request--->" + httpRequest.getRequestURI()+ "<---");
                    }         
                }
            }
        }
        //PM50111 End
        if(httpRequest == null)
        {     
            httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(req, HttpServletRequest.class);
            wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(req);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE,CLASS_NAME,"handleRequest ", " request--->" + httpRequest.getRequestURI()+ "<---");
            }
        }


//        IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(req);
//        HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(req, HttpServletRequest.class);
//        
        if (wasreq.isAsyncSupported()){
          //141092
            boolean isAsyncSupported = this.servletConfig.isAsyncSupported();
            if (!isAsyncSupported){
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
                if (reqState != null) {
                    reqState.setAttribute("resourceNotSupportAsync", "servlet[ "+ getServletName() +" ]");
                }
            }
          //141092
           wasreq.setAsyncSupported(isAsyncSupported);
        }
        
        HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(res, HttpServletResponse.class);

        boolean isInclude = dispatchContext.isInclude();
        boolean isForward = dispatchContext.isForward();

 

        // PK54805 End
        if (state == UNAVAILABLE_PERMANENTLY_STATE) {
            UnavailableException ue = new UnavailableException(unavailableMessage);

            // PK56247 Start
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(ue, dispatchContext.getCurrentServletReference());

            if (isInclude || isForward) {

                /*
                 * Throw the UnavailableException if we are an include or
                 * forward so the calling servlet has a chance to handle it.
                 */
                // PK76117 Start
                if (discernUnavailableServlet) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "init", "Create WebContainerUnavailableException");
                    throw WebContainerUnavailableException.create(ue);
                }
                // PK76117 End
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.exiting(CLASS_NAME, "handleRequest", "throw exception : " + ue.toString());
                }
                throw ue;
            }
            // PK56247 Start
            // throw constructErrorReport(ue,
            // dispatchContext.getCurrentServletReference());
            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "handleRequest", "throw error report : " + errorReport.getExceptionType());
            }
            throw errorReport;
        }

        boolean servletCalled = false; // PK56247
        try {

            // begin 270421 SVT:Portal Automation ServletInvocationListener
            // testcase fails WAS.webcontainer
            notifyInvocationListeners = evtSource.hasServletInvocationListeners();
            // end 270421 SVT:Portal Automation ServletInvocationListener
            // testcase fails WAS.webcontainer

            // Changes required so we can leave the last servlet reference on
            // the stack
            String nested = (String) req.getAttribute(WebAppRequestDispatcher.DISPATCH_NESTED_ATTR);
            if (nested != null)
                dispatchContext.pushServletReference(this);
            else
                dispatchContext.clearAndPushServletReference(this);

            // begin 270421 SVT:Portal Automation ServletInvocationListener
            // testcase fails WAS.webcontainer
            //if (isTraceOn && logger.isLoggable(Level.FINE)) { // 306998.15
            //String includeReqURI = (String) httpRequest.getAttribute(WebAppRequestDispatcher.REQUEST_URI_INCLUDE_ATTR);
            //    String requestURI = httpRequest.getRequestURI();
            //}
            // end 270421 SVT:Portal Automation ServletInvocationListener
            // testcase fails WAS.webcontainer

            if (httpRequest.isSecure()
                    && (httpRequest.getAttribute("javax.servlet.request.cipher_suite")== null)) { // PM92496 Start
                // we have an SSL connection...set the attributes
                ServletRequest implRequest = ServletUtil.unwrapRequest(httpRequest);
                
                String cipherSuite = ((SRTServletRequest) implRequest).getCipherSuite();
                ((SRTServletRequest) implRequest).setSSLAttributesInRequest(httpRequest, cipherSuite);
            }// PM92496 End

            WebAppServletInvocationEvent invocationEvent = null;

            if (notifyInvocationListeners)
                invocationEvent = new WebAppServletInvocationEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(),
                                                                   httpRequest, httpResponse);

            target = loadServlet();
            if (target==null&&!servletConfig.isClassDefined())
                throw new FileNotFoundException();
            if (state == UNINITIALIZED_STATE)
            {
                // PM01373 Start	
                if ( unavailableUntil == -1 ){ 

                    if (isTraceOn&&logger.isLoggable (Level.FINE))
                    {logger.logp(Level.FINE, CLASS_NAME,"handleRequest", " state --> " + getStateString(state)+ " ,  init");}

                    init(servletConfig);
                }		 
                else {
                    if (isTraceOn&&logger.isLoggable (Level.FINE))
                    {logger.logp(Level.FINE, CLASS_NAME,"handleRequest", " state --> " + getStateString(state) + "&& unavailableUntil --> "+ unavailableUntil);}

                    lastAccessTime = System.currentTimeMillis();
                    long timeDiff = unavailableUntil - lastAccessTime;
                    if (timeDiff <= 0)
                    {		
                        if (isTraceOn&&logger.isLoggable (Level.FINE))
                        {logger.logp(Level.FINE, CLASS_NAME,"handleRequest", " unavailable time expired , init ");}

                        init(servletConfig);
                    }
                    else
                    {
                        int timeLeft = (int) (timeDiff) / 1000;
                        if (timeLeft == 0)
                        {
                            //caused by truncation of long to int
                            timeLeft = 1;
                        }
                        if (isTraceOn&&logger.isLoggable (Level.FINE))					
                        {logger.logp(Level.FINE, CLASS_NAME,"handleRequest", " remaining unavailable time --> " + timeLeft);}
                        //UnavailableException from init()is already handled then thrown, need to handle this one and throw.

                        UnavailableException ue = new UnavailableException(unavailableMessage, timeLeft);
                        if (isTraceOn&&logger.isLoggable (Level.FINE))
                        {logger.logp(Level.FINE, CLASS_NAME,"handleRequest", " handle UnavailableException for TempUE ");}

                        this.handleUnavailableException(ue, true);					
                        throw ue;						
                    }
                }// PM01373 End
            }

            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "internal servlet --> " + servletConfig.isInternal());
            }

            if (context.isMimeFilteringEnabled()) {
                ChainedResponse chainedResp = new ChainedResponse(httpRequest, httpResponse);

                // set a default content type
                chainedResp.setContentType("text/html");

                // defect 55215 - automatically transfer client headers through
                // the chain
                Enumeration names = httpRequest.getHeaderNames();
                while (names.hasMoreElements()) {

                    String name = (String) names.nextElement();
                    String value = httpRequest.getHeader(name);
                    if (!name.toLowerCase().startsWith("content")) {

                        // don't transfer content headers
                        chainedResp.setAutoTransferringHeader(name, value);
                    }
                }

                this.service(httpRequest, chainedResp, invocationEvent);

                // BEGIN PQ47136
                // part 1: check to see if chainedResponse
                // contains a redirectURI; if so get the value.
                String _redirectURI = null;
                if (chainedResp.isRedirected()) {
                    _redirectURI = chainedResp.getRedirectURI();
                }
                // END PQ47136

                String mimeType = chainedResp.getHeader("content-type");

                IServletWrapper wrapper = getMimeFilterWrapper(mimeType);
                while (wrapper != null) {
                    httpRequest = chainedResp.getChainedRequest();

                    // begin pq50381: part 1 --> keep a copy of previous
                    // chainedResponse
                    // Purpose: Pass pertinent response information along chain.
                    ChainedResponse prevChainedResp = chainedResp;
                    // begin pq40381: part 1

                    chainedResp = new ChainedResponse(httpRequest, httpResponse);

                    // begin pq50381: part 2
                    // Purpose: Transcode Publishing checks StoredResponse for
                    // mime filtered servlet's contentType to decide how to
                    // filter
                    // ultimate response. Transfer client set headers/ cookies
                    // thru chain.
                    chainedResp.setContentType(mimeType);
                    transferHeadersFromPrevChainedResp(chainedResp, prevChainedResp);
                    // end pq50381: part 2

                    // BEGIN PQ47136
                    // part 2: if redirectURI was specified
                    // set the location header to the redirectURI.
                    // and set appropriate status code.
                    if (_redirectURI != null) {
                        // System.out.println("transferring header location = ["+
                        // _redirectURI +"]");
                        // chainedResp.setAutoTransferringHeader ("location",
                        // _redirectURI); // could not get this to work.
                        chainedResp.setHeader("location", _redirectURI); // works
                        // but
                        // should
                        // be
                        // replaced
                        // with
                        // above.
                        chainedResp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                    }
                    // END PQ47136

                    dispatchContext.pushServletReference(wrapper);

                    ((ServletWrapper) wrapper).service(httpRequest, chainedResp, invocationEvent); // optimized
                    // dispatch

                    dispatchContext.popServletReference();

                    String newMimeType = chainedResp.getHeader("content-type");
                    String nMime = newMimeType.toLowerCase();
                    String oMime = mimeType.toLowerCase();
                    int icharset = nMime.indexOf(";");
                    if (icharset != -1)
                        nMime = nMime.substring(0, icharset);
                    icharset = oMime.indexOf(";");
                    if (icharset != -1)
                        oMime = oMime.substring(0, icharset);
                    if (nMime.equals(oMime)) {
                        // recursive filter break condition
                        wrapper = null;
                    } else {
                        mimeType = newMimeType;
                        wrapper = getMimeFilterWrapper(mimeType);
                    }
                }

                chainedResp.transferResponse(httpResponse);
            } else {

                servletCalled = true; // PK56247
                service(req, res, invocationEvent);

                //PM50111 Start                           
                if (isTraceOn && logger.isLoggable(Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "isInclude->" + isInclude +" , res->"+  res +" , httpResponse->"+  httpResponse
                                +" ,filtersInvokedForRequest-->"+ filtersInvokedForRequest +" ,invokeFlushAfterService -->"+ WCCustomProperties.INVOKE_FLUSH_AFTER_SERVICE);
                }

                // Start PM50111.
                // flush any buffered data that has been written to the response as per spec 5.5 
                // for v7 compatibility we should not call flushbuffer here, since filters are not done yet. Customer need to set custom property to false.
                // flush if custom property invokeFlushAfterService is set (default) 
                // flush if the filtersAreInvoked otherwise finishRequest will take care of flush
                // Don't flush the buffer if this servlet was included (see spec) or async is set.
                // Dont' flush if the response is committed. 
                // (res instanceof IExtendedResponse) checks make sure this is response object is ours and not wrappedresponse

                if (!isInclude && !httpResponse.isCommitted() && WCCustomProperties.INVOKE_FLUSH_AFTER_SERVICE) {                         
                    //check for async now

                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                    boolean isStartAsync=false;
                    boolean isComplete = false;
                    if (reqState!=null){
                        if (reqState.isAsyncMode())
                            isStartAsync = true;
                        if (reqState.isCompleted())
                            isComplete=true;
                    }

                    if (isTraceOn && logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "isComplete->" + isComplete + ", isStartAsync->"+isStartAsync);
                    }       
                    if (!isStartAsync && !isComplete ) {                                                            
                        if (!(httpResponse instanceof IExtendedResponse)) {
                            httpResponse.flushBuffer();   
                        }
                        //else {
                        //        ((IExtendedResponse) httpResponse).flushBuffer(false);   
                        //}

                    }                

                }
            }
        } catch (WriteBeyondContentLengthException clex) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(clex, "com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest()",
            "293");
            dispatchContext.pushException(clex);
            try {
                httpResponse.flushBuffer();
            } catch (IOException i) {
                ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(i, dispatchContext.getCurrentServletReference());
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(i, "com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest()", "810");
                throw errorReport;
            }
        } catch (FileNotFoundException fnfe) {
            if (isRethrowOriginalException(req, isInclude, isForward)) {
                dispatchContext.pushException(fnfe);
                throw fnfe;
            }
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(fnfe, dispatchContext.getCurrentServletReference());
            dispatchContext.pushException(fnfe);
            //com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ioe, "com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest()", "298");
            throw errorReport;
        }
        catch (IOException ioe) {
            if (isRethrowOriginalException(req, isInclude, isForward)) {
                dispatchContext.pushException(ioe);
                throw ioe;
            }
            if((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) && ioe.getMessage()!= null && ioe.getMessage().contains("SRVE0918E")){
                throw ioe;
            }
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(ioe, dispatchContext.getCurrentServletReference());
            dispatchContext.pushException(ioe);
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ioe, "com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest()", "830");
            throw errorReport;
        } catch (UnavailableException ue) {
            ServletErrorReport errorReport = null;
            // PK76117 Start
            boolean caughtUEIsInstanceOfWUE = false;
            if (discernUnavailableServlet) {
                caughtUEIsInstanceOfWUE = (ue instanceof WebContainerUnavailableException);
            }
            if (isTraceOn && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "UnavailableException Caught : caughtUEIsInstanceOfWUE="
                            + caughtUEIsInstanceOfWUE + 
                            ", discernUnavailableServlet="+discernUnavailableServlet+ ", state = " + getStateString(state) ); //PM01373
            // PK76117 End
            try {
                errorReport = WebAppErrorReport.constructErrorReport(ue, dispatchContext.getCurrentServletReference());
                //PM98245 Start
                boolean UEfromService = false;
                if (destroyServletonServiceUnavailableException) {
                    WebContainerRequestState ueState = WebContainerRequestState.getInstance(true);
                    if (ueState != null && (ueState.getAttribute("UEinService") != null)) {
                        UEfromService = true;

                        if (isTraceOn && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "UnavailableException Caught, getAttribute(UEinService): forward--> " + isForward
                                                                                 + " ,include -->" + isInclude);
                        if (!(isInclude || isForward) ){
                            ueState.removeAttribute("UEinService");
                        }
                    }
                    else if (ueState != null && (ueState.getAttribute("UEinInit") != null)) {
                        if (isTraceOn && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "UnavailableException Caught, getAttribute(UEinInit): forward--> " + isForward + "include -->"
                                                                                 + isInclude);

                        if (!(isInclude || isForward)) {
                            ueState.removeAttribute("UEinInit");
                        }
                    }
                }//PM98245 End
                 //PM01373 need to check state since UE already handled for init()    
                 //PM98245 servlet will never be permanently unavailable i.e. we will never call destroy ,so we add check of UE from service
                if (!UEfromService && reInitServletonInitUnavailableException) {
                    if (state == this.AVAILABLE_STATE) {
                        state = this.UNINITIALIZED_STATE;
                    }
                }

                if (!caughtUEIsInstanceOfWUE && state != UNINITIALIZED_STATE) {    // PK76117	
                    handleUnavailableException(ue,false); //PM01373
                }

                if (isInclude || isForward) {
                    throw ue;
                }

                if (isRethrowOriginalException(req, isInclude, isForward)) {
                    throw ue;
                }
            } catch (UnavailableException une) {
                if (isRethrowOriginalException(req, isInclude, isForward)) {
                    // PK76117 Start
                    if (discernUnavailableServlet) {
                        if ((une instanceof WebContainerUnavailableException))
                            throw une;
                        else {
                            if (isTraceOn && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Create WebContainerUnavailableException");
                            throw WebContainerUnavailableException.create(ue);
                        }
                    }
                    // PK76117 End
                    throw une;
                }
                errorReport = WebAppErrorReport.constructErrorReport(une, dispatchContext.getCurrentServletReference());
            } finally {
                if (!caughtUEIsInstanceOfWUE || !servletCalled) { // PK76117
                    // 198256 - begin
                    if (!ue.isPermanent()&& (state == this.UNAVAILABLE_STATE || state == this.UNINITIALIZED_STATE)) //PM01373
                        httpResponse.setHeader("Retry-After", String.valueOf(ue.getUnavailableSeconds()));
                    // 198256 - end
                }
                dispatchContext.pushException(ue);
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ue, "com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest()",
                "302");
            }
            throw errorReport;
        }

        finally {
            dispatchContext.popServletReference();
            // PK50133 - start
            if (context.getJSPClassLoaderLimit() > 0
                            && (context.getJSPClassLoaderExclusionList() == null || !context.getJSPClassLoaderExclusionList().contains(
                                                                                                                                       httpRequest.getRequestURI()))) {
                // PK82657 - add check for whether to track forwards and
                // includes
                if (context.isJSPClassLoaderLimitTrackIF() || (!isInclude && !isForward)) {
                    ClassLoader cl = getTargetClassLoader();
                    Class jspClassLoaderClassName = context.getJSPClassLoaderClassName();
                    if (cl != null && cl.getClass().isAssignableFrom(jspClassLoaderClassName)) {
                        context.addAndCheckJSPClassLoaderLimit(this);
                    }
                }
            }
            // PK50133 - end


            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "handleRequest");
            }
            // 569469
        }
    }

    private boolean isRethrowOriginalException(ServletRequest req, boolean isInclude, boolean isForward) {
        return (isInclude || isForward) && req.getAttribute(WebContainerConstants.IGNORE_DISPATCH_STATE) == null;
    }

    protected Object getTransaction() throws Exception {
        return null;
    }

    protected void checkTransaction(Object transaction) {

    }

    protected void checkForRollback() {

    }

    /**
     * @see com.ibm.ws.core.CommandSequence#addCommand(Command)
     */
    public void addCommand(Command command) {
    }

    public String getName() {
        if (servletConfig == null)
            return null;

        return servletConfig.getServletName();
    }

    /**
     * @see com.ibm.ws.core.CommandSequence#removeCommand(Command)
     */
    public void removeCommand(Command command) {
    }

    protected void doDestroy() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "doDestroy " + this.toString()); // 569469
        // logger.logp(Level.FINE, CLASS_NAME,"doDestroy", "entry");
        if (state != this.UNINITIALIZED_STATE) {
            
            // process preDestroys first 
            if ((context != null) && (target != null)) {
                Throwable t = context.invokeAnnotTypeOnObjectAndHierarchy(target, ANNOT_TYPE.PRE_DESTROY);
                if (t != null) {
                    // log exception - could be from user's code - and move on 
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "Exception caught during preDestroy processing: " + t);
                    }
                }

            }
            
            
            WebComponentMetaData cmd = null;
            cmd = ((com.ibm.wsspi.webcontainer.servlet.IServletConfig) servletConfig).getMetaData();

            Object secObject = null;
            TxCollaboratorConfig txConfig = null;
            // IConnectionCollaboratorHelper connCollabHelper=
            // collabHelper.createConnectionCollaboratorHelper();
//            HandleList _connectionHandleList = new HandleList();
            try {
                webAppNameSpaceCollab.preInvoke(cmd);
                // 246216
                // This should be called after the nameSpaceCollaborator as is
                // done
                // in handleRequest so preInvoke can reference the component
                // meta data
                secObject = collabHelper.getSecurityCollaborator().preInvoke(servletConfig.getServletName());

                collabHelper.doInvocationCollaboratorsPreInvoke(webAppInvocationCollaborators, (WebComponentMetaData)(getWebApp().getWebAppCmd()));

                // end LIDB549.21
                txConfig = txCollab.preInvoke(null, servlet23);

//                connCollab.preInvoke(_connectionHandleList, true);
                deregisterMBean();

                ClassLoader origClassLoader = null;
                try {
                    origClassLoader = ThreadContextHelper.getContextClassLoader();
                    final ClassLoader warClassLoader = context.getClassLoader();
                    if (warClassLoader != origClassLoader) {
                        ThreadContextHelper.setClassLoader(warClassLoader);
                    } else {
                        origClassLoader = null;
                    }
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "Servlet.unload.initiated:.{0}", getServletName());

                    setUnavailable();
                    evtSource.onServletStartDestroy(getServletEvent());

                    for (int i = 0; (nServicing.get() > 0) && i < WCCustomProperties.SERVLET_DESTROY_WAIT_TIME; i++) {
                        try {
                            if (i == 0) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                                    logger.logp(Level.FINE, CLASS_NAME, "doDestroy",
                                            "servlet is still servicing...will wait up to 60 seconds for servlet to become idle: {0}",
                                            getServletName());
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                                    logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "Waiting.servlet.to.finish.servicing.requests:.{0}",
                                            getServletName());
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                    "com.ibm.ws.webcontainer.servlet.ServletWrapper.doDestroy", "377", this);
                        }
                    }

                    if (nServicing.get() > 0) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                            logger.logp(Level.FINE, CLASS_NAME, "doDestroy",
                                    "Servlet.wait.for.destroy.timeout.has.expired,.destroy.will.be.forced:.{0}", getServletName());
                    }
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "enter Servlet.destroy(): {0}", getServletName());
                    }
                    if (target != null) {
                        target.destroy();
                        logger.logp(Level.INFO, CLASS_NAME, "doDestroy", "[{0}].Destroy.successful", new Object[] { getServletName(),
                                this.context.getContextPath(), this.context.getApplicationName() });
                    }

                    evtSource.onServletFinishDestroy(getServletEvent());
                    evtSource.onServletUnloaded(getServletEvent());

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "Servlet.unloaded:.{0}", getServletName());
                } catch (Exception e) {
                    throw new ServletException(e);
                } finally {

                    if (origClassLoader != null) {
                        final ClassLoader fOrigClassLoader = origClassLoader;

                        ThreadContextHelper.setClassLoader(fOrigClassLoader);
                    }

                }
            } catch (Throwable e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper
                        .processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.destroy", "403", this);
                logger.logp(Level.SEVERE, CLASS_NAME, "doDestroy", "Uncaught.destroy().exception.thrown.by.servlet", new Object[] {
                        getServletName(), getWebApp().getApplicationName(), e });
                evtSource.onServletDestroyError(new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e));
                context.log("Error occurred while destroying servlet", e);
            } finally {
/*                try {
                    connCollab.postInvoke(_connectionHandleList, true);
                } catch (CSIException e) {
                    // It is already added to cache..do we need to throw the
                    // exception back
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.doDestroy",
                            "260", this);
                }*/
                try {
                    txCollab.postInvoke(null, txConfig, servlet23);
                } catch (Exception e) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.doDestroy",
                            "268", this);
                }

                // begin LIDB549.21
                collabHelper.doInvocationCollaboratorsPostInvoke(webAppInvocationCollaborators, (WebComponentMetaData)(getWebApp().getWebAppCmd()));
                // end LIDB549.21

                try {
                    collabHelper.getSecurityCollaborator().postInvoke(secObject);
                } catch (Exception e) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.doDestroy",
                            "325", this);
                }

                webAppNameSpaceCollab.postInvoke();
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "doDestroy", "exit Servlet.destroy(): {0}", getServletName());
        // 569469
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "doDestroy");
        }
        // 569469

    }

    /**
     * This method is called by the subclass implementation (eg.,
     * JSPExtensionServletWrapper) when it realizes that a reload is required.
     * The difference between this method and a destroy() is that the
     * servletConfig object is not destroyed because it only contains config
     * information which remains unchanged.
     * 
     */
    public void prepareForReload() {
        doDestroy();
        target = null;
        targetLoader = null;
        state = this.UNINITIALIZED_STATE;
    }

    /**
     * @see javax.servlet.Servlet#destroy() will be called by the container to
     *      destroy the target and propagate the appropriate events. This will
     *      also nullify the servletConfig object, hence it should be called by
     *      the subclasses only if they no longer need the servletConfig.
     */
    public void destroy() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "destroy", "servlet destroy for -->" + getServletName() + ", state is -->" + getStateString(state));
        }
        try {
            if (state != UNINITIALIZED_STATE && state != UNAVAILABLE_PERMANENTLY_STATE) {
                try {
                    doDestroy();
                } catch (Throwable th) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.servlet.ServletWrapper.destroy", "1152", this);
                    logger.logp(Level.SEVERE, CLASS_NAME, "destroy", "Exception.occured.during.servlet.destroy", th);
                } finally {
                    /* Avoid nulling things out so we can continue to handle requests in this wrapper
                     * while the app is being destroyed
                    target = null;
                    servletConfig = null;
                    targetLoader = null;
                    */
                    state = this.UNAVAILABLE_PERMANENTLY_STATE;
                }
            }
        } finally {
            invalidateCacheWrappers();
        }
    }

    /**
     * @see javax.servlet.Servlet#getServletConfig()
     */
    public IServletConfig getServletConfig() {
        return servletConfig;
    }

    /**
     * @see javax.servlet.Servlet#getServletInfo()
     */
    public String getServletInfo() {
        return getName() + ":" + servletConfig.getClassName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse)
     */
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        WebAppServletInvocationEvent evt = null;
        if (notifyInvocationListeners) {
            evt = new WebAppServletInvocationEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), req, res); // 263020
        }

        service(req, res, evt);
    }

    /**
     * @see javax.servlet.Servlet#service(ServletRequest, ServletResponse)
     */
    public void service(ServletRequest req, ServletResponse res, WebAppServletInvocationEvent evt) throws ServletException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "service " + this.toString()+ " ,req-->"+ req + " ,res-->"+ res); //PM50111 // 569469
        // logger.logp(Level.FINE, CLASS_NAME,"service", "service " +
        // this.toString()); // PK26183
        boolean notify = notifyInvocationListeners && (evt != null);
        if (unavailableUntil != -1) {
            lastAccessTime = System.currentTimeMillis();
            long timeDiff = unavailableUntil - lastAccessTime;
            if (timeDiff <= 0) {
                setAvailable();
            } else {
                int timeLeft = (int) (timeDiff) / 1000;
                if (timeLeft == 0) {
                    // caused by truncation of long to int
                    timeLeft = 1;
                }
                throw new UnavailableException(unavailableMessage, timeLeft);
            }
        }

        try {
            // PK02277: Load target if it is null
            if (target == null)
                load();
            try {
                if (notify)
                    evtSource.onServletStartService(evt);
            } catch (Throwable th) {
                // LIBERTY
                nServicing.getAndIncrement();
                throw th;
            }
            // LIBERTY
            nServicing.getAndIncrement();

            if (notify) {
                long curLastAccessTime = System.currentTimeMillis(); // 266936,
                                                                     // bad
                                                                     // webAppModule
                                                                     // response
                                                                     // time
                                                                     // from PMI
                                                                     // (use
                                                                     // local
                                                                     // method
                                                                     // variable)
                lastAccessTime = curLastAccessTime;
                target.service(req, res);
                long endTime = System.currentTimeMillis();
                evt.setResponseTime(endTime - curLastAccessTime);
                evtSource.onServletFinishService(evt);
            } else
                target.service(req, res);
        } catch (UnavailableException e) {
            //PM98245 Start
            if(destroyServletonServiceUnavailableException) {           
                // save the UE is thrown from service.
                WebContainerRequestState UEServiceRequestState = WebContainerRequestState.getInstance(true);   
                if (UEServiceRequestState.getAttribute("UEinInit") == null)
                    UEServiceRequestState.setAttribute("UEinService", true);
            }//PM98245 End
            throw e;
        } catch (IOException ioe) {
           
            if((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) && ioe.getMessage()!= null && ioe.getMessage().contains("SRVE0918E")){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                    logger.logp(Level.FINE, CLASS_NAME, "service", "Caught BlockingWriteNotAllowedException for servlet [" + getServletName() + "]");
                }
            }
            // begin PK04668 IF THE CLIENT THAT MADE THE SERVLET REQUEST GOES
            // DOWN,THERE IS WAS.webcontainer
            else if (ioe instanceof ClosedConnectionException) { // do not log as errors since this will fill logs too quickly.
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "service", "Caught ClosedConnectionException for servlet [" + getServletName() + "]");
                }
            } else {
                // Delegate to ApplicationErrorUtils, since we don't have the right tc 
                ApplicationErrorUtils.issueAppExceptionMessage("SRVE0777E", ioe);
            }
            // end PK04668 IF THE CLIENT THAT MADE THE SERVLET REQUEST GOES
            // DOWN,THERE IS WAS.webcontainer

            // defect 156072.1
            if (notify) {
                ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), ioe);
                evtSource.onServletServiceError(errorEvent);

                evtSource.onServletFinishService(evt);
            }

            throw ioe;
        } catch (ServletException e) {
            if (!suppressServletExceptionLogging) {        
                ApplicationErrorUtils.issueAppExceptionMessage("SRVE0777E", e);
             }

            // defect 156072.1
            if (notify) {
                ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
                evtSource.onServletServiceError(errorEvent);

                evtSource.onServletFinishService(evt);
            }

            throw e;
        } catch (UnsatisfiedLinkError e) {
            logger.logp(Level.SEVERE, CLASS_NAME, "service", "Place.servlet.class.on.classpath.of.the.application.server",
                    new Object[] { getServletName(), getWebApp().getApplicationName(), e });

            // defect 156072.1
            if (notify) {
                ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
                evtSource.onServletServiceError(errorEvent);

                evtSource.onServletFinishService(evt);
            }

            throw new ServletException(e);
        } catch (RuntimeException e) {
            if(!WCCustomProperties.SUPPRESS_LOGGING_SERVICE_RUNTIME_EXCEP){ //739806 ,PM79934 Start
                ApplicationErrorUtils.issueAppExceptionMessage("SRVE0777E", e);
            }
            else{
                //logs only if trace enabled
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){                                        
                    logger.logp(Level.FINE, CLASS_NAME,"service", "Exception thrown while servicing the application servlet -->" + e.toString());                   
                }
            }
            //739806 ,PM79934 End  
            if (notify) {
                ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
                evtSource.onServletServiceError(errorEvent);

                evtSource.onServletFinishService(evt);
            }

            throw e;
        } catch (Throwable e) {
            //logger.logp(Level.SEVERE, CLASS_NAME, "service", "Uncaught service() exception thrown by servlet {0}: {2}", new Object[] {
            //        getServletName(), getWebApp().getApplicationName(), e });
            //com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.ServletInstance.service", "290", this);

            ApplicationErrorUtils.issueAppExceptionMessage("SRVE0777E", e);
            
            if (notify) {
                evtSource.onServletFinishService(evt);
                ServletErrorEvent errorEvent = new ServletErrorEvent(this, getServletContext(), getServletName(), servletConfig.getClassName(), e);
                evtSource.onServletServiceError(errorEvent);
            }

            throw new ServletErrorReport(e);
        } finally {
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
            if (reqState != null && reqState.getAttribute("webcontainer.resetAsyncStartedOnExit") != null){
                ((SRTServletRequest) ServletUtil.unwrapRequest(req)).setAsyncStarted(false);
                reqState.removeAttribute("webcontainer.resetAsyncStartedOnExit");
            }

            nServicing.getAndDecrement();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                logger.exiting(CLASS_NAME, "service");
        }
    }

    /**
     * Method initialize() is called by the creator of the ServletWrapper
     * object. The creator constructs the object and immediately initializes it
     * with the ServletConfig object. This method is meant to be called only
     * from the *outside* and specifically by an ExtensionProcessor instance,
     * and never by this class, or its subclasses.
     * 
     * @param config
     */
    public void initialize(IServletConfig config) throws Exception {
        this.servletConfig = config;
        webAppInvocationCollaborators = context.getWebAppInvocationCollaborators();
    }

    public void loadOnStartupCheck() throws Exception {
        if (servletConfig.isLoadOnStartup()) {
            // System.out.println("Loading at startup
            // "+servletConfig.getServletName());
            try {
                loadServlet();
            } catch (UnavailableException ue) {
                handleUnavailableException(ue, false); //PM01373
                // PK76117 Start
                if (discernUnavailableServlet) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Create WebContainerUnavailableException");
                    throw WebContainerUnavailableException.create(ue);
                }
                // PK76117 End
                throw ue;
            }
            init(servletConfig);
        }
    }

    public void load() throws Exception {
    	 if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
             logger.entering(CLASS_NAME, "load"); 
        loadServlet();
        if (target!=null) {
        init(servletConfig);
        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)){
        	logger.logp(Level.FINE,CLASS_NAME, "load","unable to load servlet's target so skipping init"); 
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "load"); 
    }

    /*
     * Method unload
     */
    public void unload() throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "unload className-->[" + servletConfig.getClassName() + "], servletName[" + servletConfig.getServletName()
                    + "]"); // 569469
        // logger.logp(Level.FINE, CLASS_NAME,"unload",
        // "entry className-->["+servletConfig.getClassName()+"], servletName["+servletConfig.getServletName()+"]");
        // // PK26183

        if (state == UNINITIALIZED_STATE) {
            return;
        }

        try {
            doDestroy();
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.servlet.ServletWrapper.unload", "1511", this);
            logger.logp(Level.SEVERE, CLASS_NAME, "unload", "Exception.occured.during.servlet.unload", th);
        } finally {
            /*
             * 125087: Do not explicitly null out the servlet instance or its classloader.  These should
             * be GCed when the wrapper itself is garbage collected.
             */ 
            //target = null;
            //targetLoader = null;
            invalidateCacheWrappers();
            state = UNINITIALIZED_STATE;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.exiting(CLASS_NAME, "unload"); // 569469

    }

    /**
     * Method loadServlet.
     */
    // begin 280649 SERVICE: clean up separation of core and shell
    // WASCC.web.webcontainer : reuse with other ServletWrapper impls.
    // private synchronized Servlet loadServlet() throws Exception {
    protected synchronized Servlet loadServlet() throws Exception {
        // end 280649 SERVICE: clean up separation of core and shell
        // WASCC.web.webcontainer : reuse with other ServletWrapper impls.
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "loadServlet, className-->[" + servletConfig.getClassName() + "], servletName["
                    + servletConfig.getServletName() + "]"); // 569469
        // logger.logp(Level.FINE, CLASS_NAME,"loadServlet",
        // "entry className-->["+servletConfig.getClassName()+"], servletName["+servletConfig.getServletName()+"]");
        if (target != null) {
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
					logger.exiting(CLASS_NAME,"loadServlet, Found target for className-->["+servletConfig.getClassName()+"], servletName["+servletConfig.getServletName()+"]"); //PM01373
            return target;
        }

        Servlet servlet = servletConfig.getServlet();
        if (servlet != null) {
        	target = servlet;
            return target;
        }

        // load the servlet
        final Class<? extends Servlet> servletClass = servletConfig.getServletClass();
        final String className = servletConfig.getClassName();
        final String servletName = servletConfig.getServletName();

        if (className == null) {
            logger.logp(Level.WARNING, CLASS_NAME, "run", "servlet.classname.is.null", new Object []{servletName});
            return null;
        }
        
        ClassLoader origClassLoader = null;

        Object token = ThreadIdentityManager.runAsServer();
        try {

            origClassLoader = ThreadContextHelper.getContextClassLoader();

            final ClassLoader loader;
            if (targetLoader == null) {
                loader = context.getClassLoader();
                setTargetClassLoader(loader);
            } else {
                loader = targetLoader;
            }

            if (loader != origClassLoader) {
                ThreadContextHelper.setClassLoader(loader);
            } else {
                origClassLoader = null;
            }

            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws ServletException {
                    try {
                        Servlet s = null;
                       
                            if (className.equals("com.ibm.ws.webcontainer.servlet.SimpleFileServlet")) {
                                s = context.getSimpleFileServlet();
                                createTarget(s);
                            } else if (className.equals("com.ibm.ws.webcontainer.servlet.DirectoryBrowsingServlet")) {
                                s = context.getDirectoryBrowsingServlet();
                                createTarget(s);
                            } else if (servletClass != null) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                                    logger.logp(Level.FINE, CLASS_NAME, "loadServlet:run", "Use CDI to create servlet from servletClass:" + servletClass);
                                }
                                createTarget(servletClass);
                            } else {
                                // If a serialized version of the servlet exists create a new object from it by using Beans.instantiate.
                                // otherwise use a ManagedObject factory to create it for cdi 1.2 support.
                                String serializedName = className.replace('.','/').concat(".ser");
                                // Try to find a serialized object with this name
                                if (loader.getResourceAsStream(serializedName)!=null) {
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                                        logger.logp(Level.FINE, CLASS_NAME, "loadServlet:run", "A serialized object exists. Create an instance of it : " + serializedName);
                                    }
                                    s = (javax.servlet.Servlet) Beans.instantiate(loader, className);
                                    createTarget(s);
                                } else {
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                                        logger.logp(Level.FINE, CLASS_NAME, "loadServlet:run", "use CDI to create servlet from className:" + className);
                                    }
                                    Class<?> sclass = loader.loadClass(className);
                                    createTarget(sclass);  
                                }    
                            }
                    } catch (ClassNotFoundException e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "208", this);
                        // log the error
                        logger.logp(Level.SEVERE, CLASS_NAME, "run", "classnotfoundexception.loading.servlet.class", e);
                        throw new UnavailableException(MessageFormat.format(nls.getString("Servlet.Could.not.find.required.servlet.class",
                                "Servlet [{0}]: Could not find required servlet - {1}"), new Object[] { className, e.getMessage() }));
                    } catch (ClassCastException e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "213", this);
                        throw new UnavailableException(MessageFormat.format(nls.getString("Servlet.not.a.servlet.class",
                                "Servlet [{0}]: not a servlet class"), new Object[] { className }));
                    } catch (NoClassDefFoundError e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "218", this);
                        throw new UnavailableException(
                                MessageFormat.format(nls.getString("Servlet.was.found.but.is.missing.another.required.class",
                                        "Servlet [{0}]: {1} was found, but is missing another required class.\n"), new Object[] { servletName,
                                        className })
                                        + nls
                                                .getString(
                                                        "This.error.implies.servlet.was.originally.compiled.with.classes.which.cannot.be.located.by.server",
                                                        "This error typically implies that the servlet was originally compiled with classes which cannot be located by the server.\n")
                                        + nls.getString("Check.your.classpath.ensure.all.classes.present",
                                                "Check your classpath to ensure that all classes required by the servlet are present.\n")
                                        + nls
                                                .getString(
                                                        "be.debugged.by.recompiling.the.servlet.using.only.the.classes.in.the.application's.runtime.classpath",
                                                        "\n  This problem can be debugged by recompiling the servlet using only the classes in the application's runtime classpath\n")
                                        + MessageFormat.format(nls.getString("Application.classpath", "Application classpath=[{0}]"),
                                                new Object[] { context.getClasspath() }));
                    } catch (ClassFormatError e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "227", this);
                        throw new UnavailableException(
                                MessageFormat.format(nls.getString("Servlet.found.but.corrupt", "Servlet [{0}]: {1} was found, but is corrupt:\n"),
                                        new Object[] { servletName, className })
                                        + nls.getString("class.resides.in.proper.package.directory",
                                                "1. Check that the class resides in the proper package directory.\n")
                                        + nls
                                                .getString("classname.defined.in.server.using.proper.case.and.fully.qualified.package",
                                                        "2. Check that the classname has been defined in the server using the proper case and fully qualified package.\n")
                                        + nls.getString("class.transfered.using.binary.mode",
                                                "3. Check that the class was transfered to the filesystem using a binary tranfer mode.\n")
                                        + nls.getString("class.compiled.using.proper.case",
                                                "4. Check that the class was compiled using the proper case (as defined in the class definition).\n")
                                        + nls.getString("class.not.renamed.after.compiled",
                                                "5. Check that the class file was not renamed after it was compiled."));
                    } catch (IOException e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "1349", this);
                        throw new ServletException(
                                MessageFormat.format(nls.getString("IOException.loading.servlet"),
                                        new Object[] { servletName, className }), e);
                    }  
                    catch (InjectionException ie) {	//596191 Start	
                        com.ibm.ws.ffdc.FFDCFilter.processException(ie, "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "228", this);
                        logger.logp(Level.SEVERE, CLASS_NAME, "init",
                                    "Servlet.found.but.injection.failure",
                                    new Object[] { servletName, className, ie.getLocalizedMessage() });
                        // Liberty - convert to UnavailableException, so a 404, instead of a 500, error will be returned to the client
                        String string = nls.getString("Servlet.found.but.injection.failure",
                                        "SRVE0319E: For the [{0}] servlet, {1} servlet class was found, but a resource injection failure has occurred. {2}");
                        String s = MessageFormat.format(string, new Object[] { servletName, className, ie.getLocalizedMessage() });
                        throw new UnavailableException(s);
                     }//596191 End 
                    catch (IllegalAccessException e) {
                    	com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "1349", this);
                        throw new ServletException(
                                MessageFormat.format(nls.getString("IllegalAccessException.loading.servlet"),
                                        new Object[] { servletName, className }), e);
                    } catch (InstantiationException e) {
                    	com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                "com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet", "1349", this);
                        throw new ServletException(
                                MessageFormat.format(nls.getString("InstantiationException.loading.servlet"),
                                        new Object[] { servletName, className }), e);
                    } 
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.exiting(CLASS_NAME, "loadServlet"); // 569469

                    return null;
                }
            });

        } catch (PrivilegedActionException e) {
            Throwable th = e.getCause();
            if (th instanceof Exception)
                throw ((Exception) th);
            throw new Exception(th);
        } catch (Throwable t) {
            throw new Exception(t);
        } finally {
            ThreadIdentityManager.reset(token);

            if (origClassLoader != null) {
                final ClassLoader fOrigClassLoader = origClassLoader;
                ThreadContextHelper.setClassLoader(fOrigClassLoader);
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.exiting(CLASS_NAME, "loadServlet"); // 569469

        }

        return target;
    }

    private ServletEvent getServletEvent() {
        if (event == null) {
            event = new ServletEvent(this, servletConfig.getServletContext(), servletConfig.getServletName(), servletConfig.getClassName());
        }

        return event;
    }

    //New parameter added for init() PM01373
	private synchronized void handleUnavailableException(UnavailableException e, boolean isInit) throws UnavailableException
	 {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "handleUnavailableException", e); // 569469
        // logger.logp(Level.FINE, CLASS_NAME,"handleUnavailableException", "",
        // e);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "handleUnavailableException", "UnavailableException was thrown by servlet: " + getServletName()
                    + " reason:" + e.getMessage());
        if (state == UNAVAILABLE_PERMANENTLY_STATE) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.exiting(CLASS_NAME, "handleUnavailableException", "state is already permanently unavailable, throw ue"); // 569469
            // logger.logp(Level.FINE, CLASS_NAME,"handleUnavailableException",
            // "state is already permanently unavailable, throw ue");
            throw new UnavailableException(unavailableMessage);
        } else if (e.isPermanent()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.logp(Level.FINE, CLASS_NAME, "handleUnavailableException", "exception is permanent");
            if (state == AVAILABLE_STATE) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "handleUnavailableException", "state is available so destroy the servlet");
                /*
                 * This means the UnavailableException is thrown from the
                 * service method so we should destroy this servlet.
                 */
                // destroy();
                // destroy does not remove from the requestMapper which can lead
                // to a 500 error code on a subsequent request
                if (!context.removeServlet(getServletName()))
                    ;
                {
                    // In the case that there are no servlet mappings, we still
                    // need to destroy
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "handleUnavailableException",
                                "removeServlet didn't destroy. destroy from handleUnavailableException");
                    destroy();
                }
            }
            unavailableMessage = e.getMessage();
            if(isInit){											
				this.setUninitialize();  // PM01373
			}
			else{				
            setUnavailable();
			}
        } else {
            int secs = e.getUnavailableSeconds();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.logp(Level.FINE, CLASS_NAME, "handleUnavailableException", "ue is not permanent, unavailable secs -->[" + secs + "]");
            if (secs > 0) {
                long time = System.currentTimeMillis() + ((secs) * 1000);
                if (time > unavailableUntil) {
                    unavailableMessage = e.getMessage();
                    // only set the time if the new time is farther away than
                    // the old time.
                    setUnavailableUntil(time, isInit);		//PM01373
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.exiting(CLASS_NAME, "handleUnavailableException"); // 569469
    }

    /**
     * Method setUnavailableUntil.
     * 
     * @param time
	 * @param isInit
     */
	private void setUnavailableUntil(long time, boolean isInit) //PM01373
	{
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setUnavailableUntil", "setUnavailableUntil() : " + time);
        if(isInit){
			state = UNINITIALIZED_STATE;  //PM01373
		}
		else {
        state = UNAVAILABLE_STATE;
		}
        unavailableUntil = time;
        evtSource.onServletUnavailableForService(getServletEvent());
    }
	
	// PM01373 Start
	/**
	 * Puts a servlet into uninitialize state.
	 * 
	 */
	protected void setUninitialize()
	{
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    		logger.logp(Level.FINE, CLASS_NAME,"setUninitialized ","" + this.toString());    

		state = UNINITIALIZED_STATE;
	}
	// PM01373 End

    /**
     * Puts a servlet out of service. This will destroy the servlet (if it was
     * initialized) and then mark it permanently unavailable
     */
    protected void setUnavailable() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setUnavailable", "" + this.toString()); // PK26183

        state = UNAVAILABLE_PERMANENTLY_STATE;
        evtSource.onServletUnavailableForService(getServletEvent());
    }

    private void setAvailable() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "setAvailable " + this.toString()); // 569469
        evtSource.onServletAvailableForService(getServletEvent());
        // System.out.println("Setting as available");
        state = AVAILABLE_STATE;
        unavailableMessage = null;
        unavailableUntil = -1;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.exiting(CLASS_NAME, "setAvailable"); // 569469
    }

    // 263020 Make protected since we must override destroy now
    // which makes a call to invalidateCacheWrappers
    protected synchronized void invalidateCacheWrappers() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.entering(CLASS_NAME, "invalidateCacheWrappers"); // 569469

        if (cacheWrappers != null) {
            // invalidate all the cache wrappers that wrap this target.
            Iterator i = cacheWrappers.iterator();

            while (i.hasNext()) {
                ServletReferenceListener w = (ServletReferenceListener) i.next();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "invalidateCacheWrappers", "servlet reference listener -->[" + w + "]");
                w.invalidate();
            }

            cacheWrappers = null;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
            logger.exiting(CLASS_NAME, "invalidateCacheWrappers"); // 569469
    }

    /**
     * @see com.ibm.ws.container.Container#addSubContainer(Container)
     */
    public void addSubContainer(Container con) {

    }

    /**
     * @see com.ibm.ws.container.Container#getParent()
     */
    public Container getParent() {
        return context;
    }

    /**
     * @see com.ibm.ws.container.Container#getSubContainer(String)
     */
    public Container getSubContainer(String name) {
        return null;
    }

    /**
     * @see com.ibm.ws.container.Container#initialize(Configuration)
     */
    public void initialize(Configuration config) {
    }

    /**
     * @see com.ibm.ws.container.Container#isActive()
     */
    public boolean isActive() {
        return (state != UNINITIALIZED_STATE);
    }

    /**
     * @see com.ibm.ws.container.Container#isAlive()
     */
    public boolean isAlive() {
        return (state != this.UNAVAILABLE_PERMANENTLY_STATE && state != this.UNAVAILABLE_STATE);
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * @see com.ibm.ws.container.Container#removeSubContainer(String)
     */
    public Container removeSubContainer(String name) {
        return null;
    }

    /**
     * @see com.ibm.ws.container.Container#start()
     */
    public void start() {
    }

    /**
     * @see com.ibm.ws.container.Container#stop()
     */
    public void stop() {
    }

    /**
     * @see com.ibm.ws.container.Container#subContainers()
     */
    public Iterator subContainers() {
        return null;
    }

    /**
     * @see com.ibm.ws.core.CommandSequence#execute(IWCCRequest, IWCCResponse)
     */
    public void execute(IExtendedRequest req, IExtendedResponse res) {
    }

    /**
     * @see com.ibm.ws.core.RequestProcessor#handleRequest(Request, Response)
     */
    /**
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    public ServletContext getServletContext() {
        return servletConfig.getServletContext();
    }

    public WebApp getWebApp() {
        return context;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletName()
     */
    public String getServletName() {
        if (servletConfig == null)
            return null;
        return servletConfig.getServletName();
    }

    private IServletWrapper getMimeFilterWrapper(String mimeType) {
        try {
            if (mimeType.indexOf(";") != -1)
                mimeType = mimeType.substring(0, mimeType.indexOf(";"));
            return context.getMimeFilterWrapper(mimeType);
        } catch (ServletException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.servlet.ServletWrapper.getMimeFilterWrapper", "834",
                    this);
            context.logError("Failed to load filter for mime-type: " + mimeType, e);
            return null;
        }
    }

    // begin pq50381: part 3
    private void transferHeadersFromPrevChainedResp(ChainedResponse current, ChainedResponse previous) {

        Iterable<String> headers = previous.getHeaderNames();
        for (String name:headers) {
            String value = (String) previous.getHeader(name);
            current.setHeader(name, value);
        }
        Cookie[] theCookies = previous.getCookies();
        for (int i = 0; i < theCookies.length; i++) {
            Cookie currCookie = theCookies[i];
            current.addCookie(currCookie);
        }
    }

    // end pq50381: part 3
    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.ws.webcontainer.util.CacheTarget#addCacheWrapper(com.ibm.ws.
     * webcontainer.util.CacheWrapper)
     */
    public synchronized void addServletReferenceListener(ServletReferenceListener listener) {
        if (this.cacheWrappers == null) {
            cacheWrappers = new ArrayList();
        }
        this.cacheWrappers.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletWrapper#setTargetClassLoader
     * (java.lang.ClassLoader)
     */
    public void setTargetClassLoader(ClassLoader loader) {
        this.targetLoader = loader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.servlet.IServletWrapper#getTarget()
     */
    public Servlet getTarget() {
        return target;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletWrapper#getTargetClassLoader()
     */
    public ClassLoader getTargetClassLoader() {
        return targetLoader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletWrapper#setTarget(javax.servlet
     * .Servlet)
     */
    public void setTarget(Servlet target) {
        this.target = target;
    }

    public String toString() {
        Iterable<String> mappings = null;
        // PK76117 Start
        if (state == UNAVAILABLE_PERMANENTLY_STATE) {
            return "ServletWrapper[Servlet is permanently unavailable]";
        }
        // PK76117 End
        if (servletConfig != null)
            mappings = servletConfig.getMappings();
        return "ServletWrapper[" + getServletName() + ":" + mappings + "]";
    }

    
    @Override
    // Do not sync for performance reasons.  It is ok to print this warning more than once, just trying to limit it for the most part.
    public boolean hitWarningStatus() {
        if (warningStatusSet == false) {
            warningStatusSet = true;
            return true;
        }
        return false;
    }
    
    // begin 268176 Welcome file wrappers are not checked for resource existence
    // WAS.webcontainer
    /**
     * Returns whether the requested wrapper resource exists.
     */
    public boolean isAvailable() {
        return true;
    }

    // end 268176 Welcome file wrappers are not checked for resource existence
    // WAS.webcontainer

    protected synchronized void createTarget(Servlet s) throws InjectionException{ //596191
        if (s instanceof SingleThreadModel) {
            isSTM = true;
            servletConfig.setSingleThreadModelServlet(isSTM);
            target = new SingleThreadModelServlet(s.getClass());
        } else {
            target = s;
        }
    }

    protected void createTarget(Class<?> Klass) throws InjectionException, InstantiationException, IllegalAccessException
    {
        createTarget((Servlet)Klass.newInstance());
    }

    // PK80340 Start
    public boolean isDefaultServlet() {
        boolean result = false;
        if (servletConfig != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "isDefaultServlet", " : mappings:" + servletConfig.getMappings());
            ;
            for (String curMapping:servletConfig.getMappings()){
                if (curMapping.equals("/"))
                    return true;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() & logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "isDefaultServlet", " result=" + result);

        return false;
    }

    // PK80340 End

    public boolean isInternal() {
        return servletConfig.isInternal();
    }
    
    //modifies the target for SingleThreadModel & caching
    public void modifyTarget(Servlet s) {
        //do nothing here
        //can be overridden
    }

    
    public void startRequest(ServletRequest request) {
    	
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
			logger.entering(CLASS_NAME,"startRequest","state = " + getStateString(state));
		}
    	
        HttpServletRequest httpServletReq = (HttpServletRequest) ServletUtil.unwrapRequest(request,HttpServletRequest.class);
        
        String method = httpServletReq.getMethod();

		boolean checkHeadRequest = (!defaultHeadRequestBehavior && method.equals("HEAD"));
		boolean checkTraceRequest = (!defaultTraceRequestBehavior && method.equals("TRACE"));
		
    	// Only check for default implementation if request is doTrace or doHead.
    	// Don't synchronize to prevent a bottleneck - result should be the same 
    	// even if two threads run at the same time.
		if ((checkHeadRequest || checkTraceRequest)) {
			if (checkedForDefaultMethods==null){
			    checkForDefaultImplementation();
			    checkedForDefaultMethods = new Boolean(Boolean.TRUE);
			}
			if (checkHeadRequest && this.defaultHeadMethodInUse) {
				request.setAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod","HEAD");	
			} else if(checkTraceRequest && this.defaultTraceMethodInUse) {
				request.setAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod","TRACE");
			}
		}
				
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
			logger.exiting(CLASS_NAME,"startRequest"," method = " + method);
		}

		
    }
	
    public void finishRequest(ServletRequest request) {
    	// remove attribute, if it was ever added (easier to just try the remove than work
    	// out if it is set.
		request.removeAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");				
	}
   
	public void checkForDefaultImplementation() {
				
		
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
			logger.entering(CLASS_NAME,"checkForDefaultMethods","already checked = " + this.checkedForDefaultMethods);
		}
			
	    if (checkedForDefaultMethods == null ) {
			    	
            final String targetClassName = servletConfig.getClassName();
            final ClassLoader targetClassLoader = context.getClassLoader();
		
            try {
		        AccessController.doPrivileged(new PrivilegedExceptionAction() {
			    public Object run()
			    {
		                Class targetClass;
		                try {
		                    targetClass = Class.forName(targetClassName, false, targetClassLoader);
		                } catch (java.lang.ClassNotFoundException exc ) {
		    			    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
		    			        logger.logp(Level.FINE, CLASS_NAME,"checkForDefaultMethods","Class not found when checking for default implementations. Class = " + targetClassName);
			                return null;	
		                }
		
				        if (!defaultHeadRequestBehavior)
					        defaultHeadMethodInUse = checkForDefaultImplementation(targetClass, "doHead", PARAMS_HEAD_TRACE);
		                if (!defaultTraceRequestBehavior)
		                    defaultTraceMethodInUse = checkForDefaultImplementation(targetClass, "doTrace", PARAMS_HEAD_TRACE);
		                return null;
			        }   					
		        });
            } catch (PrivilegedActionException exc) {
			    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
			        logger.logp(Level.FINE, CLASS_NAME,"checkForDefaultMethods","PrivelegedActionException when checking for default implementations. Class = " + targetClassName);
            }
		}
				
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
	    	logger.exiting(CLASS_NAME,"checkForDefaultMethods","Default's in use? doHead = " + (defaultHeadRequestBehavior ? "not checked" : (defaultHeadMethodInUse ? "true" : "false")) + ", doTrace = " + (defaultTraceRequestBehavior ? "not checked" : (defaultTraceMethodInUse ? "true" : "false")));;
		
	    return;
	}
		
	// PK83258 Add method checkDefaultImplementation
	private boolean checkForDefaultImplementation(Class checkClass, String checkMethod, Class[] methodParams){
		
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
	    	logger.exiting(CLASS_NAME,"checkForDefaultImplementation","Method : " + checkMethod + ", Class : " + checkClass.getName());
		
		
		boolean defaultMethodInUse=true;
		
		while (defaultMethodInUse && checkClass!=null && !checkClass.getName().equals("javax.servlet.http.HttpServlet")) {
		    try {  
		    						       
		        checkClass.getDeclaredMethod(checkMethod, methodParams);
			
			    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
			    	logger.logp(Level.FINE, CLASS_NAME,"checkForDefaultImplementation","Class implementing " + checkMethod + " is " + checkClass.getName());
			
			    defaultMethodInUse=false;
			    break;
			     
		    } catch (java.lang.NoSuchMethodException exc) {    	
			    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
			    	logger.logp(Level.FINE, CLASS_NAME,"checkForDefaultImplementation",checkMethod + " is not implemented by class " + checkClass.getName()); 
		    } catch (java.lang.SecurityException exc) {
			    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
			    	logger.logp(Level.FINE, CLASS_NAME,"checkForDefaultImplementation","Cannot determine if " + checkMethod + " is implemented by class " + checkClass.getName());				    	
		    }
		    
		    checkClass = checkClass.getSuperclass();
	    }  
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))  //306998.14
	    	logger.exiting(CLASS_NAME,"checkForDefaultImplementation","Result : " + defaultMethodInUse);
	    
        return defaultMethodInUse;
	}

	private String getStateString(byte state){
	    switch(state){
            case AVAILABLE_STATE: 
                return "Available";
	        case UNINITIALIZED_STATE: 
	            return "Uninitialized";
	        case UNAVAILABLE_STATE: 
	            return "Unavailable";
	        case UNAVAILABLE_PERMANENTLY_STATE:
	            return "Permanently unavailable";
	        default:
	            return ""+state;
	    }
	}

}
