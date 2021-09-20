/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.collaborator;

import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ejs.j2c.HandleList;
import com.ibm.ws.webcontainer.collaborator.ConnectionCollaborator;
import com.ibm.ws.webcontainer.collaborator.WebAppNameSpaceCollaborator;
import com.ibm.ws.webcontainer.collaborator.WebAppSecurityCollaborator;
import com.ibm.ws.webcontainer.collaborator.WebAppTransactionCollaborator;
import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.servlet.FileServletWrapper;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public abstract class CollaboratorHelper implements ICollaboratorHelper {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.collaborator");
    private static final String CLASS_NAME = "com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper";
    protected WebApp webApp;
    protected IWebAppSecurityCollaborator securityCollaborator;
    protected IWebAppNameSpaceCollaborator nameSpaceCollaborator;
    protected IWebAppTransactionCollaborator transactionCollaborator;
    protected IConnectionCollaborator connectionCollaborator;
    public static final EnumSet<CollaboratorInvocationEnum> allCollabEnum = EnumSet.allOf(CollaboratorInvocationEnum.class); // The
                                                                                                                       // default
                                                                                                                       // would
                                                                                                                       // be
                                                                                                                       // to
                                                                                                                       // execute
                                                                                                                       // all
                                                                                                                       // the
                                                                                                                       // collaborators
    boolean servlet_23_or_greater = true;
    
    public static final boolean DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR = WCCustomProperties.DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR;       

    public CollaboratorHelper(WebApp webApp) {
        this.webApp = webApp;
        if (webApp != null) {
            servlet_23_or_greater = (webApp.getEffectiveMajorVersion() > 2) || (webApp.getEffectiveMinorVersion() >= 3);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
                logger.logp(Level.FINE, CLASS_NAME, "CollaboratorHelper", "servlet_23_or_greater->" + servlet_23_or_greater);
            }
        }
    }

    // moved from com.ibm.ws.wswebcontainer.security.SecurityCollaboratorHelper
    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * processSecurityPreInvokeException
     * (com.ibm.wsspi.webcontainer.security.SecurityViolationException,
     * com.ibm.wsspi.webcontainer.RequestProcessor,
     * javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse,
     * com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext,
     * com.ibm.ws.webcontainer.webapp.WebApp, java.lang.String)
     */
    public Object processSecurityPreInvokeException(SecurityViolationException sve, RequestProcessor requestProcessor, HttpServletRequest request,
            HttpServletResponse response, WebAppDispatcherContext dispatchContext, WebApp context, String name) throws ServletErrorReport {

        Object secObject = null;

        // begin pq56177

        secObject = sve.getWebSecurityContext();
        int sc = sve.getStatusCode(); // access status code directly. Is
                                      // SC_FORBIDDEN the default?
        // if (sc==null){
        // if
        // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
        // (Level.FINE) == true)
        // {
        // logger.logp(Level.FINE,
        // CLASS_NAME,"processSecurityPreInvokeException",
        // "webReply is null, default to 403 status code");
        // }
        // sc = HttpServletResponse.SC_FORBIDDEN;
        // }
        Throwable cause = sve.getCause();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
            logger.entering(CLASS_NAME, "processSecurityPreInvokeException");
            logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException",
                    "SecurityCollaboratorHelper.processPreInvokeException():  WebSecurityException thrown (" + sve.toString()
                            + ").  HTTP status code: " + sc + "resource : " + name);

        } // end if

        if (sc == HttpServletResponse.SC_FORBIDDEN) {
            // If the user has defined a custom error page for
            // SC_FORBIDDEN (HTTP status code 403) then send
            // it to the client ...
            if (context.isErrorPageDefined(sc) == true) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
                    logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException", "Using user defined error page for HTTP status code "
                            + sc);
                }

                WebAppErrorReport wErrorReport = new WebAppErrorReport(cause);
                wErrorReport.setErrorCode(sc);
                context.sendError(request, response, wErrorReport);
            } else {
                // ... otherwise, use the one provided by the
                // SecurityCollaborator
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
                    logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException",
                            "Using default security error page for HTTP status code " + sc);
                }

                try {
                    securityCollaborator.handleException(request, response, cause);
                } catch (Exception ex) {
                    if (requestProcessor != null) {
                        throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
                    } else {
                        throw WebAppErrorReport.constructErrorReport(ex, name);
                    }
                }
                // reply.sendError(wResp);
            } // end if-else
        } else if (sc == HttpServletResponse.SC_UNAUTHORIZED) {
            // Invoking handleException will add the necessary headers
            // to the response ...
            try {
                securityCollaborator.handleException(request, response, cause);
            } catch (Exception ex) {
                if (requestProcessor != null) {
                    throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
                } else {
                    throw WebAppErrorReport.constructErrorReport(ex, name);
                }
            }

            // ... if the user has defined a custom error page for
            // SC_UNAUTHORIZED (HTTP status code 401) then
            // send it to the client
            if (context.isErrorPageDefined(sc) == true) {
            	
            	WebContainerRequestState reqState = com.ibm.wsspi.webcontainer.WebContainerRequestState.getInstance(false);
    			boolean errorPageAlreadySent = false;
    			if (reqState!=null) {
    				String spnegoErrorPageAlreadySent = (String)reqState.getAttribute("spnego.error.page");
    				reqState.removeAttribute("spnego.error.page");
    				if (spnegoErrorPageAlreadySent != null && spnegoErrorPageAlreadySent.equalsIgnoreCase("true")) {  					    		
    					errorPageAlreadySent = true; 
    				    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true) {
    				         logger.logp(Level.FINE, CLASS_NAME,"processSecurityPreInvokeException", "skip error page - already created by spego code");
    				    }	
    				}    
    			} 

    			if (!errorPageAlreadySent) {

    				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
    					logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException", "Using user defined error page for HTTP status code "
    							+ sc);
    				}

    				WebAppErrorReport wErrorReport = new WebAppErrorReport(cause);
    				wErrorReport.setErrorCode(sc);
    				context.sendError(request, response, wErrorReport);
    				
    			}	
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
                    logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException",
                            "Using default security error page for HTTP status code " + sc);
                }
                // reply.sendError(wResp); comment-out 140967
            }

        } else {
            // Unexpected status code ... not SC_UNAUTHORIZED or SC_FORBIDDEN
            if ((logger.isLoggable(Level.FINE) == true)) {
                logger.logp(Level.FINE, CLASS_NAME, "processSecurityPreInvokeException", "HTTP status code: " + sc);
            }
            try {
                securityCollaborator.handleException(request, response, cause);
            } catch (Exception ex) {
                if (requestProcessor != null) {
                    throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
                } else {
                    throw WebAppErrorReport.constructErrorReport(ex, name);
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) == true) {
            logger.exiting(CLASS_NAME, "processSecurityPreInvokeException");
        }
        return secObject;
    }

    // end PK10057

    public IWebAppNameSpaceCollaborator getWebAppNameSpaceCollaborator() {
        if (nameSpaceCollaborator == null)
            nameSpaceCollaborator = new WebAppNameSpaceCollaborator();
        return nameSpaceCollaborator;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * createConnectionCollaboratorHelper()
     */
    public IConnectionCollaborator getWebAppConnectionCollaborator() {
        if (connectionCollaborator == null)
            connectionCollaborator = new ConnectionCollaborator();
        return connectionCollaborator;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * doInvocationCollaboratorsPreInvoke
     * (com.ibm.ws.webcontainer.spiadapter.collaborator
     * .IInvocationCollaborator[],
     * com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData,
     * javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void doInvocationCollaboratorsPreInvoke(IInvocationCollaborator[] webAppInvocationCollaborators,
            com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd, ServletRequest request, ServletResponse response) {

    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * doInvocationCollaboratorsPostInvoke
     * (com.ibm.ws.webcontainer.spiadapter.collaborator
     * .IInvocationCollaborator[],
     * com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData,
     * javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void doInvocationCollaboratorsPostInvoke(IInvocationCollaborator[] webAppInvocationCollaborators,
            com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd, ServletRequest request, ServletResponse response) {

    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * doInvocationCollaboratorsPreInvoke
     * (com.ibm.ws.webcontainer.spiadapter.collaborator
     * .IInvocationCollaborator[],
     * com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData)
     */
    public void doInvocationCollaboratorsPreInvoke(IInvocationCollaborator[] webAppInvocationCollaborators,
            com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd) {

    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * doInvocationCollaboratorsPostInvoke
     * (com.ibm.ws.webcontainer.spiadapter.collaborator
     * .IInvocationCollaborator[],
     * com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData)
     */
    public void doInvocationCollaboratorsPostInvoke(IInvocationCollaborator[] webAppInvocationCollaborators,
            com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd) {

    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
     * getSecurityCollaborator()
     */
    public IWebAppSecurityCollaborator getSecurityCollaborator() {
        if (securityCollaborator == null)
            securityCollaborator = new WebAppSecurityCollaborator();
        return securityCollaborator;
    }

    public IWebAppTransactionCollaborator getWebAppTransactionCollaborator() {
        if (transactionCollaborator == null) {
            transactionCollaborator = new WebAppTransactionCollaborator();
        }
        return transactionCollaborator;
    }

    public void preInvokeCollaborators(ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException,
            IOException, Exception {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "preInvokeCollaborators");

        com.ibm.ejs.j2c.HandleList _connectionHandleList = new HandleList();
        collabMetaData.setConnectionHandleList(_connectionHandleList);

        WebComponentMetaData cmd = collabMetaData.getComponentMetaData(); // cmd
                                                                          // could
                                                                          // be
                                                                          // null
        HttpServletRequest httpRequest = collabMetaData.getHttpServletRequest();
        HttpServletResponse httpResponse = collabMetaData.getHttpServletResponse();
        boolean isSTM = false;
        IServletConfig servletConfig = collabMetaData.getServletConfig(); // servletConfig
                                                                          // could
                                                                          // be
                                                                          // null
        String servletName = null;
        IServletContext servletContext = collabMetaData.getServletContext();
        IWebAppDispatcherContext dispatchContext = collabMetaData.getWebAppDispatcherContext();
        IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(httpRequest);

        boolean sessionSecurityIntegrationEnabled = false;
        boolean securityEnforced = false;

        RequestProcessor reqProc = collabMetaData.getRequestProcessor();
        
        //do sessionInvoke under the following circumstances
        boolean sessionInvoke = 
                //begin: one of these must be true
                (!WCCustomProperties.IGNORE_SESSION_STATIC_FILE_REQUEST // don't ignore session on static file)? (custom prop default: true)
                    || reqProc == null // reqProc is null aka invoking from WebContainer.java
                    || !(reqProc instanceof FileServletWrapper // reqProc is not an instance of a static file processor
                            || reqProc instanceof DefaultExtensionProcessor)) // reqProc is not an instance of a static file processor
                //end: one of these must be true
                //begin: all of these must be true
                && colEnum != null 
                && colEnum.contains(CollaboratorInvocationEnum.SESSION) // collabEnum tells us to invoke session
                &&(reqProc==null||!reqProc.isInternal()); //not an internal servlet. For RRD to work, we need to wrap the request object before the session
                                                            //preInvoke so that 
                //end: all of these must be true
        
        collabMetaData.setSessionInvokeRequired(sessionInvoke);

        try {
            if (((IServletContextExtended) collabMetaData.getServletContext()).getSessionContext() != null) {
                sessionSecurityIntegrationEnabled = ((IServletContextExtended) collabMetaData.getServletContext()).getSessionContext().getIntegrateWASSecurity();
                if (sessionSecurityIntegrationEnabled) {
                    ((IExtendedRequest) wasreq).setRunningCollaborators(true); // PK01801
                }
            }
            

            int callbacksID = 0;
             DispatcherType dispatchType = dispatchContext.getDispatcherType();
            if (cmd != null) {
                if (dispatchType==DispatcherType.FORWARD || dispatchType==DispatcherType.INCLUDE) {
                    callbacksID = cmd.getCallbacksId();
                } else {
                    callbacksID = cmd.setCallbacksID();
                }
            }

            collabMetaData.setCallbacksID(callbacksID);

            if (servletConfig != null) {
                isSTM = servletConfig.isSingleThreadModelServlet();
                servletName = servletConfig.getServletName();
            }

            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.NAMESPACE)) {
                if (cmd != null)
                    this.nameSpaceCollaborator.preInvoke(cmd);
                else
                    logger.logp(Level.FINE, CLASS_NAME, "preInvokeCollaborators", "no component metadata so namespace will not be preInvoked");
            }
            
            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.CLASSLOADER)) {
                ClassLoader origClassLoader = ThreadContextHelper.getContextClassLoader();
                collabMetaData.setOrigClassLoader(origClassLoader);
                final ClassLoader warClassLoader = servletContext.getClassLoader();
                if (warClassLoader != origClassLoader) {
                    if (logger.isLoggable(Level.FINE)){
                        if(origClassLoader != null) {
                            logger.logp(Level.FINE, CLASS_NAME, "preInvokeCollaborators", "PK26183 re-set class loader from --> " + origClassLoader.toString()
                                        + " ,to --> " + warClassLoader.toString());
                        }
                        else{
                            logger.logp(Level.FINE, CLASS_NAME, "preInvokeCollaborators", "PK26183 re-set class loader from origClassLoader--> null ,to --> " + warClassLoader.toString()); 
                        }
                    }
                    ThreadContextHelper.setClassLoader(warClassLoader);
                } else {
                    origClassLoader = null;
                }
            }

            // TODO: should transactions or connections be invoked if there is
            // no component meta data?
            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.TRANSACTION)) {

                // 113997 - only call the tx collab if Servlet 2.3 or higher
                // LIDB441
                // - test moved inside
                // Transaction collaborator to allow ActivityService function in
                // all
                // servlets

                TxCollaboratorConfig txConfig;
                txConfig = transactionCollaborator.preInvoke(httpRequest, servlet_23_or_greater);

                if (txConfig != null) {
                    txConfig.setDispatchContext(dispatchContext);
                    collabMetaData.setTransactionConfig(txConfig);
                }
            }

            boolean servletRequestListenersNotificationNeeded = true;
            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.SECURITY)) {
                securityEnforced = dispatchContext.isEnforceSecurity() // PK70824
                                  || (httpRequest!=null && httpRequest.getDispatcherType().equals(DispatcherType.ERROR));
                // Invoke notifyServletRequestCreated here because JSR 375 requires that all CDI scopes are available.
                if (securityCollaborator.isCDINeeded()) {
                    // Invoke notifyServletRequestCreated here because JSR 375 requires that all CDI scopes are available. 
                    collabMetaData.setServletRequestCreated(webApp.notifyServletRequestCreated(httpRequest));
                    servletRequestListenersNotificationNeeded = false;
                }
                
                Object securityObject = securityCollaborator.preInvoke(httpRequest, httpResponse, servletName, securityEnforced);
                collabMetaData.setSecurityObject(securityObject); 
            }

            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.CONNECTION)) {
                this.connectionCollaborator.preInvoke(_connectionHandleList, isSTM);
                collabMetaData.setConnectionHandleList(_connectionHandleList);
            }

            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.INVOCATION)) {
                IInvocationCollaborator[] webAppInvocationCollaborators = collabMetaData.getServletContext().getWebAppInvocationCollaborators();
                doInvocationCollaboratorsPreInvoke(webAppInvocationCollaborators, cmd, httpRequest, httpResponse);
            }

            // PK01801 BEGIN
            if (sessionSecurityIntegrationEnabled) {
                ((IExtendedRequest) wasreq).setRunningCollaborators(false); // PK01801
            }
            // PK01801 END
            collabMetaData.setPostInvokeNecessary(true);

            // If a transaction has been started back in the dispatch chain
            // Save a reference to it for checking when the dispatch is
            // complete.
            collabMetaData.setTransaction(getTransaction());

            if (sessionInvoke) {
                dispatchContext.sessionPreInvoke();
            }
            
            //HttpServletRequest javadocs describe it as:
            //Interface for receiving notification events about requests coming into and going out of scope of a web application. 
            if (servletRequestListenersNotificationNeeded) { 
                //Async error handling goes through this path and can set the setServletRequestCreated to false. That can stop the request listener destroy notification in the postInvoke.
                //Set it manually so it won't trigger the notification for listeners' request created.
                WebContainerRequestState reqState = com.ibm.wsspi.webcontainer.WebContainerRequestState.getInstance(false);
                if (DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR && reqState != null && reqState.getAttribute("_invokeAsyncErrorHandling") != null ) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "preInvokeCollaborators", "Async error handling, setting servlet request created manually"); 

                    collabMetaData.setServletRequestCreated(true);      //set manually; this won't trigger the servlet request created listener
                }
                else
                    collabMetaData.setServletRequestCreated(webApp.notifyServletRequestCreated(httpRequest));
            }
            
        } finally {
            if (sessionSecurityIntegrationEnabled) {
                ((IExtendedRequest) wasreq).setRunningCollaborators(false); // PK01801
            }
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME,"preInvokeCollaborators");
        }
    }

    public void postInvokeCollaborators(ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException,
            IOException, Exception {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
            logger.entering(CLASS_NAME, "postInvokeCollaborators");

        WebComponentMetaData cmd = collabMetaData.getComponentMetaData();
        HttpServletRequest httpRequest = collabMetaData.getHttpServletRequest();
        HttpServletResponse httpResponse = collabMetaData.getHttpServletResponse();
        boolean isSTM = false;
        IWebAppDispatcherContext dispatchContext = collabMetaData.getWebAppDispatcherContext();
        Object transaction = collabMetaData.getTransaction();

        boolean sessionInvoke = collabMetaData.isSessionInvokeRequired();

        // Invoke security collaborator here because JSR 375 requires that all CDI scopes are available.
        boolean postInvokeForSecureResponseNeeded = true;
        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.SECURITY) && securityCollaborator.isCDINeeded()) {
            postInvokeForSecureResponseNeeded = false;
            Object secObject = collabMetaData.getSecurityObject();
            this.securityCollaborator.postInvokeForSecureResponse(secObject);
        }
        
        //HttpServletRequest javadocs describe it as:
        //Interface for receiving notification events about requests coming into and going out of scope of a web application. 
        WebContainerRequestState reqState = com.ibm.wsspi.webcontainer.WebContainerRequestState.getInstance(false); // start PI26908
        
        if (collabMetaData.isServletRequestCreated())          
        {
            if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
                boolean invokeFiltersException = false;
                if(reqState != null && reqState.getAttribute("invokeFiltersException") != null){
                    invokeFiltersException = true;
                    reqState.removeAttribute("invokeFiltersException");
                }
                if(invokeFiltersException){ 
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"postInvokeCollaborators", "deferring destroy listener request");
                    reqState.setAttribute("deferringNotifyServletRequestDestroyed", true);
                }
                else{
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"postInvokeCollaborators", "destroy listener request");
                    webApp.notifyServletRequestDestroyed(httpRequest);
                    if(reqState != null)
                        reqState.removeAttribute("deferringNotifyServletRequestDestroyed");
                }
            } 
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"postInvokeCollaborators", "destroy listener request");
                webApp.notifyServletRequestDestroyed(httpRequest);
            }
        } //end PI26908

        if (sessionInvoke) {
            dispatchContext.sessionPostInvoke();
        }

        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.TRANSACTION)) {
            if (transaction != null)
                checkTransaction(transaction);
            else
                checkForRollback();
        }

        if (cmd != null) {
            cmd.handleCallbacks(collabMetaData.getCallbacksID());
        }

        if (collabMetaData.isPostInvokeNecessary()){
            if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.INVOCATION) && collabMetaData.isPostInvokeNecessary()) {
                IInvocationCollaborator[] webAppInvocationCollaborators = collabMetaData.getServletContext().getWebAppInvocationCollaborators();
                doInvocationCollaboratorsPostInvoke(webAppInvocationCollaborators, cmd, httpRequest, httpResponse);
            }
        }
        
        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.CONNECTION)) {
            HandleList _connectionHandleList = collabMetaData.getConnectionHandleList();
            this.connectionCollaborator.postInvoke(_connectionHandleList, isSTM);
        }

        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.TRANSACTION)) {
            Object txConfig = collabMetaData.getTransactionConfig();
            this.transactionCollaborator.postInvoke(httpRequest, txConfig, servlet_23_or_greater);
        }

        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.SECURITY)) {
            Object secObject = collabMetaData.getSecurityObject();
            if (postInvokeForSecureResponseNeeded) {
                this.securityCollaborator.postInvokeForSecureResponse(secObject);
            }
            this.securityCollaborator.postInvoke(secObject);
        }
        
        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.CLASSLOADER)) {
            ClassLoader origClassLoader = collabMetaData.getOrigClassLoader();
            if (origClassLoader != null) {
                final ClassLoader fOrigClassLoader = origClassLoader;

                ThreadContextHelper.setClassLoader(fOrigClassLoader);
            }
        }

        if (colEnum != null && colEnum.contains(CollaboratorInvocationEnum.NAMESPACE)) {
            this.nameSpaceCollaborator.postInvoke();
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
            logger.exiting(CLASS_NAME,"postInvokeCollaborators");
    }

    protected abstract Object getTransaction() throws Exception;

    protected abstract void checkTransaction(Object transaction);

    protected abstract void checkForRollback();

}
