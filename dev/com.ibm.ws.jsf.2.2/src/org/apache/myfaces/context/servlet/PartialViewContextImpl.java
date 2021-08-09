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

// Defects
// RTC 168859 / JIRA MYFACE-3973 - add id attribute to partial-response

package org.apache.myfaces.context.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlBody;
import javax.faces.component.html.HtmlHead;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitContextFactory;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;
import javax.faces.lifecycle.ClientWindow;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.ViewMetadata;

import org.apache.myfaces.context.PartialResponseWriterImpl;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.renderkit.html.HtmlResponseStateManager;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ExternalContextUtils;
import org.apache.myfaces.shared.util.StringUtils;

public class PartialViewContextImpl extends PartialViewContext
{

    private static final String FACES_REQUEST = "Faces-Request";
    private static final String PARTIAL_AJAX = "partial/ajax";
    private static final String PARTIAL_AJAX_REQ = "javax.faces.partial.ajax";
    private static final String PARTIAL_PROCESS = "partial/process";
    private static final String SOURCE_PARAM_NAME = "javax.faces.source";
    private static final String JAVAX_FACES_REQUEST = "javax.faces.request";

    /**
     * Internal extension for
     * https://issues.apache.org/jira/browse/MYFACES-2841
     * will be changed for 2.1 to the official marker
     */
    private static final String PARTIAL_IFRAME = "org.apache.myfaces.partial.iframe";
    
    private static final  Set<VisitHint> PARTIAL_EXECUTE_HINTS = Collections.unmodifiableSet( 
            EnumSet.of(VisitHint.EXECUTE_LIFECYCLE, VisitHint.SKIP_UNRENDERED));
    
    // unrendered have to be skipped, transient definitely must be added to our list!
    private static final  Set<VisitHint> PARTIAL_RENDER_HINTS = 
            Collections.unmodifiableSet(EnumSet.of(VisitHint.SKIP_UNRENDERED));

    private FacesContext _facesContext = null;
    private boolean _released = false;
    // Cached values, since their parent methods could be called
    // many times and the result does not change during the life time
    // of this object.
    private Boolean _ajaxRequest = null;

    /**
     * Internal extension for
     * https://issues.apache.org/jira/browse/MYFACES-2841
     * will be changed for 2.1 to the official marker
     */
    private Boolean _iframeRequest = null;

    private Collection<String> _executeClientIds = null;
    private Collection<String> _renderClientIds = null;
    // Values that need to be saved because exists a setXX method 
    private Boolean _partialRequest = null;
    private Boolean _renderAll = null;
    private PartialResponseWriter _partialResponseWriter = null;
    private VisitContextFactory _visitContextFactory = null;
    private Boolean _resetValues = null;

    public PartialViewContextImpl(FacesContext context)
    {
        _facesContext = context;
    }
    
    public PartialViewContextImpl(FacesContext context, 
            VisitContextFactory visitContextFactory)
    {
        _facesContext = context;
        _visitContextFactory = visitContextFactory;
    }

    @Override
    public boolean isAjaxRequest()
    {
        assertNotReleased();
        if (_ajaxRequest == null)
        {
            String requestType = _facesContext.getExternalContext().
                   getRequestHeaderMap().get(FACES_REQUEST);
            _ajaxRequest = (requestType != null && PARTIAL_AJAX.equals(requestType));
            String reqParmamterPartialAjax = _facesContext.getExternalContext().
                    getRequestParameterMap().get(PARTIAL_AJAX_REQ);
            //jsdoc reference in an ajax request the javax.faces.partial.ajax must be set as ajax parameter
            //the other one is Faces-Request == partial/ajax which is basically the same
            _ajaxRequest = _ajaxRequest || reqParmamterPartialAjax != null;
        }
        return _ajaxRequest;
    }

