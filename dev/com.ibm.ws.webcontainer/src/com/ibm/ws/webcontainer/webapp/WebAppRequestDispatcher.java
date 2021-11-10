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
package com.ibm.ws.webcontainer.webapp;

import java.io.IOException;
import java.util.EmptyStackException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.filter.ChainedResponse;
import com.ibm.websphere.webcontainer.async.AsyncRequestDispatcherConfig;
import com.ibm.websphere.webcontainer.async.FragmentResponse;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.servlet.exception.NoTargetForURIException;
import com.ibm.ws.webcontainer.srt.ISRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTRequestContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.wsspi.ard.JspAsyncRequestDispatcher;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.util.ServletUtil;

/**
 * There is a certain degree of code duplication in this class for the purpose
 * of performance gains
 * 
 * RequestDispatcher implementation
 */
public class WebAppRequestDispatcher implements RequestDispatcher, WebContainerConstants, JspAsyncRequestDispatcher {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher";

    private WebApp webapp;
    private RequestProcessor target = null;
    private String path = null;
    private boolean topLevel = true;
    private boolean exception = false;
    private boolean isURIDispatch = false;
    // private int numForwards = 0;
    private static boolean checkAtWAR = WebContainer.getWebContainer().isEnableSecurityAtWARBoundary();
    private static boolean checkAtEAR = WebContainer.getWebContainer().isEnableSecurityAtEARBoundary();

    public static boolean dispatcherRethrowSER = WCCustomProperties.DISPATCHER_RETHROW_SER;

