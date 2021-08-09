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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.view.ActionSource2AttachedObjectHandler;
import javax.faces.view.AttachedObjectHandler;
import javax.faces.view.Location;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;

/**
 *
 * @author Leonardo Uribe
 */
@JSFFaceletTag(
            name = "f:resetValues",
            bodyContent = "empty")
public class ResetValuesActionListenerHandler extends TagHandler
    implements ActionSource2AttachedObjectHandler
{
    /**
     * 
     */
    @JSFFaceletAttribute(name = "render", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.Object")
    private final TagAttribute _render;
    
    private final Collection<String> _clientIds;

    public ResetValuesActionListenerHandler(TagConfig config)
    {
        super(config);
        _render = getRequiredAttribute("render");
        if (_render.isLiteral())
        {
            String value = _render.getValue();
            if (value == null)
            {
                _clientIds = Collections.emptyList();
            }
            else
            {
                String[] arrValue = value.split(" ");
                _clientIds = Arrays.asList(arrValue);
            }
        }
        else
        {
            _clientIds = null;
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
        //Apply only if we are creating a new component
        if (!ComponentHandler.isNew(parent))
        {
            return;
        }
        if (parent instanceof ActionSource)
        {
            applyAttachedObject(ctx.getFacesContext(), parent, false);
        }
        else if (UIComponent.isCompositeComponent(parent))
        {
            if (getAttribute(JSFAttr.FOR_ATTR) == null)
            {
                throw new TagException(tag, "is nested inside a composite component"
                        + " but does not have a for attribute.");
            }
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
            mctx.addAttachedObjectHandler(parent, this);
        }
        else
        {
            throw new TagException(this.tag,
                    "Parent is not composite component or of type ActionSource, type is: " + parent);
        }
    }

    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        applyAttachedObject(context, parent, true);
    }
    
    public void applyAttachedObject(FacesContext context, UIComponent parent, boolean checkForParentCC)
    {
        UIComponent topParentComponent = null;
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);
        
        if (checkForParentCC)
        {
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(faceletContext);
            UIComponent parentComponent = parent;
            while (parentComponent != null)
            {
                if (UIComponent.isCompositeComponent(parentComponent))
                {
                    List<AttachedObjectHandler> handlerList = mctx.getAttachedObjectHandlers(parentComponent);
                    if (handlerList != null && handlerList.contains(this))
                    {
                        //Found, but we need to go right to the top for it. 
                        topParentComponent = parentComponent;
                    }
                }
                parentComponent = parentComponent.getParent();
            }
        }

        ActionSource as = (ActionSource) parent;
        ActionListener listener = null;
        if (_render.isLiteral())
        {
            listener = new LiteralResetValuesActionListener(_clientIds,
                topParentComponent != null ? 
                (Location) topParentComponent.getAttributes().get(
                    CompositeComponentELUtils.LOCATION_KEY) : null);
        }
        else
        {
            listener = new ResetValuesActionListener(_render
                .getValueExpression(faceletContext, Object.class) ,
                topParentComponent != null ? 
                (Location) topParentComponent.getAttributes().get(
                    CompositeComponentELUtils.LOCATION_KEY) : null);
        }
        as.addActionListener(listener);
    }

    /**
     * TODO: Document me!
     */
    @JSFFaceletAttribute
    public String getFor()
    {
        TagAttribute forAttribute = getAttribute("for");
        
        if (forAttribute == null)
        {
            return null;
        }
        else
        {
            return forAttribute.getValue();
        }
    }
    
    private final static class ResetValuesActionListener implements ActionListener, Serializable
    {
        private static final long serialVersionUID = -9200000013153262119L;

        private ValueExpression renderExpression;
        
        private Location topCompositeComponentReference;
        
        private ResetValuesActionListener()
        {
        }
        
        public ResetValuesActionListener(ValueExpression renderExpression, Location location)
        {
            this.renderExpression = renderExpression;
            this.topCompositeComponentReference = location;
        }

        public void processAction(ActionEvent event) throws AbortProcessingException
        {
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null)
            {
                return;
            }
            UIViewRoot root = faces.getViewRoot();
            if (root == null)
            {
                return;
            }
            Object value = renderExpression.getValue(faces.getELContext());
            Collection<String> clientIds = null;
            if (value == null)
            {
                value = Collections.emptyList();
            }
            if (value instanceof Collection)
            {
                clientIds = (Collection) value;
            }
            else if (value instanceof String)
            {
                String[] arrValue = ((String)value).split(" ");
                clientIds = Arrays.asList(arrValue);
            }
            else
            {
                throw new IllegalArgumentException("Type " + value.getClass()
                        + " not supported for attribute render");
            }
            
            // Calculate the final clientIds
            UIComponent contextComponent = null;
            if (topCompositeComponentReference != null)
            {
                contextComponent = CompositeComponentELUtils.getCompositeComponentBasedOnLocation(
                    faces, event.getComponent(), topCompositeComponentReference);
                if (contextComponent == null)
                {
                    contextComponent = event.getComponent();
                }
                else
                {
                    contextComponent = contextComponent.getParent();
                }
            }
            else
            {
                contextComponent = event.getComponent();
            }
            List<String> list = new ArrayList<String>();
            for (String id : clientIds)
            {
                list.add(getComponentId(faces, contextComponent, id));
            }
            
            // Call resetValues
            root.resetValues(faces, list);
        }
    }
    
    private final static class LiteralResetValuesActionListener implements ActionListener, Serializable
    {
        private static final long serialVersionUID = -9200000013153262119L;

        private Collection<String> clientIds;
        
        private Location topCompositeComponentReference;
        
        private LiteralResetValuesActionListener()
        {
        }
        
        public LiteralResetValuesActionListener(Collection<String> clientIds, Location location)
        {
            this.clientIds = clientIds;
            this.topCompositeComponentReference = location;
        }

        public void processAction(ActionEvent event) throws AbortProcessingException
        {
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null)
            {
                return;
            }
            UIViewRoot root = faces.getViewRoot();
            if (root == null)
            {
                return;
            }
            
            // Calculate the final clientIds
            UIComponent contextComponent = null;
            if (topCompositeComponentReference != null)
            {
                contextComponent = CompositeComponentELUtils.getCompositeComponentBasedOnLocation(
                    faces, event.getComponent(), topCompositeComponentReference);
                if (contextComponent == null)
                {
                    contextComponent = event.getComponent();
                }
                else
                {
                    contextComponent = contextComponent.getParent();
                }
            }
            else
            {
                contextComponent = event.getComponent();
            }
            List<String> list = new ArrayList<String>();
            for (String id : clientIds)
            {
                list.add(getComponentId(faces, contextComponent, id));
            }
            
            root.resetValues(faces, list);
        }
    }
    
    private static final String getComponentId(FacesContext facesContext, 
        UIComponent contextComponent, String id)
    {
        UIComponent target = contextComponent.findComponent(id);
        if (target == null)
        {
            target = contextComponent.findComponent(
                facesContext.getNamingContainerSeparatorChar() + id);
        }
        if (target != null)
        {
            return target.getClientId(facesContext);
        }
        return id;
    }
}
