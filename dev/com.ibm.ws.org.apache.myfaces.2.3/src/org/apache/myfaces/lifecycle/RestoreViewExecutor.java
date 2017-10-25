/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.lifecycle;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.ProtectedViewException;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.flow.FlowHandler;
import javax.faces.lifecycle.ClientWindow;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.webapp.FacesServlet;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.event.PostClientWindowAndViewInitializedEvent;

import org.apache.myfaces.renderkit.ErrorPageWriter;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ExternalContextUtils;
import org.apache.myfaces.shared.util.ViewProtectionUtils;

/**
 * Implements the Restore View Phase (JSF Spec 2.2.1)
 * 
 * @author Nikolay Petrov (latest modification by $Author: tandraschko $)
 * @author Bruno Aranda (JSF 1.2)
 * @version $Revision: 1805207 $ $Date: 2017-08-16 15:24:44 +0000 (Wed, 16 Aug 2017) $
 */
class RestoreViewExecutor extends PhaseExecutor
{

    //private static final Log log = LogFactory.getLog(RestoreViewExecutor.class);
    private static final Logger log = Logger.getLogger(RestoreViewExecutor.class.getName());
    
    private RestoreViewSupport _restoreViewSupport;
    
    private Boolean _viewNotFoundCheck;
    
    private RenderKitFactory _renderKitFactory = null;
    
    @Override
    public void doPrePhaseActions(FacesContext facesContext)
    {
        // Call initView() on the ViewHandler. 
        // This will set the character encoding properly for this request.
        // Note that we are doing this here, because we need the character encoding
        // to be set as early as possible (before any PhaseListener is executed).
        facesContext.getApplication().getViewHandler().initView(facesContext);
    }

    public boolean execute(FacesContext facesContext)
    {
        if (facesContext == null)
        {
            throw new FacesException("FacesContext is null");
        }

        // get some required Objects
        Application application = facesContext.getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        UIViewRoot viewRoot = facesContext.getViewRoot();
        RestoreViewSupport restoreViewSupport = getRestoreViewSupport(facesContext);

        // Examine the FacesContext instance for the current request. If it already contains a UIViewRoot
        if (viewRoot != null)
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("View already exists in the FacesContext");
            }
            
            // Set the locale on this UIViewRoot to the value returned by the getRequestLocale() method on the
            // ExternalContext for this request
            viewRoot.setLocale(facesContext.getExternalContext().getRequestLocale());
            
            restoreViewSupport.processComponentBinding(facesContext, viewRoot);
            
            // invoke the afterPhase MethodExpression of UIViewRoot
            _invokeViewRootAfterPhaseListener(facesContext);
            
