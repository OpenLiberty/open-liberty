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
package org.apache.myfaces.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewVisitOption;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewDeclarationLanguageFactory;
import javax.faces.view.ViewMetadata;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.application.viewstate.StateCacheUtils;
import org.apache.myfaces.shared.application.DefaultViewHandlerSupport;
import org.apache.myfaces.shared.application.InvalidViewIdException;
import org.apache.myfaces.shared.application.ViewHandlerSupport;
import org.apache.myfaces.view.facelets.StateWriter;

/**
 * JSF 2.0 ViewHandler implementation 
 *
 * @since 2.0
 */
public class ViewHandlerImpl extends ViewHandler
{
    private static final Logger log = Logger.getLogger(ViewHandlerImpl.class.getName());
    
    public static final String FORM_STATE_MARKER = "<!--@@JSF_FORM_STATE_MARKER@@-->";
    private ViewHandlerSupport _viewHandlerSupport;
    private ViewDeclarationLanguageFactory _vdlFactory;
    
    private Set<String> _protectedViewsSet;
    private Set<String> _unmodifiableProtectedViewsSet;
    
    /**
     * Gets the current ViewHandler via FacesContext.getApplication().getViewHandler().
     * We have to use this method to invoke any other specified ViewHandler-method
     * in the code, because direct access (this.method()) will cause problems if
     * the ViewHandler is wrapped.
     * @param facesContext
     * @return
     */
    public static ViewHandler getViewHandler(FacesContext facesContext)
    {
        return facesContext.getApplication().getViewHandler();
    }

