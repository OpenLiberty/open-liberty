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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.component.UIComponent;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.el.VariableMapperWrapper;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * The decorate tag acts the same as a composition tag, but it will not trim 
 * everything outside of it. This is useful in cases where you have a list of 
 * items in a document, which you would like to be decorated or framed.
 *  
 * The sum of it all is that you can take any element in the document and decorate 
 * it with some external logic as provided by the template.
 * 
 * NOTE: This implementation is provided for compatibility reasons and
 * it is considered faulty. It is enabled using
 * org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY web config param.
 * Don't use it if EL expression caching is enabled.
 * 
 * @author Jacob Hookom
 * @version $Id: DecorateHandler.java,v 1.16 2008/07/13 19:01:41 rlubke Exp $
 */
//@JSFFaceletTag(name="ui:decorate")
public final class LegacyDecorateHandler extends TagHandler implements TemplateClient
{

    //private static final Logger log = Logger.getLogger("facelets.tag.ui.decorate");
    private static final Logger log = Logger.getLogger(DecorateHandler.class.getName());

    /**
     * The resolvable URI of the template to use. The content within the decorate tag 
     * will be used in populating the template specified.
     */
    //@JSFFaceletAttribute(
    //        name="template",
    //        className="javax.el.ValueExpression",
    //        deferredValueType="java.lang.String")
    private final TagAttribute _template;

    private final Map<String, DefineHandler> _handlers;

    private final LegacyParamHandler[] _params;

    /**
     * @param config
     */
    public LegacyDecorateHandler(TagConfig config)
    {
        super(config);
        _template = getRequiredAttribute("template");
        _handlers = new HashMap<String, DefineHandler>();

        for (DefineHandler handler : TagHandlerUtils.findNextByType(nextHandler, DefineHandler.class))
        {
            _handlers.put(handler.getName(), handler);
            if (log.isLoggable(Level.FINE))
            {
                log.fine(tag + " found Define[" + handler.getName() + "]");
            }
        }

        Collection<LegacyParamHandler> params = TagHandlerUtils.findNextByType(nextHandler, 
                LegacyParamHandler.class);
        if (!params.isEmpty())
        {
            int i = 0;
            _params = new LegacyParamHandler[params.size()];
            for (LegacyParamHandler handler : params)
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
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext,
     * javax.faces.component.UIComponent)
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        VariableMapper orig = ctx.getVariableMapper();
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        actx.pushClient(this);

        if (_params != null)
        {
            VariableMapper vm = new VariableMapperWrapper(orig);
            ctx.setVariableMapper(vm);
            for (int i = 0; i < _params.length; i++)
            {
                _params[i].apply(ctx, parent);
            }
        }

        FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
        String path;
        boolean markInitialState = false;
        if (!_template.isLiteral())
        {
            String uniqueId = actx.generateUniqueFaceletTagId(fcc.startComponentUniqueIdSection(), tagId);
            //path = getTemplateValue(actx, fcc, parent, uniqueId);
            String restoredPath = (String) ComponentSupport.restoreInitialTagState(ctx, fcc, parent, uniqueId);
            if (restoredPath != null)
            {
                // If is not restore view phase, the path value should be
                // evaluated and if is not equals, trigger markInitialState stuff.
                if (!PhaseId.RESTORE_VIEW.equals(ctx.getFacesContext().getCurrentPhaseId()))
                {
                    path = this._template.getValue(ctx);
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
                path = this._template.getValue(ctx);
            }
            ComponentSupport.saveInitialTagState(ctx, fcc, parent, uniqueId, path);
        }
        else
        {
            path = _template.getValue(ctx);
        }
        try
        {
            try
            {
                boolean oldMarkInitialState = false;
                Boolean isBuildingInitialState = null;
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
                    ctx.includeFacelet(parent, path);
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
                }
            }
            finally
            {
                ctx.setVariableMapper(orig);
                actx.popClient(this);
            }
        }
        finally
        {
            if (!_template.isLiteral())
            {
                fcc.endComponentUniqueIdSection();
            }
        }
        if (!_template.isLiteral() && fcc.isUsingPSSOnThisView() && fcc.isRefreshTransientBuildOnPSS() &&
            !fcc.isRefreshingTransientBuild())
        {
            //Mark the parent component to be saved and restored fully.
            ComponentSupport.markComponentToRestoreFully(ctx.getFacesContext(), parent);
        }
        if (!_template.isLiteral() && fcc.isDynamicComponentSection())
        {
            ComponentSupport.markComponentToRefreshDynamically(ctx.getFacesContext(), parent);
        }
    }

    public boolean apply(FaceletContext ctx, UIComponent parent, String name) throws IOException, FacesException,
            FaceletException, ELException
    {
        if (name != null)
        {
            DefineHandler handler = _handlers.get(name);
            if (handler != null)
            {
                handler.applyDefinition(ctx, parent);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            this.nextHandler.apply(ctx, parent);
            return true;
        }
    }
}