            return false;
        }
        
        String viewId = restoreViewSupport.calculateViewId(facesContext);

        // Determine if the current request is an attempt by the 
        // servlet container to display an error page.
        // If the request is an error page request, the servlet container
        // is required to set the request parameter "javax.servlet.error.message".
        final boolean errorPageRequest = facesContext.getExternalContext().getRequestMap()
                                                 .get("javax.servlet.error.message") != null;
        
        // Determine if this request is a postback or an initial request.
        // But if it is an error page request, do not treat it as a postback (since 2.0)
        if (!errorPageRequest && restoreViewSupport.isPostback(facesContext))
        { // If the request is a postback
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Request is a postback");
            }

            if (checkViewNotFound(facesContext))
            {
                String derivedViewId = viewHandler.deriveLogicalViewId(facesContext, viewId);
                ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(facesContext, 
                    derivedViewId);
            
                // viewHandler.deriveLogicalViewId() could trigger an InvalidViewIdException, which
                // it is handled internally sending a 404 error code set the response as complete.
                if (facesContext.getResponseComplete())
                {
                    return true;
                }
            
                if (vdl == null || derivedViewId == null)
                {
                    sendSourceNotFound(facesContext, viewId);
                    return true;
                }
                else if (!restoreViewSupport.checkViewExists(facesContext, derivedViewId))
                {
                    sendSourceNotFound(facesContext, viewId);
                    return true;
                }
            }
            
            try
            {
                facesContext.setProcessingEvents(false);
                // call ViewHandler.restoreView(), passing the FacesContext instance for the current request and the 
                // view identifier, and returning a UIViewRoot for the restored view.
                
                viewRoot = viewHandler.restoreView(facesContext, viewId);
                if (viewRoot == null)
                {
                    if (facesContext.getResponseComplete())
                    {
                        // If the view handler cannot restore the view and the response
                        // is complete, it can be an error or some logic in restoreView.
                        return true;
                    }
                    else
                    {
                        // If the return from ViewHandler.restoreView() is null, throw a ViewExpiredException with an 
                        // appropriate error message.
                        throw new ViewExpiredException("View \"" + viewId + "\" could not be restored.", viewId);
                    }
                }
                // If the view is transient (stateless), it is necessary to check the view protection
                // in POST case.
                if (viewRoot.isTransient())
                {
                    checkViewProtection(facesContext, viewHandler, viewRoot.getViewId(), viewRoot);
                }
                
                // Store the restored UIViewRoot in the FacesContext.
                facesContext.setViewRoot(viewRoot);
            }
            finally
            {
                facesContext.setProcessingEvents(true);
            }
            
            // Restore binding
            // See https://javaserverfaces-spec-public.dev.java.net/issues/show_bug.cgi?id=806
            restoreViewSupport.processComponentBinding(facesContext, viewRoot);
            
            ClientWindow clientWindow = facesContext.getExternalContext().getClientWindow();
            if (clientWindow != null)
            {
                // The idea of this event is once the client window and the view is initialized, 
                // you have the information required to clean up any scope that belongs to old
                // clientWindow instances like flow scope (in server side state saving).
                facesContext.getApplication().publishEvent(facesContext, PostClientWindowAndViewInitializedEvent.class, 
                        clientWindow);
            }
        }
        else
        { // If the request is a non-postback
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Request is not a postback. New UIViewRoot will be created");
            }
            
            //viewHandler.deriveViewId(facesContext, viewId)
            //restoreViewSupport.deriveViewId(facesContext, viewId)
            String logicalViewId = viewHandler.deriveLogicalViewId(facesContext, viewId);
            ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(facesContext, logicalViewId);
            
            // viewHandler.deriveLogicalViewId() could trigger an InvalidViewIdException, which
            // it is handled internally sending a 404 error code set the response as complete.
            if (facesContext.getResponseComplete())
            {
                return true;
            }
            
            if (checkViewNotFound(facesContext))
            {
                if (vdl == null || logicalViewId == null)
                {
                    sendSourceNotFound(facesContext, viewId);
                    return true;
                }
                else if (!restoreViewSupport.checkViewExists(facesContext, logicalViewId))
                {
                    sendSourceNotFound(facesContext, viewId);
                    return true;
                }
            }
            
            if (vdl != null)
            {
                ViewMetadata metadata = vdl.getViewMetadata(facesContext, viewId);
                
                if (metadata != null)
                {
                    viewRoot = metadata.createMetadataView(facesContext);
                    
                    if(facesContext.getResponseComplete())
                    {
                        // this can happen if the current request is a debug request,
                        // in this case no further processing is necessary
                        return true;
                    }
                }
    
                if (viewRoot == null)
                {
                    facesContext.renderResponse();
                }
                else if (viewRoot != null && !ViewMetadata.hasMetadata(viewRoot))
                {
                    facesContext.renderResponse();
                }
            }
            else
            {
                // Call renderResponse
                facesContext.renderResponse();
            }

            checkViewProtection(facesContext, viewHandler, logicalViewId, viewRoot);
            
            // viewRoot can be null here, if ...
            //   - we don't have a ViewDeclarationLanguage (e.g. when using facelets-1.x)
            //   - there is no view metadata or metadata.createMetadataView() returned null
            if (viewRoot == null)
            {
                // call ViewHandler.createView(), passing the FacesContext instance for the current request and 
                // the view identifier
                viewRoot = viewHandler.createView(facesContext, viewId);
            }
            
            if (viewRoot == null && facesContext.getResponseComplete())
            {
                // If the view handler cannot create the view and the response
                // is complete, it can be an error, just get out of the algorithm.
                return true;
            }
            
            // Subscribe the newly created UIViewRoot instance to the AfterAddToParent event, passing the 
            // UIViewRoot instance itself as the listener.
            // -= Leonardo Uribe =- This line it is not necessary because it was
            // removed from jsf 2.0 section 2.2.1 when pass from EDR2 to Public Review 
            // viewRoot.subscribeToEvent(PostAddToViewEvent.class, viewRoot);
            
            // Store the new UIViewRoot instance in the FacesContext.
            facesContext.setViewRoot(viewRoot);

            ClientWindow clientWindow = facesContext.getExternalContext().getClientWindow();
            if (clientWindow != null)
            {
                facesContext.getApplication().publishEvent(facesContext, PostClientWindowAndViewInitializedEvent.class, 
                        clientWindow);
            }
            
            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
            if (flowHandler != null)
            {
                flowHandler.clientWindowTransition(facesContext);
            }

            // Publish an AfterAddToParent event with the created UIViewRoot as the event source.
            application.publishEvent(facesContext, PostAddToViewEvent.class, viewRoot);
        }

        // add the ErrorPageBean to the view map to fully support 
        // facelet error pages, if we are in ProjectStage Development
        // and currently generating an error page
        if (errorPageRequest && facesContext.isProjectStage(ProjectStage.Development))
        {
            facesContext.getViewRoot().getViewMap()
                    .put(ErrorPageWriter.ERROR_PAGE_BEAN_KEY, new ErrorPageWriter.ErrorPageBean());
        }
        
        // invoke the afterPhase MethodExpression of UIViewRoot
        _invokeViewRootAfterPhaseListener(facesContext);
        
        return false;
    }
    
    private void checkViewProtection(FacesContext facesContext, ViewHandler viewHandler, 
        String viewId, UIViewRoot root) throws ProtectedViewException
    {
        boolean valid = true;
        if (ViewProtectionUtils.isViewProtected(facesContext, viewId))
        {
            // "... Obtain the value of the value of the request parameter whose 
            // name is given by the value of ResponseStateManager.NON_POSTBACK_VIEW_TOKEN_PARAM. 
            // If there is no value, throw ProtectedViewException ..."
            String token = (String) facesContext.getExternalContext().getRequestParameterMap().get(
                ResponseStateManager.NON_POSTBACK_VIEW_TOKEN_PARAM);
            if (token != null && token.length() > 0)
            {
                String renderKitId = null;
                if (root != null)
                {
                    renderKitId = root.getRenderKitId();
                }
                if (renderKitId == null)
                {
                    renderKitId = viewHandler.calculateRenderKitId(facesContext);
                }
                RenderKit renderKit = getRenderKitFactory().getRenderKit(facesContext, renderKitId);
                ResponseStateManager rsm = renderKit.getResponseStateManager();
                
                String storedToken = rsm.getCryptographicallyStrongTokenFromSession(facesContext);
                if (token.equals(storedToken))
                {
                    if (!ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
                    {
                        // Any check beyond this point only has sense for servlet requests.
                        String referer = facesContext.getExternalContext().
                            getRequestHeaderMap().get("Referer");
                        if (referer != null)
                        {
                            valid = valid && checkRefererOrOriginHeader(
                                facesContext, viewHandler, referer);
                        }
                        String origin = facesContext.getExternalContext().
                            getRequestHeaderMap().get("Origin");
                        if (valid && origin != null)
                        {
                            valid = valid && checkRefererOrOriginHeader(
                                facesContext, viewHandler, origin);
                        }
                    }
                }
                else
                {
                    valid = false;
                }
            }
            else
            {
                valid = false;
            }
        }
        if (!valid)
        {
            throw new ProtectedViewException();
        }
    }
    
    private boolean checkRefererOrOriginHeader(FacesContext facesContext, 
        ViewHandler viewHandler, String refererOrOrigin)
    {
        try
        {
            // The referer can be absolute or relative. 
            ExternalContext ectx = facesContext.getExternalContext();
            URI refererURI = new URI(refererOrOrigin);
            String path = refererURI.getPath();
            String appContextPath = ectx.getApplicationContextPath();
            if (refererURI.isAbsolute())
            {
                // Check if the referer comes from the same host
                String host = refererURI.getHost();
                int port = refererURI.getPort();
                String serverHost = ectx.getRequestServerName();
                int serverPort = ectx.getRequestServerPort();

                boolean matchPort = true;
                if (serverPort != -1 && port != -1)
                {
                    matchPort = (serverPort == port);
                }
                boolean isStrictJsf2OriginHeaderAppPath = 
                                MyfacesConfig.getCurrentInstance(ectx).isStrictJsf2OriginHeaderAppPath();
                if (!path.equals(""))
                {
                    if (serverHost.equals(host) && matchPort && path.contains(appContextPath))
                    {
                        // Referer Header match
                    }
                    else
                    {
                        // Referer Header does not match
                        return false;
                    }
                }
                else
                {
                    if (serverHost.equals(host) && matchPort && !isStrictJsf2OriginHeaderAppPath)
                    {
                        // Origin Header match and 
                        // STRICT_JSF_2_ORIGIN_HEADER_APP_PATH property is set to false (default)
                        // Because we don't want to strictly follow JSF 2.x spec
                    }
                    else
                    {
                        // Origin Header does not match
                        return false;
                    }
                }
            }
            // In theory path = appContextPath + servletPath + pathInfo. 
            int appContextPathIndex = appContextPath != null ? path.indexOf(appContextPath) : -1;
            int servletPathIndex = -1;
            int pathInfoIndex = -1;
            if (ectx.getRequestServletPath() != null && ectx.getRequestPathInfo() != null)
            {
                servletPathIndex = ectx.getRequestServletPath() != null ? 
                    path.indexOf(ectx.getRequestServletPath(), 
                                 appContextPathIndex >= 0 ? appContextPathIndex : 0) : -1;
                if (servletPathIndex != -1)
                {
                    pathInfoIndex = servletPathIndex + ectx.getRequestServletPath().length();
                }
            }
            else
            {
                servletPathIndex = -1;
                pathInfoIndex = (appContextPathIndex >= 0 ? appContextPathIndex : 0) + appContextPath.length();
            }

            // If match appContextPath(if any) and match servletPath or pathInfo referer header is ok
            if ((appContextPath == null || appContextPathIndex >= 0) && 
                (servletPathIndex >= 0 || pathInfoIndex >= 0))
            {
                String refererViewId;
                if (pathInfoIndex >= 0)
                {
                    refererViewId = path.substring(pathInfoIndex);
                }
                else
                {
                    refererViewId = path.substring(servletPathIndex);
                }
                
                String logicalViewId = viewHandler.deriveViewId(facesContext, refererViewId);
                    
                // If the header is present, use the protected view API to determine if any of
                // the declared protected views match the value of the Referer header.
                // - If so, conclude that the previously visited page is also a protected 
                //   view and it is therefore safe to continue.
                // - Otherwise, try to determine if the value of the Referer header corresponds 
                //   to any of the views in the current web application.
                // -= Leonardo Uribe =- All views that are protected also should exists!. the
                // only relevant check is use ViewHandler.deriveViewId(...) here. Check if the
                // view is protected here is not necessary.
                if (logicalViewId != null)
                {
                    return true;
                }
                else
                {
                    // View do not exists
                }
            }
            return true;
        }
        catch (URISyntaxException ex)
        {
            return false;
        }
    }
    
    /**
     * Invoke afterPhase MethodExpression of UIViewRoot.
     * Note: In this phase it is not possible to invoke the beforePhase method, because we
     * first have to restore the view to get its attributes. Also it is not really possible
     * to call the afterPhase method inside of UIViewRoot for this phase, thus it was decided
     * in the JSF 2.0 spec rev A to put this here.
     * @param facesContext
     */
    private void _invokeViewRootAfterPhaseListener(FacesContext facesContext)
    {
        // get the UIViewRoot (note that it must not be null at this point)
        UIViewRoot root = facesContext.getViewRoot();
        MethodExpression afterPhaseExpression = root.getAfterPhaseListener();
        if (afterPhaseExpression != null)
        {
            PhaseEvent event = new PhaseEvent(facesContext, getPhase(), _getLifecycle(facesContext));
            try
            {
                afterPhaseExpression.invoke(facesContext.getELContext(), new Object[] { event });
            }
            catch (Throwable t) 
            {
                log.log(Level.SEVERE, "An Exception occured while processing " +
                        afterPhaseExpression.getExpressionString() + 
                        " in Phase " + getPhase(), t);
            }
        }
    }
    
    /**
     * Gets the current Lifecycle instance from the LifecycleFactory
     * @param facesContext
     * @return
     */
    private Lifecycle _getLifecycle(FacesContext facesContext)
    {
        LifecycleFactory factory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        String id = facesContext.getExternalContext().getInitParameter(FacesServlet.LIFECYCLE_ID_ATTR);
        if (id == null)
        {
            id = LifecycleFactory.DEFAULT_LIFECYCLE;
        }
        return factory.getLifecycle(id);  
    }
    
    protected RestoreViewSupport getRestoreViewSupport()
    {
        return getRestoreViewSupport(FacesContext.getCurrentInstance());
    }
    
    protected RestoreViewSupport getRestoreViewSupport(FacesContext context)
    {
        if (_restoreViewSupport == null)
        {
            _restoreViewSupport = new DefaultRestoreViewSupport(context);
        }
        return _restoreViewSupport;
    }

    protected RenderKitFactory getRenderKitFactory()
    {
        if (_renderKitFactory == null)
        {
            _renderKitFactory = (RenderKitFactory)FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        }
        return _renderKitFactory;
    }
    
    /**
     * @param restoreViewSupport
     *            the restoreViewSupport to set
     */
    public void setRestoreViewSupport(RestoreViewSupport restoreViewSupport)
    {
        _restoreViewSupport = restoreViewSupport;
    }

    public PhaseId getPhase()
    {
        return PhaseId.RESTORE_VIEW;
    }
    
    protected boolean checkViewNotFound(FacesContext facesContext)
    {
        if (_viewNotFoundCheck == null)
        {
            
            _viewNotFoundCheck = MyfacesConfig.getCurrentInstance(
                facesContext.getExternalContext()).isStrictJsf2ViewNotFound();
        }
        return _viewNotFoundCheck;
    }
    
    private void sendSourceNotFound(FacesContext context, String message)
    {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try
        {
            context.responseComplete();
            response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
        catch (IOException ioe)
        {
            throw new FacesException(ioe);
        }
    }
}