    @Override
    public boolean isExecuteAll()
    {
        assertNotReleased();

        if (isAjaxRequest())
        {
            String executeMode = _facesContext.getExternalContext().
                    getRequestParameterMap().get(
                    PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME);
            if (PartialViewContext.ALL_PARTIAL_PHASE_CLIENT_IDS.equals(executeMode))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPartialRequest()
    {
        assertNotReleased();

        if (_partialRequest == null)
        {
            String requestType = _facesContext.getExternalContext().
                    getRequestHeaderMap().get(FACES_REQUEST);
            _partialRequest = (requestType != null && PARTIAL_PROCESS.equals(requestType));
        }
        return _partialRequest || isAjaxRequest();
    }

    @Override
    public boolean isRenderAll()
    {
        assertNotReleased();

        if (_renderAll == null)
        {
            if (isAjaxRequest())
            {
                String executeMode = _facesContext.getExternalContext().
                        getRequestParameterMap().get(
                        PartialViewContext.PARTIAL_RENDER_PARAM_NAME);
                if (PartialViewContext.ALL_PARTIAL_PHASE_CLIENT_IDS.equals(executeMode))
                {
                    _renderAll = true;
                }
            }
            if (_renderAll == null)
            {
                _renderAll = false;
            }
        }
        return _renderAll;
    }

    /**
     * Extension for
     * https://issues.apache.org/jira/browse/MYFACES-2841
     * internal extension which detects that the submit is an iframe request
     * will be changed for the official version which will come in 2.1
     *
     * @return true if the current request is an iframe based ajax request
     */
    public boolean isIFrameRequest()
    {
        if (_iframeRequest == null)
        {
            _iframeRequest = _facesContext.getExternalContext().getRequestParameterMap().containsKey(PARTIAL_IFRAME);
        }
        return _iframeRequest;
    }

    @Override
    public void setPartialRequest(boolean isPartialRequest)
    {
        assertNotReleased();

        _partialRequest = isPartialRequest;

    }

    @Override
    public void setRenderAll(boolean renderAll)
    {
        assertNotReleased();

        _renderAll = renderAll;
    }

    @Override
    public Collection<String> getExecuteIds()
    {
        assertNotReleased();

        if (_executeClientIds == null)
        {
            String executeMode = _facesContext.getExternalContext().
                    getRequestParameterMap().get(
                    PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME);

            if (executeMode != null && !"".equals(executeMode) &&
                    //!PartialViewContext.NO_PARTIAL_PHASE_CLIENT_IDS.equals(executeMode) &&
                    !PartialViewContext.ALL_PARTIAL_PHASE_CLIENT_IDS.equals(executeMode))
            {

                String[] clientIds
                        = StringUtils.splitShortString(_replaceTabOrEnterCharactersWithSpaces(executeMode), ' ');

                //The collection must be mutable
                List<String> tempList = new ArrayList<String>();
                for (String clientId : clientIds)
                {
                    if (clientId.length() > 0)
                    {
                        tempList.add(clientId);
                    }
                }
                // The "javax.faces.source" parameter needs to be added to the list of
                // execute ids if missing (otherwise, we'd never execute an action associated
                // with, e.g., a button).

                String source = _facesContext.getExternalContext().getRequestParameterMap().get
                        (PartialViewContextImpl.SOURCE_PARAM_NAME);

                if (source != null)
                {
                    source = source.trim();

                    if (!tempList.contains(source))
                    {
                        tempList.add(source);
                    }
                }

                _executeClientIds = tempList;
            }
            else
            {
                _executeClientIds = new ArrayList<String>();
            }
        }
        return _executeClientIds;
    }

    private String _replaceTabOrEnterCharactersWithSpaces(String mode)
    {
        StringBuilder builder = new StringBuilder(mode.length());
        for (int i = 0; i < mode.length(); i++)
        {
            if (mode.charAt(i) == '\t' ||
                    mode.charAt(i) == '\n')
            {
                builder.append(' ');
            }
            else
            {
                builder.append(mode.charAt(i));
            }
        }
        return builder.toString();
    }

    @Override
    public Collection<String> getRenderIds()
    {
        assertNotReleased();

        if (_renderClientIds == null)
        {
            String renderMode = _facesContext.getExternalContext().
                    getRequestParameterMap().get(
                    PartialViewContext.PARTIAL_RENDER_PARAM_NAME);

            if (renderMode != null && !"".equals(renderMode) &&
                    //!PartialViewContext.NO_PARTIAL_PHASE_CLIENT_IDS.equals(renderMode) &&
                    !PartialViewContext.ALL_PARTIAL_PHASE_CLIENT_IDS.equals(renderMode))
            {
                String[] clientIds
                        = StringUtils.splitShortString(_replaceTabOrEnterCharactersWithSpaces(renderMode), ' ');

                //The collection must be mutable
                List<String> tempList = new ArrayList<String>();
                for (String clientId : clientIds)
                {
                    if (clientId.length() > 0)
                    {
                        tempList.add(clientId);
                    }
                }
                _renderClientIds = tempList;
            }
            else
            {
                _renderClientIds = new ArrayList<String>();

                if (PartialViewContext.ALL_PARTIAL_PHASE_CLIENT_IDS.equals(renderMode))
                {
                    _renderClientIds.add(PartialResponseWriter.RENDER_ALL_MARKER);
                }
            }
        }
        return _renderClientIds;
    }

    @Override
    public PartialResponseWriter getPartialResponseWriter()
    {
        assertNotReleased();

        if (_partialResponseWriter == null)
        {
            ResponseWriter responseWriter = _facesContext.getResponseWriter();
            if (responseWriter == null)
            {
                // This case happens when getPartialResponseWriter() is called before
                // render phase, like in ExternalContext.redirect(). We have to create a
                // ResponseWriter from the RenderKit and then wrap if necessary. 
                try
                {
                    RenderKit renderKit = _facesContext.getRenderKit();
                    if (renderKit == null)
                    {
                        // If the viewRoot was set to null by some reason, or there is no 
                        // renderKitId on that view, this could be still an ajax redirect,
                        // so we have to try to calculate the renderKitId and return a 
                        // RenderKit instance, to send the response.
                        String renderKitId
                                = _facesContext.getApplication().getViewHandler().calculateRenderKitId(_facesContext);
                        RenderKitFactory rkf
                                = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
                        renderKit = rkf.getRenderKit(_facesContext, renderKitId);
                    }
                    responseWriter = renderKit.createResponseWriter(
                            _facesContext.getExternalContext().getResponseOutputWriter(), "text/xml",
                            _facesContext.getExternalContext().getRequestCharacterEncoding());
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("Cannot create Partial Response Writer", e);
                }
            }
            // It is possible that the RenderKit return a PartialResponseWriter instance when 
            // createResponseWriter,  so we should cast here for it and prevent double wrapping.
            if (responseWriter instanceof PartialResponseWriter)
            {
                _partialResponseWriter = (PartialResponseWriter) responseWriter;
            }
            else
            {
                _partialResponseWriter = new PartialResponseWriterImpl(responseWriter);
            }
        }
        return _partialResponseWriter;
    }

    /**
     * process the partial response
     * allowed phase ids according to the spec
     *
     *
     */
    @Override
    public void processPartial(PhaseId phaseId)
    {
        assertNotReleased();

        UIViewRoot viewRoot = _facesContext.getViewRoot();

        if (phaseId == PhaseId.APPLY_REQUEST_VALUES
                || phaseId == PhaseId.PROCESS_VALIDATIONS
                || phaseId == PhaseId.UPDATE_MODEL_VALUES)
        {
            processPartialExecute(viewRoot, phaseId);
        }
        else if (phaseId == PhaseId.RENDER_RESPONSE)
        {
            processPartialRendering(viewRoot, phaseId);
        }
    }

    private void processPartialExecute(UIViewRoot viewRoot, PhaseId phaseId)
    {
        PartialViewContext pvc = _facesContext.getPartialViewContext();
        Collection<String> executeIds = pvc.getExecuteIds();
        if (executeIds == null || executeIds.isEmpty())
        {
            return;
        }
        
        VisitContext visitCtx = getVisitContextFactory().getVisitContext(_facesContext, executeIds, 
                PARTIAL_EXECUTE_HINTS);
        viewRoot.visitTree(visitCtx, new PhaseAwareVisitCallback(_facesContext, phaseId));
    }

    private void processPartialRendering(UIViewRoot viewRoot, PhaseId phaseId)
    {
        //TODO process partial rendering
        //https://issues.apache.org/jira/browse/MYFACES-2118
        //Collection<String> renderIds = getRenderIds();

        // We need to always update the view state marker when processing partial
        // rendering, because there is no way to check when the state has been changed
        // or not. Anyway, if we return empty response, according to the spec a javascript
        // message displayed, so we need to return something.
        //if (renderIds == null || renderIds.isEmpty()) {
        //    return;
        //}

        // note that we cannot use this.getPartialResponseWriter(), because
        // this could cause problems if PartialResponseWriter is wrapped
        PartialResponseWriter writer = _facesContext.getPartialViewContext().getPartialResponseWriter();
        PartialViewContext pvc = _facesContext.getPartialViewContext();

        ResponseWriter oldWriter = _facesContext.getResponseWriter();
        boolean inDocument = false;

        //response type = text/xml
        //no caching and no timeout if possible!
        ExternalContext externalContext = _facesContext.getExternalContext();
        externalContext.setResponseContentType("text/xml");
        externalContext.addResponseHeader("Pragma", "no-cache");
        externalContext.addResponseHeader("Cache-control", "no-cache");
        //under normal circumstances pragma should be enough, IE needs
        //a special treatment!
        //http://support.microsoft.com/kb/234067
        externalContext.addResponseHeader("Expires", "-1");

        try
        {
            String currentEncoding = writer.getCharacterEncoding();
            writer.writePreamble("<?xml version=\"1.0\" encoding=\""+
                (currentEncoding == null ? "UTF-8" : currentEncoding) +"\"?>");
            writer.startDocument();
            
            writer.writeAttribute("id", viewRoot.getContainerClientId(_facesContext),"id"); //RTC 168859 / JIRA MYFACE-3973 
            
            inDocument = true;
            _facesContext.setResponseWriter(writer);
            
            if (isResetValues())
            {
                viewRoot.resetValues(_facesContext, getRenderIds());
            }

            if (pvc.isRenderAll())
            {
                processRenderAll(viewRoot, writer);
            }
            else
            {
                Collection<String> renderIds = pvc.getRenderIds();
                //Only apply partial visit if we have ids to traverse
                if (renderIds != null && !renderIds.isEmpty())
                {
                    // render=@all, so output the body.
                    if (renderIds.contains(PartialResponseWriter.RENDER_ALL_MARKER))
                    {
                        processRenderAll(viewRoot, writer);
                    }
                    else
                    {
                        List<UIComponent> updatedComponents = null;
                        if (!ExternalContextUtils.isPortlet(_facesContext.getExternalContext()) &&
                                MyfacesConfig.getCurrentInstance(externalContext).isStrictJsf2RefreshTargetAjax())
                        {
                            RequestViewContext rvc = RequestViewContext.getCurrentInstance(_facesContext);
                            if (rvc.isRenderTarget("head"))
                            {
                                UIComponent head = findHeadComponent(viewRoot);
                                if (head != null)
                                {
                                    writer.startUpdate("javax.faces.ViewHead");
                                    head.encodeAll(_facesContext);
                                    writer.endUpdate();
                                    if (updatedComponents == null)
                                    {
                                        updatedComponents = new ArrayList<UIComponent>();
                                    }
                                    updatedComponents.add(head);
                                }
                            }
                            if (rvc.isRenderTarget("body") || rvc.isRenderTarget("form"))
                            {
                                UIComponent body = findBodyComponent(viewRoot);
                                if (body != null)
                                {
                                    writer.startUpdate("javax.faces.ViewBody");
                                    body.encodeAll(_facesContext);
                                    writer.endUpdate();
                                    if (updatedComponents == null)
                                    {
                                        updatedComponents = new ArrayList<UIComponent>();
                                    }
                                    updatedComponents.add(body);
                                }
                            }
                        }

                        VisitContext visitCtx = getVisitContextFactory().getVisitContext(
                                _facesContext, renderIds, PARTIAL_RENDER_HINTS);
                        viewRoot.visitTree(visitCtx,
                                           new PhaseAwareVisitCallback(_facesContext, phaseId, updatedComponents));
                    }
                }
                else if (!ExternalContextUtils.isPortlet(_facesContext.getExternalContext()) &&
                        MyfacesConfig.getCurrentInstance(externalContext).isStrictJsf2RefreshTargetAjax())
                {
                    RequestViewContext rvc = RequestViewContext.getCurrentInstance(_facesContext);
                    if (rvc.isRenderTarget("head"))
                    {
                        UIComponent head = findHeadComponent(viewRoot);
                        if (head != null)
                        {
                            writer.startUpdate("javax.faces.ViewHead");
                            head.encodeAll(_facesContext);
                            writer.endUpdate();
                        }
                    }
                    if (rvc.isRenderTarget("body") || rvc.isRenderTarget("form"))
                    {
                        UIComponent body = findBodyComponent(viewRoot);
                        if (body != null)
                        {
                            writer.startUpdate("javax.faces.ViewBody");
                            body.encodeAll(_facesContext);
                            writer.endUpdate();
                        }
                    }
                }
            }

            // invoke encodeAll() on every UIViewParameter in the view to 
            // enable every UIViewParameter to save its value in the state
            // just like UIViewRoot.encodeEnd() does on a normal request
            // (see MYFACES-2645 for details)
            Collection<UIViewParameter> viewParams = ViewMetadata.getViewParameters(viewRoot);
            if (!viewParams.isEmpty())
            {
                for (UIViewParameter param : viewParams)
                {
                    param.encodeAll(_facesContext);
                }
            }

            //Retrieve the state and apply it if it is not null.
            String viewState = _facesContext.getApplication().getStateManager().getViewState(_facesContext);
            if (viewState != null)
            {
                writer.startUpdate(HtmlResponseStateManager.generateUpdateViewStateId(
                    _facesContext));
                writer.write(viewState);
                writer.endUpdate();
            }
            
            
            ClientWindow cw = _facesContext.getExternalContext().getClientWindow();
            if (cw != null)
            {
                writer.startUpdate(HtmlResponseStateManager.generateUpdateClientWindowId(
                    _facesContext));
                writer.write(cw.getId());
                writer.endUpdate();
            }
        }
        catch (IOException ex)
        {
            Logger log = Logger.getLogger(PartialViewContextImpl.class.getName());
            if (log.isLoggable(Level.SEVERE))
            {
                log.log(Level.SEVERE, "", ex);
            }

        }
        finally
        {
            try
            {
                if (inDocument)
                {
                    writer.endDocument();
                }
                writer.flush();
            }
            catch (IOException ex)
            {
                Logger log = Logger.getLogger(PartialViewContextImpl.class.getName());
                if (log.isLoggable(Level.SEVERE))
                {
                    log.log(Level.SEVERE, "", ex);
                }
            }

            _facesContext.setResponseWriter(oldWriter);
        }

    }

    private void processRenderAll(UIViewRoot viewRoot, PartialResponseWriter writer) throws IOException
    {
        //java.util.Iterator<UIComponent> iter = viewRoot.getFacetsAndChildren();
        writer.startUpdate(PartialResponseWriter.RENDER_ALL_MARKER);
        //while (iter.hasNext()) 
        //{ 
        //UIComponent comp = iter.next();

        //TODO: Do not check for a specific instance,
        //just render all children.
        //if (comp instanceof javax.faces.component.html.HtmlBody)
        //{
        //comp.encodeAll (_facesContext);
        //}
        //}
        for (int i = 0, childCount = viewRoot.getChildCount(); i < childCount; i++)
        {
            UIComponent comp = viewRoot.getChildren().get(i);
            comp.encodeAll(_facesContext);
        }
        writer.endUpdate();
    }

    /**
     * has to be thrown in many of the methods if the method is called after the instance has been released!
     */
    private void assertNotReleased()
    {
        if (_released)
        {
            throw new IllegalStateException("Error the FacesContext is already released!");
        }
    }

    @Override
    public void release()
    {
        assertNotReleased();
        _visitContextFactory = null;
        _executeClientIds = null;
        _renderClientIds = null;
        _ajaxRequest = null;
        _partialRequest = null;
        _renderAll = null;
        _facesContext = null;
        _released = true;
    }

    private UIComponent findHeadComponent(UIViewRoot root)
    {
        for (UIComponent child : root.getChildren())
        {
            if (child instanceof HtmlHead)
            {
                return child;
            }
            else
            {
                for (UIComponent grandchild : child.getChildren())
                {
                    if (grandchild instanceof HtmlHead)
                    {
                        return grandchild;
                    }
                }
            }
        }
        return null;
    }

    private UIComponent findBodyComponent(UIViewRoot root)
    {
        for (UIComponent child : root.getChildren())
        {
            if (child instanceof HtmlBody)
            {
                return child;
            }
            else
            {
                for (UIComponent grandchild : child.getChildren())
                {
                    if (grandchild instanceof HtmlBody)
                    {
                        return grandchild;
                    }
                }
            }
        }
        return null;
    }
    
    private VisitContextFactory getVisitContextFactory()
    {
        if (_visitContextFactory == null)
        {
            _visitContextFactory = (VisitContextFactory)FactoryFinder.getFactory(FactoryFinder.VISIT_CONTEXT_FACTORY);
        }
        return _visitContextFactory;
    }

    @Override
    public boolean isResetValues()
    {
        if (_resetValues == null)
        {
            String value = _facesContext.getExternalContext().getRequestParameterMap().
                get(RESET_VALUES_PARAM_NAME);
            _resetValues = "true".equals(value);
        }
        return _resetValues;
    }

    private class PhaseAwareVisitCallback implements VisitCallback
    {

        private PhaseId _phaseId;
        private FacesContext _facesContext;
        private List<UIComponent> _alreadyUpdatedComponents;

        public PhaseAwareVisitCallback(FacesContext facesContext, PhaseId phaseId)
        {
            this._phaseId = phaseId;
            this._facesContext = facesContext;
            this._alreadyUpdatedComponents = null;
        }

        public PhaseAwareVisitCallback(FacesContext facesContext, PhaseId phaseId,
                                       List<UIComponent> alreadyUpdatedComponents)
        {
            this._phaseId = phaseId;
            this._facesContext = facesContext;
            this._alreadyUpdatedComponents = alreadyUpdatedComponents;
        }

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (_phaseId == PhaseId.APPLY_REQUEST_VALUES)
            {
                target.processDecodes(_facesContext);
            }
            else if (_phaseId == PhaseId.PROCESS_VALIDATIONS)
            {
                target.processValidators(_facesContext);
            }
            else if (_phaseId == PhaseId.UPDATE_MODEL_VALUES)
            {
                target.processUpdates(_facesContext);
            }
            else if (_phaseId == PhaseId.RENDER_RESPONSE)
            {
                processRenderComponent(target);
            }
            else
            {
                throw new IllegalStateException("PPR Response, illegale phase called");
            }

            // Return VisitResult.REJECT as processDecodes/Validators/Updates already traverse sub tree
            return VisitResult.REJECT;
        }

        /**
         * the rendering subpart of the tree walker
         * every component id which is passed down via render must be handled
         * here!
         *
         * @param target the target component to be handled!
         */
        private void processRenderComponent(UIComponent target)
        {
            boolean inUpdate = false;
            PartialResponseWriter writer = (PartialResponseWriter) _facesContext.getResponseWriter();
            if (this._alreadyUpdatedComponents != null)
            {
                //Check if the parent was already updated.
                UIComponent parent = target;
                while (parent != null)
                {
                    if (this._alreadyUpdatedComponents.contains(parent))
                    {
                        return;
                    }
                    parent = parent.getParent();
                }
            }
            try
            {
                writer.startUpdate(target.getClientId(_facesContext));
                inUpdate = true;
                target.encodeAll(_facesContext);
            }
            catch (IOException ex)
            {
                Logger log = Logger.getLogger(PartialViewContextImpl.class.getName());
                if (log.isLoggable(Level.SEVERE))
                {
                    log.log(Level.SEVERE, "IOException for rendering component", ex);
                }
            }
            finally
            {
                if (inUpdate)
                {
                    try
                    {
                        writer.endUpdate();
                    }
                    catch (IOException ex)
                    {
                        Logger log = Logger.getLogger(PartialViewContextImpl.class.getName());
                        if (log.isLoggable(Level.SEVERE))
                        {
                            log.log(Level.SEVERE, "IOException for rendering component, stopping update rendering", ex);
                        }
                    }
                }
            }
        }
    }
}