    public ViewHandlerImpl()
    {
        _protectedViewsSet = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
        _unmodifiableProtectedViewsSet = Collections.unmodifiableSet(_protectedViewsSet);
        _vdlFactory = (ViewDeclarationLanguageFactory)
                FactoryFinder.getFactory(FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY);
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New ViewHandler instance created");
        }
    }

    @Override
    public String deriveViewId(FacesContext context, String input)
    {
        if(input != null)
        {
            try
            {
                return getViewHandlerSupport(context).calculateAndCheckViewId(context, input);
            }
            catch (InvalidViewIdException e)
            {
                sendSourceNotFound(context, e.getMessage());
            }
        }
        return input;   // If the argument input is null, return null.
    }
    
    @Override
    public String deriveLogicalViewId(FacesContext context, String rawViewId)
    {
        if(rawViewId != null)
        {
            try
            {
                return getViewHandlerSupport(context).calculateViewId(context, rawViewId);
            }
            catch (InvalidViewIdException e)
            {
                sendSourceNotFound(context, e.getMessage());
            }
        }
        return rawViewId;   // If the argument input is null, return null.
    }

    @Override
    public String getBookmarkableURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        Map<String, List<String>> viewParameters;
        if (includeViewParams)
        {
            viewParameters = getViewParameterList(context, viewId, parameters);
        }
        else
        {
            viewParameters = parameters;
        }
        
        // note that we cannot use this.getActionURL(), because this will
        // cause problems if the ViewHandler is wrapped
        String actionEncodedViewId = getViewHandler(context).getActionURL(context, viewId);
        
        ExternalContext externalContext = context.getExternalContext();
        String bookmarkEncodedURL = externalContext.encodeBookmarkableURL(actionEncodedViewId, viewParameters);
        return externalContext.encodeActionURL(bookmarkEncodedURL);
    }

    @Override
    public String getRedirectURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        Map<String, List<String>> viewParameters;
        if (includeViewParams)
        {
            viewParameters = getViewParameterList(context, viewId, parameters);
        }
        else
        {
            viewParameters = parameters;
        }
        
        // note that we cannot use this.getActionURL(), because this will
        // cause problems if the ViewHandler is wrapped
        String actionEncodedViewId = getViewHandler(context).getActionURL(context, viewId);
        
        ExternalContext externalContext = context.getExternalContext();
        String redirectEncodedURL = externalContext.encodeRedirectURL(actionEncodedViewId, viewParameters);
        return externalContext.encodeActionURL(redirectEncodedURL);
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(
            FacesContext context, String viewId)
    {
        // return a suitable ViewDeclarationLanguage implementation for the given viewId
        return _vdlFactory.getViewDeclarationLanguage(viewId);
    }

    @Override
    public void initView(FacesContext context) throws FacesException
    {
        if(context.getExternalContext().getRequestCharacterEncoding() == null)
        {
            super.initView(context);    
        }
    }

    /**
     * Get the locales specified as acceptable by the original request, compare them to the
     * locales supported by this Application and return the best match.
     */
    @Override
    public Locale calculateLocale(FacesContext facesContext)
    {
        Application application = facesContext.getApplication();
        for (Iterator<Locale> requestLocales = facesContext.getExternalContext().getRequestLocales(); requestLocales
                .hasNext();)
        {
            Locale requestLocale = requestLocales.next();
            for (Iterator<Locale> supportedLocales = application.getSupportedLocales(); supportedLocales.hasNext();)
            {
                Locale supportedLocale = supportedLocales.next();
                // higher priority to a language match over an exact match
                // that occurs further down (see JSTL Reference 1.0 8.3.1)
                if (requestLocale.getLanguage().equals(supportedLocale.getLanguage())
                        && (supportedLocale.getCountry() == null || supportedLocale.getCountry().length() == 0))
                {
                    return supportedLocale;
                }
                else if (supportedLocale.equals(requestLocale))
                {
                    return supportedLocale;
                }
            }
        }

        Locale defaultLocale = application.getDefaultLocale();
        return defaultLocale != null ? defaultLocale : Locale.getDefault();
    }

    @Override
    public String calculateRenderKitId(FacesContext facesContext)
    {
        Object renderKitId = facesContext.getExternalContext().getRequestMap().get(
                ResponseStateManager.RENDER_KIT_ID_PARAM);
        if (renderKitId == null)
        {
            renderKitId = facesContext.getApplication().getDefaultRenderKitId();
        }
        if (renderKitId == null)
        {
            renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
        }
        return renderKitId.toString();
    }
    
    @Override
    public UIViewRoot createView(FacesContext context, String viewId)
    {
       checkNull(context, "facesContext");
       String calculatedViewId = getViewHandlerSupport(context).calculateViewId(context, viewId);
       
       // we cannot use this.getVDL() directly (see getViewHandler())
       //return getViewHandler(context)
       //        .getViewDeclarationLanguage(context, calculatedViewId)
       //            .createView(context, calculatedViewId);
       // -= Leonardo Uribe =- Temporally reverted by TCK issues.
       ViewDeclarationLanguage vdl = getViewDeclarationLanguage(context,calculatedViewId);
       if (vdl == null)
       {
           // If there is no VDL that can handle the view, throw 404 response.
           sendSourceNotFound(context, viewId);
           return null;
       }
       return vdl.createView(context,calculatedViewId);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId)
    {
        checkNull(context, "facesContext");
        checkNull(viewId, "viewId");
        return getViewHandlerSupport(context).calculateActionURL(context, viewId);
    }

    @Override
    public String getResourceURL(FacesContext facesContext, String path)
    {
        checkNull(facesContext, "facesContext");
        checkNull(path, "path");
        if (path.length() > 0 && path.charAt(0) == '/')
        {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            if (contextPath == null)
            {
                return path;
            }
            else if (contextPath.length() == 1 && contextPath.charAt(0) == '/')
            {
                // If the context path is root, it is not necessary to append it, otherwise
                // and extra '/' will be set.
                return path;
            }
            else
            {
                return  contextPath + path;
            }
        }
        return path;

    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender)
            throws IOException, FacesException
    {

        checkNull(context, "context");
        checkNull(viewToRender, "viewToRender");

        // we cannot use this.getVDL() directly (see getViewHandler())
        //String viewId = viewToRender.getViewId();
        //getViewHandler(context).getViewDeclarationLanguage(context, viewId)
        //        .renderView(context, viewToRender);
        // -= Leonardo Uribe =- Temporally reverted by TCK issues.
        getViewDeclarationLanguage(context,viewToRender.getViewId()).renderView(context, viewToRender);
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        checkNull(context, "context");
    
        String calculatedViewId = getViewHandlerSupport(context).calculateViewId(context, viewId);
        
        // we cannot use this.getVDL() directly (see getViewHandler())
        //return getViewHandler(context)
        //        .getViewDeclarationLanguage(context,calculatedViewId)
        //            .restoreView(context, calculatedViewId);
        // -= Leonardo Uribe =- Temporally reverted by TCK issues.
        ViewDeclarationLanguage vdl = getViewDeclarationLanguage(context,calculatedViewId);
        if (vdl == null)
        {
            // If there is no VDL that can handle the view, throw 404 response.
            sendSourceNotFound(context, viewId);
            return null;
            
        }
        return vdl.restoreView(context, calculatedViewId); 
    }
    
    @Override
    public void writeState(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        if(context.getPartialViewContext().isAjaxRequest())
        {
            return;
        }

        ResponseStateManager responseStateManager = context.getRenderKit().getResponseStateManager();
        
        setWritingState(context, responseStateManager);

        StateManager stateManager = context.getApplication().getStateManager();
        
        // By the spec, it is necessary to use a writer to write FORM_STATE_MARKER, 
        // after the view is rendered, to preserve changes done on the component tree
        // on rendering time. But if server side state saving is used, this is not 
        // really necessary, because a token could be used and after the view is
        // rendered, a simple call to StateManager.saveState() could do the trick.
        // The code below check if we are using MyFacesResponseStateManager and if
        // that so, check if the current one support the trick.
        if (StateCacheUtils.isMyFacesResponseStateManager(responseStateManager))
        {
            if (StateCacheUtils.getMyFacesResponseStateManager(responseStateManager).
                    isWriteStateAfterRenderViewRequired(context))
            {
                // Only write state marker if javascript view state is disabled
                context.getResponseWriter().write(FORM_STATE_MARKER);
            }
            else
            {
                stateManager.writeState(context, new Object[2]);
            }
        }
        else
        {
            // Only write state marker if javascript view state is disabled
            context.getResponseWriter().write(FORM_STATE_MARKER);
        }
    }

    @Override
    public void addProtectedView(String urlPattern)
    {
        _protectedViewsSet.add(urlPattern);
    }

    @Override
    public boolean removeProtectedView(String urlPattern)
    {
        return _protectedViewsSet.remove(urlPattern);
    }

    @Override
    public Set<String> getProtectedViewsUnmodifiable()
    {
        return _unmodifiableProtectedViewsSet;
    }
    
    private void setWritingState(FacesContext context, ResponseStateManager rsm)
    {
        // Facelets specific hack:
        // Tell the StateWriter that we're about to write state
        StateWriter stateWriter = StateWriter.getCurrentInstance(context);
        if (stateWriter != null)
        {
            // Write the STATE_KEY out. Unfortunately, this will
            // be wasteful for pure server-side state managers where nothing
            // is actually written into the output, but this cannot
            // programatically be discovered
            // -= Leonardo Uribe =- On MyFacesResponseStateManager was added
            // some methods to discover it programatically.
            if (StateCacheUtils.isMyFacesResponseStateManager(rsm))
            {
                if (StateCacheUtils.getMyFacesResponseStateManager(rsm).isWriteStateAfterRenderViewRequired(context))
                {
                    stateWriter.writingState();
                }
                else
                {
                    stateWriter.writingStateWithoutWrapper();
                }
            }
            else
            {
                stateWriter.writingState();
            }
            
            
        }
        //else
        //{
            //we're in a JSP, let the JSPStatemanager know that we need to actually write the state
        //}        
    }
    
    private Map<String, List<String>> getViewParameterList(FacesContext context,
            String viewId, Map<String, List<String>> parametersFromArg)
    {
        UIViewRoot viewRoot = context.getViewRoot();
        String currentViewId = viewRoot.getViewId();
        Collection<UIViewParameter> toViewParams = null;
        Collection<UIViewParameter> currentViewParams = ViewMetadata.getViewParameters(viewRoot);

        if (currentViewId.equals(viewId))
        {
            toViewParams = currentViewParams;
        }
        else
        {
            String calculatedViewId = getViewHandlerSupport(context).calculateViewId(context, viewId);  
            // we cannot use this.getVDL() directly (see getViewHandler())
            //ViewDeclarationLanguage vdl = getViewHandler(context).
            //        getViewDeclarationLanguage(context, calculatedViewId);
            // -= Leonardo Uribe =- Temporally reverted by TCK issues.
            ViewDeclarationLanguage vdl = getViewDeclarationLanguage(context,calculatedViewId);
            ViewMetadata viewMetadata = vdl.getViewMetadata(context, viewId);
            // getViewMetadata() returns null on JSP
            if (viewMetadata != null)
            {
                UIViewRoot viewFromMetaData = viewMetadata.createMetadataView(context);
                toViewParams = ViewMetadata.getViewParameters(viewFromMetaData);
            }
        }

        if (toViewParams == null || toViewParams.isEmpty())
        {
            return parametersFromArg;
        }
        
        // we need to use a custom Map to add the view parameters,
        // otherwise the current value of the view parameter will be added to
        // the navigation case as a static (!!!) parameter, thus the value
        // won't be updated on any following request
        // (Note that parametersFromArg is the Map from the NavigationCase)
        // Also note that we don't have to copy the Lists, because they won't be changed
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        parameters.putAll(parametersFromArg);

        for (UIViewParameter viewParameter : toViewParams)
        {
            if (!parameters.containsKey(viewParameter.getName()))
            {
                String parameterValue = viewParameter.getStringValueFromModel(context);
                if (parameterValue == null)
                {
                    if(currentViewId.equals(viewId))
                    {
                        parameterValue = viewParameter.getStringValue(context);
                    }
                    else
                    {
                        if (viewParameter.getName() != null)
                        {
                            for (UIViewParameter curParam : currentViewParams)
                            {
                                if (viewParameter.getName().equals(curParam.getName())) 
                                {
                                    parameterValue = curParam.getStringValue(context);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (parameterValue != null)
                {
                    // since we have checked !parameters.containsKey(viewParameter.getName())
                    // here already, the parameters Map will never contain a List under the
                    // key viewParameter.getName(), thus we do not have to check it here (again).
                    List<String> parameterValueList = new ArrayList<String>();
                    parameterValueList.add(parameterValue);
                    parameters.put(viewParameter.getName(), parameterValueList);
                }
            }
        }        
        return parameters;
    }
    
    private void checkNull(final Object o, final String param)
    {
        if (o == null)
        {
            throw new NullPointerException(param + " can not be null.");
        }
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
    
    public void setViewHandlerSupport(ViewHandlerSupport viewHandlerSupport)
    {
        _viewHandlerSupport = viewHandlerSupport;
    }    
    
    protected ViewHandlerSupport getViewHandlerSupport()
    {
        return getViewHandlerSupport(FacesContext.getCurrentInstance());
    }

    protected ViewHandlerSupport getViewHandlerSupport(FacesContext context)
    {
        if (_viewHandlerSupport == null)
        {
            _viewHandlerSupport = new DefaultViewHandlerSupport(context);
        }
        return _viewHandlerSupport;
    }

    @Override
    public Stream<String> getViews(FacesContext facesContext, String path, int maxDepth, ViewVisitOption... options)
    {
        Stream concatenatedStream = null;
        for (ViewDeclarationLanguage vdl : _vdlFactory.getAllViewDeclarationLanguages())
        {
            Stream stream = vdl.getViews(facesContext, path, maxDepth, options);
            if (concatenatedStream == null)
            {
                concatenatedStream = stream;
            }
            else
            {
                concatenatedStream = Stream.concat(concatenatedStream, stream);
            }
        }
        return concatenatedStream == null ? Stream.empty() : concatenatedStream;
    }
    
    @Override
    public String getWebsocketURL(FacesContext context, String channelAndToken)
    {
        String url = context.getExternalContext().getRequestContextPath() + 
                "/javax.faces.push/"+channelAndToken;
        return url;
    }
    
}
