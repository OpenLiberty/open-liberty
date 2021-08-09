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
package org.apache.myfaces.view.facelets.tag.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.ProjectStage;
import javax.faces.application.StateManager;
import javax.faces.component.UIComponent;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.VariableMapperWrapper;
import org.apache.myfaces.view.facelets.impl.TemplateContextImpl;
import org.apache.myfaces.view.facelets.tag.ComponentContainerHandler;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * The include tag can point at any Facelet which might use the composition tag,
 * component tag, or simply be straight XHTML/XML. It should be noted that the 
 * src path does allow relative path names, but they will always be resolved 
 * against the original Facelet requested. 
 * 
 * The include tag can be used in conjunction with multiple &lt;ui:param/&gt; 
 * tags to pass EL expressions/values to the target page.
 * 
 * @author Jacob Hookom
 * @version $Id: IncludeHandler.java 1576792 2014-03-12 15:57:38Z lu4242 $
 */
@JSFFaceletTag(name="ui:include", bodyContent="JSP")
public final class IncludeHandler extends TagHandler implements ComponentContainerHandler
{

    private static final String ERROR_PAGE_INCLUDE_PATH = "javax.faces.error.xhtml";
    private static final String ERROR_FACELET = "META-INF/rsc/myfaces-dev-error-include.xhtml";
    
    /**
     * A literal or EL expression that specifies the target Facelet that you 
     * would like to include into your document.
     */
    @JSFFaceletAttribute(
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    private final TagAttribute src;
    
    private final ParamHandler[] _params;

    /**
     * @param config
     */
    public IncludeHandler(TagConfig config)
    {
        super(config);
        this.src = this.getRequiredAttribute("src");
        
        Collection<ParamHandler> params = TagHandlerUtils.findNextByType(nextHandler, ParamHandler.class);
        if (!params.isEmpty())
        {
            int i = 0;
            _params = new ParamHandler[params.size()];
            for (ParamHandler handler : params)
            {
                _params[i++] = handler;
            }
        }
        else
        {
            _params = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, javax.faces.component.UIComponent)
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
        String path;
        boolean markInitialState = false;
        String uniqueId = null;
        if (!src.isLiteral())
        {
            uniqueId = actx.generateUniqueFaceletTagId(
                fcc.startComponentUniqueIdSection(), tagId);
        }
        else if (_params != null)
        {
            uniqueId = actx.generateUniqueFaceletTagId(
                fcc.generateUniqueComponentId(), tagId);
        }
        if (!src.isLiteral())
        {
            //String uniqueId = fcc.startComponentUniqueIdSection();
            //path = getSrcValue(actx, fcc, parent, uniqueId);
            String restoredPath = (String) ComponentSupport.restoreInitialTagState(ctx, fcc, parent, uniqueId);
            if (restoredPath != null)
            {
                // If is not restore view phase, the path value should be
                // evaluated and if is not equals, trigger markInitialState stuff.
                if (!PhaseId.RESTORE_VIEW.equals(ctx.getFacesContext().getCurrentPhaseId()))
                {
                    path = this.src.getValue(ctx);
                    if (path == null || path.length() == 0)
                    {
                        return;
                    }
                    if (!path.equals(restoredPath))
                    {
                        markInitialState = true;
                    }
                }
                else
                {
                    path = restoredPath;
                }
            }
            else
            {
                //No state restored, calculate path
                path = this.src.getValue(ctx);
            }
            ComponentSupport.saveInitialTagState(ctx, fcc, parent, uniqueId, path);
        }
        else
        {
            path = this.src.getValue(ctx);
        }
        try
        {
            if (path == null || path.length() == 0)
            {
                return;
            }
            VariableMapper orig = ctx.getVariableMapper();
            ctx.setVariableMapper(new VariableMapperWrapper(orig));
            try
            {
                //Only ui:param could be inside ui:include.
                //this.nextHandler.apply(ctx, null);
                
                URL url = null;
                boolean oldMarkInitialState = false;
                Boolean isBuildingInitialState = null;
                // if we are in ProjectStage Development and the path equals "javax.faces.error.xhtml"
                // we should include the default error page
                if (ctx.getFacesContext().isProjectStage(ProjectStage.Development) 
                        && ERROR_PAGE_INCLUDE_PATH.equals(path))
                {
                    url =ClassUtils.getResource(ERROR_FACELET);
                }
                if (markInitialState)
                {
                    //set markInitialState flag
                    oldMarkInitialState = fcc.isMarkInitialState();
                    fcc.setMarkInitialState(true);
                    isBuildingInitialState = (Boolean) ctx.getFacesContext().getAttributes().put(
                            StateManager.IS_BUILDING_INITIAL_STATE, Boolean.TRUE);
                }
                try
                {
                    if (_params != null)
                    {
                        // ui:include defines a new TemplateContext, but ui:param EL expressions
                        // defined inside should be built before the new context is setup, to
                        // apply then after. The final effect is EL expressions will be resolved
                        // correctly when nested ui:params with the same name or based on other
                        // ui:params are used.
                        
                        String[] names = new String[_params.length];
                        ValueExpression[] values = new ValueExpression[_params.length];
                        
                        for (int i = 0; i < _params.length; i++)
                        {
                            names[i] = _params[i].getName(ctx);
                            values[i] = _params[i].getValue(ctx);
                        }
                        
                        actx.pushTemplateContext(new TemplateContextImpl());
                        
                        for (int i = 0; i < _params.length; i++)
                        {
                            _params[i].apply(ctx, parent, names[i], values[i], uniqueId);
                        }
                    }
                    else
                    {
                        actx.pushTemplateContext(new TemplateContextImpl());
                    }
                    if (url == null)
                    {
                        ctx.includeFacelet(parent, path);
                    }
                    else
                    {
                        ctx.includeFacelet(parent, url);
                    }
                }
                finally
                {
                    if (markInitialState)
                    {
                        //unset markInitialState flag
                        if (isBuildingInitialState == null)
                        {
                            ctx.getFacesContext().getAttributes().remove(
                                    StateManager.IS_BUILDING_INITIAL_STATE);
                        }
                        else
                        {
                            ctx.getFacesContext().getAttributes().put(
                                    StateManager.IS_BUILDING_INITIAL_STATE, isBuildingInitialState);
                        }
                        fcc.setMarkInitialState(oldMarkInitialState);
                    }
                    actx.popTemplateContext();
                }
            }
            finally
            {
                ctx.setVariableMapper(orig);
            }
        }
        finally
        {
            if (!src.isLiteral())
            {
                fcc.endComponentUniqueIdSection();
            }
        }
        if (!src.isLiteral() && fcc.isUsingPSSOnThisView() && fcc.isRefreshTransientBuildOnPSS() &&
            !fcc.isRefreshingTransientBuild())
        {
            //Mark the parent component to be saved and restored fully.
            ComponentSupport.markComponentToRestoreFully(ctx.getFacesContext(), parent);
        }
        if (!src.isLiteral() && fcc.isDynamicComponentSection())
        {
            ComponentSupport.markComponentToRefreshDynamically(ctx.getFacesContext(), parent);
        }
    }
}