    //PM22919 - start
    static{
        if (WCCustomProperties.DISPATCHER_RETHROW_SERROR){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "<init>", " dispatcherRethrowSER is true; dispatcherRethrowSERROR is true");
            }
            dispatcherRethrowSER = true;
        }
    };
    //PM22919 - end

    /**
     * @param app
     * @param path
     * @param dispatchContext
     * 
     *            This constructor will be called by the getRequestDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher(WebApp app, String path) // PK07351 removed
    // dispatchContext
    // as constructor
    // arg
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "WebAppRequestDispatcher", "Creating RequestDispatcher for context root [" + app.getContextPath()
                        + "] path [" + path + "]");
        }
        this.webapp = app;
        this.path = path;
        // Begin 252775, Its a URI based dispatch
        isURIDispatch = true;
        // End 252775
    }

    /**
     * @param app
     * @param path
     * @param isAsync
     * @param dispatchContext
     * 
     *            This constructor will be called by the getRequestDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher(WebApp app, String path, boolean isAsync) // PK07351
    // removed
    // dispatchContext
    // as
    // constructor
    // arg
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "WebAppRequestDispatcher", "Creating RequestDispatcher for context root [" + app.getContextPath()
                        + "] path [" + path + "]");
        }
        this.webapp = app;
        this.path = path;
        // Begin 252775, Its a URI based dispatch
        isURIDispatch = true;
        // End 252775
    }

    /**
     * Constructor WebAppRequestDispatcher.
     * 
     * @param webApp
     * @param p
     * 
     *            This constructor will be called by the getNamedDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher(WebApp webApp, RequestProcessor p)// PK07351
    // removed
    // dispatchContext
    // as
    // constructor
    // arg
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "WebAppRequestDispatcher", "Creating NamedDispatcher for context root [" + webApp.getContextPath()
                        + "] processor [" + p + "]");
        }
        this.webapp = webApp;
        target = p;
        path = null;
    }

    /**
     * @see javax.servlet.RequestDispatcher#forward(ServletRequest,
     *      ServletResponse)
     */
    public void forward(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.entering(CLASS_NAME, "forward");
            if (path != null) {
                logger.logp(Level.FINE, CLASS_NAME, "forward", "enter RequestDispatcher forward path [" + path + "]");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "forward", "enter NamedDispatcher forward target [" + target + "]");
            }
        }

        this.dispatch(req, res, DispatcherType.FORWARD);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            if (path != null) {
                logger.logp(Level.FINE, CLASS_NAME, "forward", "exit RequestDispatcher forward path [" + path + "]");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "forward", "exit NamedDispatcher forward target [" + target + "]");
            }
            logger.exiting(CLASS_NAME, "forward");
        }

    }

    /**
     * @param dispatchContext2
     * @param wasReq
     */
    private void setupSecurityChecks(IExtendedRequest wasReq, WebAppDispatcherContext dispatchContext) // PK07351
    // added
    // parameter
    // dispatchContext
    // instead
    // of
    // iVar
    {
        SRTRequestContext reqCtx = ((ISRTServletRequest) wasReq).getRequestContext();
        // Spec says that we SHOULD NOT perform security access checks in
        // forwards and includes. But this exposes a security hole where
        // an unauthenticated user can get into a secured app by simply
        // entering from another app.
        // STRATEGY: depending on the following Webcontainer properties
        // perform the appropriate checks:
        // (1) enforceSecurityAtWARBoundary == true
        // We will check to see if the forwarded/included
        // resource is in a different module from the forwarding/
        // including servlet, and call the security collaborator.
        // (2) enforceSecurityAtEARBoundary == true
        // We will check to see if the forwarded/included
        // resource is in a different EAR from the forwarding/
        // including servlet, and call the security collaborator.
        //

        if (!reqCtx.isWithinModule(webapp)) {
            if (checkAtWAR)
                dispatchContext.setEnforceSecurity(true);
            else if (checkAtEAR) {
                if (!reqCtx.isWithinApplication(webapp))
                    dispatchContext.setEnforceSecurity(true);
                else
                    dispatchContext.setEnforceSecurity(false);
            } else
                dispatchContext.setEnforceSecurity(false);
        } else
            dispatchContext.setEnforceSecurity(false);

        reqCtx.setCurrWebAppBoundary(webapp);

    }

    // public void include(ServletRequest req, ServletResponse res) throws
    // ServletException, IOException
    // {
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.entering(CLASS_NAME,"include");
    // if(path !=null){
    // logger.logp(Level.FINE,
    // CLASS_NAME,"include","enter RequestDispatcher include path [" + path
    // +"]");
    // }else {
    // logger.logp(Level.FINE,
    // CLASS_NAME,"include","enter NamedDispatcher include target [" + target +
    // "]");
    // }
    // }
    //        
    // //PK17095 START
    // HttpServletRequest request = (HttpServletRequest) req;
    // HttpServletResponse response = (HttpServletResponse) res;
    // String old_req_uri = null;
    // String old_servlet_path = null;
    // String old_path_info = null;
    // String old_context_path = null;
    // String old_query_string = null;
    // String old_dispatch_type = null;
    // IExtendedRequest wasReq = null;
    // boolean firedServletRequestCreated = false;
    // WebAppDispatcherContext oldContext = null;
    // //PK17095 END
    //    
    // // PK34536 add flags to control finally code
    // boolean attributes_saved = false;
    // boolean dispatch_type_saved = false;
    // this.topLevel = false;
    // boolean boundary_changed = false;
    //    
    //        
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // WebAppDispatcherContext dispatchContext = webapp.createDispatchContext();
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    //        
    // dispatchContext.setDispatcherType(DispatcherType.INCLUDE);
    // IExtendedRequest castReq = null;
    // String QS = null;
    //        
    // try{
    // wasReq = (IExtendedRequest) ServletUtil.unwrapRequest(request);
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.logp(Level.FINE, CLASS_NAME,"include","internal request [" +
    // wasReq +"]");
    // }
    //    
    // if(((SRTServletRequest)wasReq).getRequestContext().isWithinApplication(webapp)
    // == false){
    // webapp.notifyServletRequestCreated (req);
    // firedServletRequestCreated = true;
    // }
    //            
    // //force parsing of post parameters from a previous resource
    // //((SRTServletRequest)wasReq).parseParameters();//256836
    // dispatchContext.initForNextDispatch(wasReq);
    // boundary_changed = true; // PK34536
    // // begin:244201 resolve the uri (remove /../, etc.) on includes so we are
    // not recreating jsp meta data
    // //since JspExtensionProcessor adds a mapping with the resolved jsp file
    // name
    // //begin PK19888
    // //if (relativeURI!=null)
    // //
    // dispatchContext.setRelativeUri(com.ibm.ws.util.WSUtil.resolveURI(relativeURI));
    // //end PK19888
    // //end:244201
    //    
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // // setupSecurityChecks(wasReq);
    // setupSecurityChecks(wasReq, dispatchContext);
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    //            
    // oldContext =
    // (WebAppDispatcherContext)wasReq.getWebAppDispatcherContext();
    //            
    // wasReq.setWebAppDispatcherContext(dispatchContext);
    // castReq = (IExtendedRequest) wasReq;
    //            
    // old_req_uri = (String) request.getAttribute(REQUEST_URI_INCLUDE_ATTR);
    // old_servlet_path =
    // (String)request.getAttribute(SERVLET_PATH_INCLUDE_ATTR);
    // old_path_info = (String) request.getAttribute(PATH_INFO_INCLUDE_ATTR);
    // old_context_path =
    // (String)request.getAttribute(CONTEXT_PATH_INCLUDE_ATTR);
    // old_query_string =
    // (String)request.getAttribute(QUERY_STRING_INCLUDE_ATTR);
    // attributes_saved = true; // PK34536
    //            
    // if (isURIDispatch)
    // { // Its a URI based dispatch
    //                
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // dispatchContext.setRequestURI((webapp.getContextPath().equals("/")) ?
    // path : webapp.getContextPath() + path);
    // // begin 310092 61FVT:RequestDispatcher doesnt retain parameters:second
    // include
    // //The second time we come through, the path will already be trimmed if we
    // set path=path.substring(0,qMark);
    // //modify changes from PK07531
    // String trimmedPath = path;
    // int qMark = trimmedPath.indexOf('?');
    // if (qMark != -1)
    // trimmedPath = trimmedPath.substring(0, qMark);
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.logp(Level.FINE,
    // CLASS_NAME,"include","requestDispatcher include path trimmed of queryString ["
    // + trimmedPath +"]");
    // }
    // // end 310092 61FVT:RequestDispatcher doesnt retain parameters:second
    // include
    // //begin PK19888
    // dispatchContext.setRelativeUri(com.ibm.ws.util.WSUtil.resolveURI(trimmedPath));
    // //end PK19888
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    //    
    // QS = dispatchContext.getQueryString();
    //    
    // if (QS != null && castReq != null)
    // {
    // castReq.pushParameterStack();
    // castReq.aggregateQueryStringParams(QS,false); // End 249841
    // }
    //    
    //    
    // // the request object being passed in doesn't matter because
    // // the mapper will obtain the URI information from the ThreadLocal
    // // DispatcherContext object
    // target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);
    //                    
    // if (target == null)
    // throw new NoTargetForURIException(path);
    //    
    // setAttributes(request,true,dispatchContext.getRequestURI(),dispatchContext.getServletPath(),
    // dispatchContext.getPathInfo(),webapp.getContextPath(),dispatchContext.getQueryString());
    //                
    // }
    // else
    // {
    // setAttributes(request,true,null,null,null,null,null);
    // }
    //    
    // old_dispatch_type = (String) request.getAttribute(DISPATCH_TYPE_ATTR);
    // dispatch_type_saved = true; // PK34536
    //    
    // if (request.getAttribute(DISPATCH_NESTED_ATTR) == null)
    // {
    // this.topLevel = true;
    // request.setAttribute(DISPATCH_NESTED_ATTR, NESTED_TRUE);
    // }
    // // PK34536 else
    // // PK34536 {
    // // PK34536 topLevel = false;
    // // PK34536 }
    //    
    // request.setAttribute(DISPATCH_TYPE_ATTR, INCLUDE);
    //            
    // dispatchContext.setParentContext(oldContext);
    // dispatchContext.setUseParent(true);
    // webapp.getFilterManager().invokeFilters((HttpServletRequest) req,
    // (HttpServletResponse) res, webapp, target,
    // CollaboratorHelper.allCollabEnum);
    // }
    // catch (ServletErrorReport ser)
    // {
    // //PK79464
    // if(dispatcherRethrowSER)
    // throw ser;
    //            
    // webapp.sendError(request, response, ser);
    // }
    // catch (ServletException se)
    // {
    // throw se;
    // }
    // catch (IOException ioe)
    // {
    // throw ioe;
    // }
    // catch (RuntimeException re)
    // {
    // throw re;
    // }
    // catch (Throwable th)
    // {
    // com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
    // th,
    // "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.include",
    // "619",
    // this);
    // WebAppErrorReport r = new WebAppErrorReport(th);
    // if (target instanceof ServletWrapper)
    // r.setTargetServletName(((ServletWrapper)target).getServletName());
    // r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    // webapp.sendError(request, response, r);
    // }
    // finally
    // {
    // if (QS != null && castReq != null)
    // {
    // castReq.removeQSFromList(); // 256836
    // }
    // // Begin 252775, Reset to old context since we are now leaving the
    // include
    // if(oldContext!=null){
    // wasReq.setWebAppDispatcherContext(oldContext);
    // }
    // dispatchContext.setUseParent(false);
    // dispatchContext.setParentContext(null);
    //            
    // // reset include attributes
    // if ( attributes_saved) // PK34536
    // setAttributes(request,true,old_req_uri, old_servlet_path, old_path_info,
    // old_context_path, old_query_string);
    // castReq=null;
    //                    
    // // PK34536 only update dispatch_type if it was chnaged
    // if (dispatch_type_saved) {
    // if (old_dispatch_type != null)
    // request.setAttribute(DISPATCH_TYPE_ATTR, old_dispatch_type);
    // else
    // request.removeAttribute(DISPATCH_TYPE_ATTR);
    // }
    //
    // if (topLevel)
    // request.removeAttribute(DISPATCH_NESTED_ATTR);
    //
    // // PK34535 only rollback if boundary was changed
    // if (boundary_changed)
    // {
    // //PK17095 Catching EmptyStackException for rollBackBoundary
    // try{
    // ((SRTServletRequest)wasReq).getRequestContext().rollBackBoundary();
    // }catch(EmptyStackException ese){
    // com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
    // ese,
    // "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.forward",
    // "367",
    // this);
    // }//PK17095 END
    // }
    //            
    // if(firedServletRequestCreated){
    // webapp.notifyServletRequestDestroyed (req);
    // }
    //
    //            
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // if(path !=null){
    // logger.logp(Level.FINE,
    // CLASS_NAME,"include","exit RequestDispatcher include path [" + path
    // +"]");
    // }else {
    // logger.logp(Level.FINE,
    // CLASS_NAME,"include","exit NamedDispatcher include target [" + target +
    // "]");
    // }
    // logger.exiting(CLASS_NAME,"include");
    // }
    //
    // }
    // }

    /**
     * @see javax.servlet.RequestDispatcher#include(ServletRequest,
     *      ServletResponse)
     */
    public void include(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.entering(CLASS_NAME, "include");
            if (path != null) {
                logger.logp(Level.FINE, CLASS_NAME, "include", "enter RequestDispatcher include path [" + path + "]");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "include", "enter NamedDispatcher include target [" + target + "]");
            }
        }

        this.dispatch(req, res, DispatcherType.INCLUDE);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            if (path != null) {
                logger.logp(Level.FINE, CLASS_NAME, "include", "exit RequestDispatcher include path [" + path + "]");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "include", "exit NamedDispatcher include target [" + target + "]");
            }
            logger.exiting(CLASS_NAME, "include");
        }
    }

    // public void forward(ServletRequest req, ServletResponse res) throws
    // ServletException, IOException
    // {
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.entering(CLASS_NAME,"forward");
    // if(path !=null){
    // logger.logp(Level.FINE,
    // CLASS_NAME,"forward","enter RequestDispatcher forward path [" + path
    // +"]");
    // }else {
    // logger.logp(Level.FINE,
    // CLASS_NAME,"forward","enter NamedDispatcher forward target [" + target
    // +"]");
    // }
    // }
    //        
    //        
    // //if async, schedule new dispatch;
    //        
    // //PK17095 START
    // HttpServletRequest request = (HttpServletRequest) req;
    // HttpServletResponse response = (HttpServletResponse) res;
    // String old_req_uri = null;
    // String old_servlet_path = null;
    // String old_path_info = null;
    // String old_context_path = null;
    // String old_query_string = null;
    // String old_req_uri_for = null;
    // String old_servlet_path_for = null;
    // String old_path_info_for = null;
    // String old_context_path_for = null;
    // String old_query_string_for = null;
    // boolean firedServletRequestCreated = false;
    // IExtendedRequest wasReq = null;
    // WebAppDispatcherContext oldContext = null;
    // //PK17095 END
    //
    // // PK34536 add flags to control finally code
    // boolean include_attributes_saved = false;
    // boolean forward_attributes_saved = false;
    // boolean query_string_saved = false;
    // this.topLevel = false;
    // boolean boundary_changed = false;
    //    
    //        
    //        
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // WebAppDispatcherContext dispatchContext = webapp.createDispatchContext();
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    //        
    // dispatchContext.setDispatcherType(DispatcherType.FORWARD);
    // if (res.isCommitted())
    // // TODO: Internationalize
    // throw new
    // IllegalStateException("Cannot forward. Response already committed.");
    // else
    // res.resetBuffer();
    //
    // IExtendedRequest castReq = null;
    // String originalQueryString = null, QS = null;
    //
    // try
    // {
    // //Begin 296864, Move unwrap methods to wsspi class
    // wasReq = (IExtendedRequest) ServletUtil.unwrapRequest(request);
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.logp(Level.FINE, CLASS_NAME,"forward","internal request [" +
    // wasReq +"]");
    // }
    //
    // //End 296864, Move unwrap methods to wsspi class
    //            
    //            
    // if(((SRTServletRequest)wasReq).getRequestContext().isWithinApplication(webapp)
    // == false){
    // webapp.notifyServletRequestCreated (req);
    // firedServletRequestCreated = true;
    // }
    //            
    // //force parsing of post parameters from a previous resource
    // //((SRTServletRequest)wasReq).parseParameters(); // 256836
    //    
    // dispatchContext.initForNextDispatch(wasReq);
    // //begin:244201 resolve the uri (remove /../, etc.) on forwards so we are
    // not recreating jsp meta data
    // //since JspExtensionProcessor adds a mapping with the resolved jsp file
    // name
    // String relativeURI = dispatchContext.getRelativeUri();
    // //begin PK19888
    // //if (relativeURI!=null)
    // //
    // dispatchContext.setRelativeUri(com.ibm.ws.util.WSUtil.resolveURI(relativeURI));
    // //end PK19888
    // //end:244201
    //            
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // // setupSecurityChecks(wasReq);
    // setupSecurityChecks(wasReq, dispatchContext);
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // boundary_changed=true; // PK34536
    //            
    // oldContext =
    // (WebAppDispatcherContext)wasReq.getWebAppDispatcherContext();
    // wasReq.setWebAppDispatcherContext(dispatchContext);
    // castReq = (IExtendedRequest) wasReq;
    //    
    // old_req_uri = (String) request.getAttribute(REQUEST_URI_INCLUDE_ATTR);
    // old_servlet_path =
    // (String)request.getAttribute(SERVLET_PATH_INCLUDE_ATTR);
    // old_path_info = (String) request.getAttribute(PATH_INFO_INCLUDE_ATTR);
    // old_context_path =
    // (String)request.getAttribute(CONTEXT_PATH_INCLUDE_ATTR);
    // old_query_string =
    // (String)request.getAttribute(QUERY_STRING_INCLUDE_ATTR);
    // include_attributes_saved = true; // PK34536
    //    
    // old_req_uri_for = (String)
    // request.getAttribute(REQUEST_URI_FORWARD_ATTR);
    // old_servlet_path_for =
    // (String)request.getAttribute(SERVLET_PATH_FORWARD_ATTR);
    // old_path_info_for = (String)
    // request.getAttribute(PATH_INFO_FORWARD_ATTR);
    // old_context_path_for =
    // (String)request.getAttribute(CONTEXT_PATH_FORWARD_ATTR);
    // old_query_string_for =
    // (String)request.getAttribute(QUERY_STRING_FORWARD_ATTR);
    // forward_attributes_saved = true; // PK34536
    //    
    //            
    // if (request.getAttribute(DISPATCH_NESTED_ATTR) == null)
    // {
    // this.topLevel = true;
    // request.setAttribute(DISPATCH_NESTED_ATTR, NESTED_TRUE);
    //    
    // }
    // // PK34536 else
    // // PK34536 {
    // // PK34536 this.topLevel = false;
    // // PK34536 }
    //    
    // request.setAttribute(DISPATCH_TYPE_ATTR, FORWARD);
    //    
    // if (isURIDispatch)
    // { // Its a URI based dispatch
    //                
    // // begin PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // dispatchContext.setRequestURI((webapp.getContextPath().equals("/")) ?
    // path : webapp.getContextPath() + path);
    // // begin 310092 61FVT:RequestDispatcher doesnt retain parameters:second
    // include
    // //The second time we come through, the path will already be trimmed if we
    // set path=path.substring(0,qMark);
    // //modify changes from PK07531
    // String trimmedPath = path;
    // int qMark = trimmedPath.indexOf('?');
    // if (qMark != -1)
    // trimmedPath = trimmedPath.substring(0, qMark);
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // logger.logp(Level.FINE,
    // CLASS_NAME,"forward","requestDispatcher include path trimmed of queryString ["
    // + trimmedPath +"]");
    // }
    // // end 310092 61FVT:RequestDispatcher doesnt retain parameters:second
    // include
    // //begin PK19888
    // dispatchContext.setRelativeUri(com.ibm.ws.util.WSUtil.resolveURI(trimmedPath));
    // //end PK19888
    // // end PK07351 6021Request dispatcher could not be reused as it was in
    // V5. WAS.webcontainer
    // dispatchContext.setParentContext(oldContext);
    // dispatchContext.setUseParent(false);
    //                
    // QS = dispatchContext.getQueryString();
    // originalQueryString = castReq.getQueryString();
    // query_string_saved = true; // PK34536
    // if (QS!=null && castReq != null){
    // castReq.pushParameterStack();
    // }
    // castReq.aggregateQueryStringParams(QS,true);// End 249841
    //    
    // //If we do an include and then a forward, we will no longer be at
    // topLevel.
    // //Thus we have a new test to see if it is the firstForward and then
    // //set the forward request attributes if so.
    // // if (numForwards==1)
    // // {
    // // SRV.8.4.2 These attributes are accessible from the forwarded servlet
    // via the
    // //getAttribute method on the request object. Note that these attributes
    // must
    // //always reflect the information in the original request even under the
    // situation that
    // //multiple forwards and subsequent includes are called.
    //    
    // setAttributes(request,false,oldContext.getRequestURI(),oldContext.getServletPath(),oldContext.getPathInfo(),oldContext.getContextPath(),oldContext.getQueryString());
    // // }
    //    
    // if (SecurityContext.isSecurityEnabled())
    // {
    // // these form login related identifiers can occur anywhere in the URI
    // // hence this is the only place to check for them.
    // if (trimmedPath.indexOf("j_security_check") != -1)
    // {
    // target = webapp.getLoginProcessor();
    // }
    // else if (trimmedPath.indexOf("ibm_security_logout") != -1)
    // {
    // target = webapp.getLogoutProcessor();
    // }
    // else
    // {
    // // the request object being passed in doesn't matter only
    // // the dispatchContext in the request matters
    // target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);
    // }
    // }
    // else
    // {
    // // the request object being passed in doesn't matter only
    // // the dispatchContext in the request matters
    // target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);
    // }
    // if (target == null)
    // throw new NoTargetForURIException(path);
    //
    // }
    // else
    // {
    // // SRV.8.4 The only exception to this is if the RequestDispatcher was
    // obtained via the
    // //getNamedDispatcher method. In this case, the path elements of the
    // request object
    // //must reflect those of the original request.
    // dispatchContext.setParentContext(oldContext);
    // dispatchContext.setUseParent(true);
    // //TODO:set context path
    // //SRV.8.4.2
    // //If the forwarded servlet was obtained by using the getNamedDispatcher
    // //method, these attributes must not be set.
    // setAttributes(request,false,null,null,null,null,null);
    // }
    //            
    // // clear out previously set include attributes
    // setAttributes(request,true,null,null,null,null,null);
    //
    // webapp.getFilterManager().invokeFilters((HttpServletRequest) req,
    // (HttpServletResponse) res, webapp, target,
    // CollaboratorHelper.allCollabEnum);
    //            
    // }
    // catch (ServletErrorReport ser)
    // {
    // //PK79464
    // if(dispatcherRethrowSER)
    // throw ser;
    // webapp.sendError(request, response, ser);
    // }
    // catch (ServletException se)
    // {
    // exception = true;
    // throw se;
    // }
    // catch (IOException ioe)
    // {
    // exception = true;
    // throw ioe;
    // }
    // catch (RuntimeException re)
    // {
    // exception = true;
    // throw re;
    // }
    // catch (Throwable th)
    // {
    // com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
    // th,
    // "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.forward",
    // "328",
    // this);
    // WebAppErrorReport r = new WebAppErrorReport(th);
    // if (target instanceof ServletWrapper)
    // r.setTargetServletName(((ServletWrapper)target).getServletName());
    // r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    // webapp.sendError(request, response, r);
    // }
    // finally
    // {
    // //TODO: begin here
    // if (QS != null && castReq != null)
    // {
    // castReq.removeQSFromList(); // 256836
    // }
    // // PK34536 add check for saved query string
    // if (query_string_saved) {
    // castReq.setQueryString(originalQueryString);
    // }
    // originalQueryString = null;
    //
    // // if (numForwards==1)
    // // {
    // setAttributes(request,false,null,null,null,null,null);
    // // }
    // // numForwards--;
    //            
    // if (topLevel)
    // request.removeAttribute(DISPATCH_NESTED_ATTR);
    //            
    // dispatchContext.setUseParent(false);
    // dispatchContext.setParentContext(null);
    // if(oldContext != null){
    // wasReq.setWebAppDispatcherContext(oldContext);
    // }
    //
    // // PK34536 only rollback if boundary was changed
    // if(boundary_changed)
    // {
    // //PK17095 Catching EmptyStackException for rollBackBoundary
    // try{
    // ((SRTServletRequest)wasReq).getRequestContext().rollBackBoundary();
    // }catch(EmptyStackException ese){
    // com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
    // ese,
    // "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.forward",
    // "367",
    // this);
    // }//PK17095 END
    // }
    //
    // SRTServletResponse castResp = null;
    // // reset include attributes
    // if (include_attributes_saved) // PK34536
    // setAttributes(request,true,old_req_uri, old_servlet_path, old_path_info,
    // old_context_path, old_query_string);
    // // reset forward attributes
    // if(forward_attributes_saved) // PK34536
    // setAttributes(request,false,old_req_uri_for, old_servlet_path_for,
    // old_path_info_for, old_context_path_for, old_query_string_for);
    //            
    // castReq=null;
    //            
    //
    // if (res instanceof ChainedResponse)
    // {
    // ChainedResponse w = (ChainedResponse) res;
    //
    // // make sure we drill all the way down to an SRTServletResponse...there
    // // may be multiple proxied response objects
    // ServletResponse sr = w.getProxiedHttpServletResponse();
    //
    // while (sr != null && sr instanceof ChainedResponse)
    // sr = ((ChainedResponse) sr).getProxiedHttpServletResponse();
    //
    // // if we found an srt response, cast it
    // if (sr != null && sr instanceof SRTServletResponse)
    // castResp = (SRTServletResponse) sr;
    //
    // }
    // else if (res instanceof SRTServletResponse)
    // {
    // castResp = (SRTServletResponse) res;
    // }
    //
    // if(firedServletRequestCreated){
    // webapp.notifyServletRequestDestroyed (req);
    // }
    // if (castResp != null && !exception)
    // castResp.closeResponseOutput();
    // /*else{
    // res.flushBuffer();
    // }*/
    // //TODO: Flush for wrappers?
    // if
    // (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable
    // (Level.FINE)){ //306998.15
    // if(path !=null){
    // logger.logp(Level.FINE,
    // CLASS_NAME,"forward","exit RequestDispatcher forward path [" + path
    // +"]");
    // }else {
    // logger.logp(Level.FINE,
    // CLASS_NAME,"forward","exit NamedDispatcher forward target [" + target
    // +"]");
    // }
    // logger.exiting(CLASS_NAME,"forward");
    // }
    // }
    // }
    //    

    protected void setAttributes(ServletRequest request, DispatcherType dispatcherType, String requestURI, String servletPath, String pathInfo,
                               String contextPath, String queryString) {
        if (dispatcherType == DispatcherType.INCLUDE) {
            if (requestURI != null) {
                request.setAttribute(REQUEST_URI_INCLUDE_ATTR, requestURI);
            } else {
                request.removeAttribute(REQUEST_URI_INCLUDE_ATTR);
            }

            if (servletPath != null) {
                request.setAttribute(SERVLET_PATH_INCLUDE_ATTR, servletPath);
            } else {
                request.removeAttribute(SERVLET_PATH_INCLUDE_ATTR);
            }

            if (pathInfo != null) {
                request.setAttribute(PATH_INFO_INCLUDE_ATTR, pathInfo);
            } else {
                request.removeAttribute(PATH_INFO_INCLUDE_ATTR);
            }

            if (contextPath != null) // can never be null can it???
            {
                request.setAttribute(CONTEXT_PATH_INCLUDE_ATTR, contextPath);
            } else {
                // 143687
                request.removeAttribute(CONTEXT_PATH_INCLUDE_ATTR);
            }

            if (queryString != null) {
                request.setAttribute(QUERY_STRING_INCLUDE_ATTR, queryString);
            } else {
                request.removeAttribute(QUERY_STRING_INCLUDE_ATTR);
            }
        } else if (dispatcherType == DispatcherType.FORWARD) {
            if (requestURI != null) {
                request.setAttribute(REQUEST_URI_FORWARD_ATTR, requestURI);
            } else {
                request.removeAttribute(REQUEST_URI_FORWARD_ATTR);
            }

            if (servletPath != null) {
                request.setAttribute(SERVLET_PATH_FORWARD_ATTR, servletPath);
            } else {
                request.removeAttribute(SERVLET_PATH_FORWARD_ATTR);
            }

            if (pathInfo != null) {
                request.setAttribute(PATH_INFO_FORWARD_ATTR, pathInfo);
            } else {
                request.removeAttribute(PATH_INFO_FORWARD_ATTR);
            }

            if (contextPath != null) // can never be null can it???
            {
                request.setAttribute(CONTEXT_PATH_FORWARD_ATTR, contextPath);
            } else {
                // 143687
                request.removeAttribute(CONTEXT_PATH_FORWARD_ATTR);
            }

            if (queryString != null) {
                request.setAttribute(QUERY_STRING_FORWARD_ATTR, queryString);
            } else {
                request.removeAttribute(QUERY_STRING_FORWARD_ATTR);
            }
        } else if (dispatcherType == DispatcherType.ASYNC) {
            if (requestURI != null) {
                request.setAttribute(REQUEST_URI_ASYNC_ATTR, requestURI);
            } else {
                request.removeAttribute(REQUEST_URI_ASYNC_ATTR);
            }

            if (servletPath != null) {
                request.setAttribute(SERVLET_PATH_ASYNC_ATTR, servletPath);
            } else {
                request.removeAttribute(SERVLET_PATH_ASYNC_ATTR);
            }

            if (pathInfo != null) {
                request.setAttribute(PATH_INFO_ASYNC_ATTR, pathInfo);
            } else {
                request.removeAttribute(PATH_INFO_ASYNC_ATTR);
            }

            if (contextPath != null) // can never be null can it???
            {
                request.setAttribute(CONTEXT_PATH_ASYNC_ATTR, contextPath);
            } else {
                // 143687
                request.removeAttribute(CONTEXT_PATH_ASYNC_ATTR);
            }

            if (queryString != null) {
                request.setAttribute(QUERY_STRING_ASYNC_ATTR, queryString);
            } else {
                request.removeAttribute(QUERY_STRING_ASYNC_ATTR);
            }
        }
    }
    
    protected void clearAttributes(ServletRequest request, DispatcherType dispatcherType) {
        if (dispatcherType == DispatcherType.INCLUDE) {
            request.removeAttribute(REQUEST_URI_INCLUDE_ATTR);
            request.removeAttribute(SERVLET_PATH_INCLUDE_ATTR);
            request.removeAttribute(PATH_INFO_INCLUDE_ATTR);
            request.removeAttribute(CONTEXT_PATH_INCLUDE_ATTR);
            request.removeAttribute(QUERY_STRING_INCLUDE_ATTR);
        } else if (dispatcherType == DispatcherType.FORWARD) {
            request.removeAttribute(REQUEST_URI_FORWARD_ATTR);
            request.removeAttribute(SERVLET_PATH_FORWARD_ATTR);
            request.removeAttribute(PATH_INFO_FORWARD_ATTR);
            request.removeAttribute(CONTEXT_PATH_FORWARD_ATTR);
            request.removeAttribute(QUERY_STRING_FORWARD_ATTR);
        } else if (dispatcherType == DispatcherType.ASYNC) {
            request.removeAttribute(REQUEST_URI_ASYNC_ATTR);
            request.removeAttribute(SERVLET_PATH_ASYNC_ATTR);
            request.removeAttribute(PATH_INFO_ASYNC_ATTR);
            request.removeAttribute(CONTEXT_PATH_ASYNC_ATTR);
            request.removeAttribute(QUERY_STRING_ASYNC_ATTR);
        }
 
    }
    
    public void dispatch(ServletRequest request, ServletResponse response, final DispatcherType dispatcherType) throws ServletException, IOException {
        final String dispatcherTypeString = dispatcherType.toString();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.entering(CLASS_NAME, "dispatch");
            if (path != null) {
                logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "enter RequestDispatcher " + dispatcherTypeString + " path [" + path + "]");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "enter NamedDispatcher " + dispatcherTypeString + " target [" + target
                            + "]");
            }
        }

        // PK17095 START

        HttpServletRequest httpServletReq = (HttpServletRequest) ServletUtil.unwrapRequest(request,HttpServletRequest.class);
        HttpServletResponse httpServletRes = (HttpServletResponse) ServletUtil.unwrapResponse(response,HttpServletResponse.class);


        String old_req_uri = null;
        String old_servlet_path = null;
        String old_path_info = null;
        String old_context_path = null;
        String old_query_string = null;
        String old_dispatch_type_attribute = null;

        String old_req_uri_for = null;
        String old_servlet_path_for = null;
        String old_path_info_for = null;
        String old_context_path_for = null;
        String old_query_string_for = null;

        String old_req_uri_async = null;
        String old_servlet_path_async = null;
        String old_path_info_async = null;
        String old_context_path_async = null;
        String old_query_string_async = null;
        boolean async_attributes_saved = false;

        boolean firedServletRequestCreated = false;

        IExtendedRequest wasReq = null;
        WebAppDispatcherContext oldContext = null;

        // PK34536 add flags to control finally code
        boolean include_attributes_saved = false;
        boolean forward_attributes_saved = false;
        boolean query_string_saved = false;
        this.topLevel = false;
        boolean boundary_changed = false;
        boolean dispatch_type_saved = false;

        // begin PK07351 6021Request dispatcher could not be reused as it was in
        // V5. WAS.webcontainer
        WebAppDispatcherContext dispatchContext = webapp.createDispatchContext();
        // end PK07351 6021Request dispatcher could not be reused as it was in
        // V5. WAS.webcontainer

        dispatchContext.setDispatcherType(dispatcherType);
        
        if (path == null){
            dispatchContext.setNamedDispatcher(true);
        }

        //Defect 632800
        //we need to keep track of the error dispatch type separately from the 
        //include or forward dispatch type so that error dispatches still behave
        //the same based off of whether it was a forward or include, but also
        //the ServletRequest.getDispatcherType must return error regardless of
        //whether include or forward was used.
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);

        wasReq = (IExtendedRequest) ServletUtil.unwrapRequest(request);

        if (reqState!=null&&reqState.getAttribute("isErrorDispatcherType")!=null)
        {
            wasReq.setDispatcherType(DispatcherType.ERROR);
            reqState.removeAttribute("isErrorDispatcherType");
        }
        else {
            wasReq.setDispatcherType(dispatcherType);
        }
        //Defect 632800


        if (dispatcherType == DispatcherType.FORWARD) {
            // TODO: can we change this to setDispatchHttperType?
            // dispatchContext.setForward(true);
            // numForwards++;
            if (response.isCommitted())
                // TODO: Internationalize
                throw new IllegalStateException("Cannot forward. Response already committed.");
            else
                response.resetBuffer();
        }
        // else {
        // dispatchContext.setIsInclude(true);
        // }

        IExtendedRequest castReq = null;
        String originalQueryString = null, QS = null;
        boolean wasAsyncSupported=false;
        try {

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "internal request [" + wasReq + "]");
            }

            //Setup to check is we dispatched from async to sync servlet
            wasAsyncSupported = wasReq.isAsyncSupported();            

            if (((ISRTServletRequest) wasReq).getRequestContext().isWithinApplication(webapp) == false) {
                //PK91120 Start
                if (reqState!=null&&reqState.getAttribute("com.ibm.ws.webcontainer.invokeListenerRequest") != null)
                    reqState.removeAttribute("com.ibm.ws.webcontainer.invokeListenerRequest");

                if (!webapp.getWebAppConfig().isJCDIEnabled()){
                    firedServletRequestCreated = webapp.notifyServletRequestCreated (request);
                    //firedServletRequestCreated = true;
                    if (firedServletRequestCreated && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&& logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "dispatch" , "Listener request created --> "+ firedServletRequestCreated);
                    //PK91120 End
                }
            }

            dispatchContext.initForNextDispatch(wasReq);
            boundary_changed = true; // PK34536

            setupSecurityChecks(wasReq, dispatchContext);

            oldContext = (WebAppDispatcherContext) wasReq.getWebAppDispatcherContext();
            wasReq.setWebAppDispatcherContext(dispatchContext);
            castReq = (IExtendedRequest) wasReq;

            old_req_uri = (String) request.getAttribute(REQUEST_URI_INCLUDE_ATTR);
            old_servlet_path = (String) request.getAttribute(SERVLET_PATH_INCLUDE_ATTR);
            old_path_info = (String) request.getAttribute(PATH_INFO_INCLUDE_ATTR);
            old_context_path = (String) request.getAttribute(CONTEXT_PATH_INCLUDE_ATTR);
            old_query_string = (String) request.getAttribute(QUERY_STRING_INCLUDE_ATTR);
            include_attributes_saved = true; // PK34536


            old_req_uri_for = (String) request.getAttribute(REQUEST_URI_FORWARD_ATTR);
            if (old_req_uri_for!=null){
                old_servlet_path_for = (String) request.getAttribute(SERVLET_PATH_FORWARD_ATTR);
                old_path_info_for = (String) request.getAttribute(PATH_INFO_FORWARD_ATTR);
                old_context_path_for = (String) request.getAttribute(CONTEXT_PATH_FORWARD_ATTR);
                old_query_string_for = (String) request.getAttribute(QUERY_STRING_FORWARD_ATTR);
            }

            forward_attributes_saved = true;

            old_req_uri_async = (String) request.getAttribute(REQUEST_URI_ASYNC_ATTR);
            if (old_req_uri_async!=null){
                old_servlet_path_async = (String) request.getAttribute(SERVLET_PATH_ASYNC_ATTR);
                old_path_info_async = (String) request.getAttribute(PATH_INFO_ASYNC_ATTR);
                old_context_path_async = (String) request.getAttribute(CONTEXT_PATH_ASYNC_ATTR);
                old_query_string_async = (String) request.getAttribute(QUERY_STRING_ASYNC_ATTR);
            }

            async_attributes_saved = true;

            // TODO: end

            if (dispatcherType == DispatcherType.INCLUDE) {
                old_dispatch_type_attribute = (String) request.getAttribute(DISPATCH_TYPE_ATTR);
                dispatch_type_saved = true; // PK34536
            }

            if (request.getAttribute(DISPATCH_NESTED_ATTR) == null) {
                this.topLevel = true;
                request.setAttribute(DISPATCH_NESTED_ATTR, NESTED_TRUE);

            }
            // PK34536 else
            // PK34536 {
            // PK34536 this.topLevel = false;
            // PK34536 }
            request.setAttribute(DISPATCH_TYPE_ATTR, dispatcherType.toString().toLowerCase());

            if (isURIDispatch) { // Its a URI based dispatch

                // begin PK07351 6021Request dispatcher could not be reused as
                // it was in V5. WAS.webcontainer
                if(webapp.getConfiguration().isEncodeDispatchedRequestURI()) //PI67492
                    dispatchContext.setRequestURI(((webapp.getContextPath().equals("/")) ? path : webapp.getContextPath() + path), true);
                else
                    dispatchContext.setRequestURI((webapp.getContextPath().equals("/")) ? path : webapp.getContextPath() + path);
                // begin 310092 61FVT:RequestDispatcher doesnt retain
                // parameters:second include
                // The second time we come through, the path will already be
                // trimmed if we set path=path.substring(0,qMark);
                // modify changes from PK07531
                String trimmedPath = path;
                int qMark = trimmedPath.indexOf('?');
                if (qMark != -1)
                    trimmedPath = trimmedPath.substring(0, qMark);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                    logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "requestDispatcher include path trimmed of queryString [" + trimmedPath
                                + "]");
                }
                // end 310092 61FVT:RequestDispatcher doesnt retain
                // parameters:second include
                // begin PK19888
                dispatchContext.setRelativeUri(com.ibm.ws.util.WSUtil.resolveURI(trimmedPath));
                // end PK19888
                // end PK07351 6021Request dispatcher could not be reused as it
                // was in V5. WAS.webcontainer

                // make sure to get the new QueryString before setting parent
                // context
                QS = dispatchContext.getQueryString();

                // TODO: can we move this down so its common to include and
                // forward
                if (dispatcherType != DispatcherType.INCLUDE) {
                    originalQueryString = castReq.getQueryString();
                    query_string_saved = true; // PK34536
                }

                if (QS != null && castReq != null) {
                    castReq.pushParameterStack(); // 249841
                }

                // PI81569 Start: We do not want to change the QS in 2 scenarios (setQS should be false in these cases):
                //              A) this is an include
                //              B) this is a forward and Custom Property is set and QS is null (we want to keep the original QS)        
                boolean useOriginalQSInForwardIfNull = WCCustomProperties.USE_ORIGINAL_QS_IN_FORWARD_IF_NULL;


                boolean setQS = dispatcherType != DispatcherType.INCLUDE;

                if(useOriginalQSInForwardIfNull){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                        logger.logp(Level.FINE, CLASS_NAME, "dispatch", "useOriginalQSInForwardIfNull = true, setQS = " + setQS + ", QS is null = "+ (QS == null)+", dispatcherType = "+dispatcherType);
                    }
                    setQS = setQS && !(dispatcherType == DispatcherType.FORWARD && QS == null);
                }

                castReq.aggregateQueryStringParams(QS, setQS);

                if (dispatcherType == DispatcherType.INCLUDE) {

                    // the request object being passed in doesn't matter only
                    // the dispatchContext in the request matters
                    target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);

                    // these attributes have to be setting after calling map
                    // because map will set some of the path elements
                    setAttributes(request, dispatcherType, dispatchContext.getRequestURI(), dispatchContext.getServletPath(), dispatchContext
                                  .getPathInfo(), webapp.getContextPath(), dispatchContext.getQueryString());
                } else {
                    //This custom property was added to try to maintain the V7 behavior of the following commented code.
                    // The old code never really worked, because unless you were using the same dispatcher
                    // in a nested forward dispatch, numForwards would always be 1. I think it actually did the complete opposite
                    // of its intent. It did not keep the original path elements. That is why the default
                    // value of KEEP_ORIGINAL_PATH_ELEMENTS is true which goes through the new path
                    // and actually keeps the original path elements.
                    //if (numForwards==1)
                    //                	{
                    // SRV.8.4.2 These attributes are accessible from the forwarded servlet via the
                    //getAttribute method on the request object. Note that these attributes must
                    //always reflect the information in the original request even under the situation that
                    //multiple forwards and subsequent includes are called.
                    //
                    //                		setAttributes(request,false,oldContext.getRequestURI(),oldContext.getServletPath(),oldContext.getPathInfo(),oldContext.getContextPath(),oldContext.getQueryString());
                    //                	}
                    if (!WCCustomProperties.KEEP_ORIGINAL_PATH_ELEMENTS){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                            logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "update forward or async attributes to uri before last dispatch");
                        }
                        setAttributes(request, dispatcherType, oldContext.getRequestURI(), oldContext.getServletPath(), oldContext.getPathInfo(),
                                      oldContext.getContextPath(), oldContext.getQueryString());
                    } else {
                        if (dispatcherType==DispatcherType.ASYNC){
                            if (old_req_uri_async==null){ // no need to set again if already set
                                if (old_req_uri_for!=null){ // if forward attr is set, it will reflect the original path, which is the same thing async needs to reflect
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                                        logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "set async attributes based off of forward attributes");
                                    }
                                    setAttributes(request, DispatcherType.ASYNC, old_req_uri_for, old_servlet_path_for,old_path_info_for,old_context_path_for,old_query_string_for);
                                } else {
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                                        logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "set async attributes based off of old dispatch context");
                                    }
                                    setAttributes(request, DispatcherType.ASYNC, oldContext.getRequestURI(), oldContext.getServletPath(), oldContext.getPathInfo(),
                                                  oldContext.getContextPath(), oldContext.getQueryString());
                                }
                            }
                        } else {
                            if (old_req_uri_for==null){ // no need to set again if already set
                                if (old_req_uri_async!=null){ // if async attr is set, it will reflect the original path, which is the same thing forward needs to reflect
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                                        logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "set forward attributes based off of async attributes");
                                    }
                                    setAttributes(request, DispatcherType.FORWARD, old_req_uri_async, old_servlet_path_async,old_path_info_async,old_context_path_async,old_query_string_async);
                                } else {
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                                        logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "set forward attributes based off of old dispatch context");
                                    }
                                    setAttributes(request, DispatcherType.FORWARD, oldContext.getRequestURI(), oldContext.getServletPath(), oldContext.getPathInfo(),
                                                  oldContext.getContextPath(), oldContext.getQueryString());
                                }
                            }
                        }
                    }

                    boolean securityEnabled = ((CollaboratorHelperImpl)webapp.getCollaboratorHelper()).isSecurityEnabled();
                    if (securityEnabled) {
                        // these form login related identifiers can occur
                        // anywhere in the URI
                        // hence this is the only place to check for them.
                        if (trimmedPath.indexOf("j_security_check") != -1 && (!WCCustomProperties.ENABLE_EXACT_MATCH_J_SECURITY_CHECK || trimmedPath.endsWith("/j_security_check") )){ //F011107
                            target = webapp.getLoginProcessor();
                        } else if (trimmedPath.indexOf("ibm_security_logout") != -1) {
                            target = webapp.getLogoutProcessor();
                        } else {
                            // the request object being passed in doesn't matter
                            // only
                            // the dispatchContext in the request matters
                            target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);
                        }
                    } else {
                        // the request object being passed in doesn't matter
                        // only
                        // the dispatchContext in the request matters
                        target = (RequestProcessor) webapp.getRequestMapper().map(wasReq);
                    }
                }

                if (target == null)
                    throw new NoTargetForURIException(path);

                dispatchContext.setParentContext(oldContext);
                dispatchContext.setUseParent(dispatcherType == DispatcherType.INCLUDE);
            } else {
                clearAttributes(request, dispatcherType);

                dispatchContext.setParentContext(oldContext);
                dispatchContext.setUseParent(true);
            }

            // If we are not an include, we want to set all the include
            // attributes to null
            if (dispatcherType != DispatcherType.INCLUDE) {
                clearAttributes(request, DispatcherType.INCLUDE);
            }

            webapp.getFilterManager().invokeFilters(request, response, webapp, target,
                                                    CollaboratorHelper.allCollabEnum);
        } catch (ServletErrorReport ser) {
            // PK79464
            if (dispatcherRethrowSER&&httpServletReq.getAttribute(WebContainerConstants.IGNORE_DISPATCH_STATE) == null)
                throw ser;
            webapp.sendError(httpServletReq, httpServletRes, ser);
        } catch (ServletException se) {
            exception = true;
            throw se;
        } catch (IOException ioe) {
            exception = true;
            throw ioe;
        } catch (RuntimeException re) {
            exception = true;
            throw re;
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.dispatch",
                                                                         "580", this);
            WebAppErrorReport r = new WebAppErrorReport(th);
            if (target instanceof ServletWrapper)
                r.setTargetServletName(((ServletWrapper) target).getServletName());
            r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            webapp.sendError(httpServletReq, httpServletRes, r);
        } finally {
            if (QS != null && castReq != null) {
                castReq.removeQSFromList(); // 256836
            }
            // PK34536 add check for saved query string
            if (query_string_saved) {
                castReq.setQueryString(originalQueryString);
            }

            // I don't think this has any purpose since we set again below
            // if (dispatcherType==DispatcherType.FORWARD){
            // // if (numForwards==1)
            // // {
            // setAttributes(request,DispatcherType.FORWARD,null,null,null,null,null);
            // // }
            // // numForwards--;
            // }

            // Reset to old context since we are now leaving the dispatch
            if (oldContext != null) {
                wasReq.setWebAppDispatcherContext(oldContext);
            }
            dispatchContext.setUseParent(false);
            dispatchContext.setParentContext(null);

            // PK34536 only update dispatch_type if it was chnaged
            if (dispatcherType == DispatcherType.INCLUDE && dispatch_type_saved) {
                if (old_dispatch_type_attribute != null)
                    request.setAttribute(DISPATCH_TYPE_ATTR, old_dispatch_type_attribute);
                else
                    request.removeAttribute(DISPATCH_TYPE_ATTR);
            }

            if (topLevel)
                request.removeAttribute(DISPATCH_NESTED_ATTR);

            // PK34536 only rollback if boundary was changed
            if (boundary_changed) {
                // PK17095 Catching EmptyStackException for rollBackBoundary
                try {
                    ((ISRTServletRequest) wasReq).getRequestContext().rollBackBoundary();
                } catch (EmptyStackException ese) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ese,
                                                                                 "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.forward", "367", this);
                }// PK17095 END
            }

            // reset include attributes
            if (include_attributes_saved) // PK34536
                setAttributes(request, DispatcherType.INCLUDE, old_req_uri, old_servlet_path, old_path_info, old_context_path, old_query_string);

            if (dispatcherType==DispatcherType.FORWARD&&forward_attributes_saved){
                if (!WCCustomProperties.KEEP_ORIGINAL_PATH_ELEMENTS) // PK34536
                    setAttributes(request, DispatcherType.FORWARD, old_req_uri_for, old_servlet_path_for, old_path_info_for, old_context_path_for,
                                  old_query_string_for);
                else if (old_req_uri_for==null){// only need to reset if the last forward attributes were null, i.e. before the first async call
                    // Otherwise, it always reflects the original path elements so no need to reset.
                    clearAttributes(request, DispatcherType.FORWARD);
                }
            }
            else if (dispatcherType==DispatcherType.ASYNC&&async_attributes_saved){
                if (!WCCustomProperties.KEEP_ORIGINAL_PATH_ELEMENTS) // PK34536
                    setAttributes(request, DispatcherType.ASYNC, old_req_uri_async, old_servlet_path_async, old_path_info_async, old_context_path_async,
                                  old_query_string_async);
                else if (old_req_uri_async==null){ // only need to reset if the last async attributes were null, i.e. before the first async call
                    // Otherwise, it always reflects the original path elements so no need to reset.
                    clearAttributes(request, DispatcherType.ASYNC);
                }
            }

            if (firedServletRequestCreated) {
                webapp.notifyServletRequestDestroyed(request);
            }

            if (dispatcherType == DispatcherType.FORWARD) {
                SRTServletResponse castResp = null;

                if (response instanceof SRTServletResponse) {
                    castResp = (SRTServletResponse) response;
                }
                else {
                    if (WCCustomProperties.CLOSE_WRAPPED_RESPONSE_OUTPUT_AFTER_FORWARD) {
                            castResp = (SRTServletResponse) ((IExtendedResponse) ServletUtil.unwrapResponse(response)); 
                    }
                    else {
                        if (response instanceof ChainedResponse) {
                            ChainedResponse w = (ChainedResponse) response;

                            // make sure we drill all the way down to an
                            // SRTServletResponse...there
                            // may be multiple proxied response objects
                            ServletResponse sr = w.getProxiedHttpServletResponse();

                            while (sr != null && sr instanceof ChainedResponse)
                                sr = ((ChainedResponse) sr).getProxiedHttpServletResponse();

                            // if we found an srt response, cast it
                            if (sr != null && sr instanceof SRTServletResponse)
                                castResp = (SRTServletResponse) sr;
                        }
                    }
                }

                if (castResp != null && !exception) {
                    if (wasAsyncSupported&&(reqState!=null)&&(com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31)) {
                        if (reqState.isAsyncMode()) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                                logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "Forward request did an async dispatch so don't close the output stream.");
                            } 
                            // do nothing
                        } else {
                            castResp.closeResponseOutput();  
                        }                        
                    } else {
                        castResp.closeResponseOutput();                        
                    }
                }
                else {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                        logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "cannot closeResponseOutput at the end of forward");
                    } 
                }

            } else if (wasAsyncSupported&&wasReq!=null&&!wasReq.isAsyncSupported()){
                //If the value of async supported changed between entry and exit, we went from async to sync.
                //In which case, you must commite the response
                response.flushBuffer();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "dispatch", "fragments will be executed synchronously");
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                if (path != null) {
                    logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "exit RequestDispatcher " + dispatcherTypeString + " path [" + path
                                + "]");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME, dispatcherTypeString, "exit NamedDispatcher " + dispatcherTypeString + " target [" + target
                                + "]");
                }
                logger.exiting(CLASS_NAME, "dispatch");
            }

        }

    }

    public FragmentResponse getFragmentResponse(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "getFragmentResponse", "fragments will be executed synchronously");
        }

        return new SyncFragmentResponse(this);
    }

    public void setAsyncRequestDispatcherConfig(AsyncRequestDispatcherConfig config) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "setAsyncRequestDispatcherConfig", "normal request dispatcher will not use dispatcher config");
        }
    }

    public FragmentResponse executeFragmentWithWrapper(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        return getFragmentResponse(req, resp);
    }

    public ServletResponse getFragmentResponseWrapper(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        return resp;
    }

}
