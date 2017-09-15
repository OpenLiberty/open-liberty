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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.StateManager;
import javax.faces.component.UIComponent;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.ComponentContainerHandler;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * Simple conditional tag, which evalutes its body if the
 * supplied condition is true and optionally exposes a Boolean
 * scripting variable representing the evaluation of this condition
 * 
 * @author Jacob Hookom
 * @version $Id: IfHandler.java 1641470 2014-11-24 20:35:02Z lu4242 $
 */
@JSFFaceletTag(name="c:if")
@JSFFaceletAttribute(
        name="scope",
        className="java.lang.String",
        longDescription="Scope for var.")
public final class IfHandler extends TagHandler implements ComponentContainerHandler
{

    /**
     * The test condition that determines whether or
     * not the body content should be processed.
     */
    @JSFFaceletAttribute(className="boolean", required=true)
    private final TagAttribute test;

    /**
     * Name of the exported scoped variable for the
     * resulting value of the test condition. The type
     * of the scoped variable is Boolean.  
     */
    @JSFFaceletAttribute(className="java.lang.String")
    private final TagAttribute var;

    /**
     * @param config
     */
    public IfHandler(TagConfig config)
    {
        super(config);
        this.test = this.getRequiredAttribute("test");
        this.var = this.getAttribute("var");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException
    {
        FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        String uniqueId = actx.generateUniqueFaceletTagId(
            fcc.startComponentUniqueIdSection(), tagId);
        Boolean restoredValue = (Boolean) ComponentSupport.restoreInitialTagState(ctx, fcc, parent, uniqueId);
        boolean b = false;
        boolean markInitialState = false;
        try
        {
            if (restoredValue != null)
            {
                if (!PhaseId.RESTORE_VIEW.equals(ctx.getFacesContext().getCurrentPhaseId()))
                {
                    b = this.test.getBoolean(ctx);
                    if (!restoredValue.equals(b))
                    {
                        markInitialState = true;
                    }
                }
                else
                {
                    b = restoredValue;
                }
            }
            else
            {
                // No state restored, calculate
                b = this.test.getBoolean(ctx);
            }
            //boolean b = getTestValue(ctx, fcc, parent, uniqueId);
            if (this.var != null)
            {
                ctx.setAttribute(var.getValue(ctx), Boolean.valueOf(b));
            }
            if (b)
            {
                boolean oldMarkInitialState = false;
                Boolean isBuildingInitialState = null;
                try
                {
                    if (markInitialState)
                    {
                        //set markInitialState flag
                        oldMarkInitialState = fcc.isMarkInitialState();
                        fcc.setMarkInitialState(true);
                        isBuildingInitialState = (Boolean) ctx.getFacesContext().getAttributes().put(
                                StateManager.IS_BUILDING_INITIAL_STATE, Boolean.TRUE);
                    }
                    this.nextHandler.apply(ctx, parent);
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
        }
        finally
        {
            fcc.endComponentUniqueIdSection();
        }
        //AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        ComponentSupport.saveInitialTagState(ctx, fcc, parent, uniqueId, b);
        if (fcc.isUsingPSSOnThisView() && fcc.isRefreshTransientBuildOnPSS() && !fcc.isRefreshingTransientBuild())
        {
            //Mark the parent component to be saved and restored fully.
            ComponentSupport.markComponentToRestoreFully(ctx.getFacesContext(), parent);
        }
        if (fcc.isDynamicComponentSection())
        {
            ComponentSupport.markComponentToRefreshDynamically(ctx.getFacesContext(), parent);
        }
    }
}
