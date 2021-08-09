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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectHandler;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TextHandler;

import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.TemplateContext;
import org.apache.myfaces.view.facelets.el.VariableMapperWrapper;
import org.apache.myfaces.view.facelets.tag.ComponentContainerHandler;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;
import org.apache.myfaces.view.facelets.tag.jsf.ActionSourceRule;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentBuilderHandler;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.EditableValueHolderRule;
import org.apache.myfaces.view.facelets.tag.jsf.ValueHolderRule;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;

/**
 * This handler is responsible for apply composite components. It
 * is created by CompositeResourceLibrary class when a composite component
 * is found.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
public class CompositeComponentResourceTagHandler extends ComponentHandler
    implements ComponentBuilderHandler, TemplateClient
{
    public static final String CREATE_CC_ON_POST_ADD_TO_VIEW = "oamf.cc.CREATE_CC_POST_ADD_TO_VIEW";
    
    private final Resource _resource;
    
    private Metadata _mapper;
    
    private Class<?> _lastType = Object.class;
    
    protected volatile Map<String, FaceletHandler> _facetHandlersMap;
    
    protected final Collection<FaceletHandler> _componentHandlers;
    
    protected final Collection<FaceletHandler> _facetHandlers;
    
    private boolean _dynamicCompositeComponent;
    
    public CompositeComponentResourceTagHandler(ComponentConfig config, Resource resource)
    {
        super(config);
        _resource = resource;
        _facetHandlers = TagHandlerUtils.findNextByType(nextHandler, javax.faces.view.facelets.FacetHandler.class,
                                                        InsertFacetHandler.class);
        _componentHandlers = TagHandlerUtils.findNextByType(nextHandler,
                javax.faces.view.facelets.ComponentHandler.class,
                ComponentContainerHandler.class, TextHandler.class);
        _dynamicCompositeComponent = false;
    }

    public UIComponent createComponent(FaceletContext ctx)
    {
        FacesContext facesContext = ctx.getFacesContext();
        UIComponent component = facesContext.getApplication().createComponent(facesContext, _resource);
        
        // Check required attributes if the app is not on production stage. 
        // Unfortunately, we can't check it on constructor because we need to call
        // ViewDeclarationLanguage.getComponentMetadata() and on that point it is possible to not
        // have a viewId.
        if (!facesContext.isProjectStage(ProjectStage.Production))
        {
            BeanInfo beanInfo = (BeanInfo) component.getAttributes().get(UIComponent.BEANINFO_KEY);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors())
            {
                ValueExpression ve = (ValueExpression) propertyDescriptor.getValue("required");
                if (ve != null)
                {
                    Object value = ve.getValue (facesContext.getELContext());
                    Boolean required = null;
                    if (value instanceof Boolean)
                    {
                        required = (Boolean) value;
                    }
                    else
                    {
                        required = Boolean.valueOf(value.toString());
                    } 
                    
                    if (required != null && required.booleanValue())
                    {
                        Object attrValue = this.tag.getAttributes().get (propertyDescriptor.getName());
                        
                        if (attrValue == null)
                        {
                            throw new TagException(this.tag, "Attribute '" + propertyDescriptor.getName()
                                                             + "' is required");
                        }
                    }
                }
            }
        }
        return component;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c)
            throws IOException
    {
        //super.applyNextHandler(ctx, c);
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
        
        // Since JSF 2.2, there are two cases here:
        // 
        // 1. The composite component content is defined as facelet content like usual.
        // 2. The composite component content will be defined programatically. That means,
        // once the component instance is created, the user will be responsible to add
        // children / facets and the code that process the composite component take effect
        // when the composite component is added to the view. 
        if (mctx.isDynamicCompositeComponentHandler())
        {
            _dynamicCompositeComponent = true;
            try
            {
                mctx.setDynamicCompositeComponentHandler(false);
                // If the composite component needs to be created dynamically
                // 
                Integer step = (Integer) c.getAttributes().get(CREATE_CC_ON_POST_ADD_TO_VIEW); 
                if (step == null)
                {
                    // The flag is not found, so we are creating the component right now.
                    // Add the flag and return.
                    c.getAttributes().put(CREATE_CC_ON_POST_ADD_TO_VIEW, 0);
                }
                else if (step.intValue() == 0)
                {
                    // Should not happen, stop processing
                }
                else if (step.intValue() == 1)
                {
                    // The component was created, and the listener attached to PostAddToViewEvent
                    // is executing right now. Do the necessary steps to process the 
                    // composite component dynamically.
                    applyNextHandlerIfNotAppliedDynamically(ctx, c);

                    applyCompositeComponentFacelet(ctx,c);

                    applyFinalInitializationSteps(ctx, mctx, c);

                    c.getAttributes().put(CREATE_CC_ON_POST_ADD_TO_VIEW, 2);
                }
                else
                {
                    // Refresh over dynamic composite component        
                    applyCompositeComponentFacelet(ctx,c);
                }
            }
            finally
            {
                mctx.setDynamicCompositeComponentHandler(true);
            }
        }
        else
        {
            applyNextHandlerIfNotApplied(ctx, c);

            applyCompositeComponentFacelet(ctx,c);

            if (ComponentHandler.isNew(c))
            {
                applyFinalInitializationSteps(ctx, mctx, c);
            }
        }
    }
    
    protected void applyFinalInitializationSteps(FaceletContext ctx, FaceletCompositionContext mctx, UIComponent c)
    {
        FacesContext facesContext = ctx.getFacesContext();

        ViewDeclarationLanguage vdl = facesContext.getApplication().getViewHandler().
            getViewDeclarationLanguage(facesContext, facesContext.getViewRoot().getViewId());

        List<AttachedObjectHandler> handlers = mctx.getAttachedObjectHandlers(c);

        if (handlers != null)
        {
            vdl.retargetAttachedObjects(facesContext, c, handlers);

            // remove the list of handlers, as it is no longer necessary
            mctx.removeAttachedObjectHandlers(c);
        }

        vdl.retargetMethodExpressions(facesContext, c);

        if ( FaceletCompositionContext.getCurrentInstance(ctx).isMarkInitialState())
        {
            // Call it only if we are using partial state saving
            c.markInitialState();
            // Call it to other components created not bound by a tag handler
            c.getFacet(UIComponent.COMPOSITE_FACET_NAME).markInitialState();
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void applyNextHandlerIfNotApplied(FaceletContext ctx, UIComponent c)
        throws IOException
    {
        //Apply all facelets not applied yet.
        
        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) c.getAttributes().get(UIComponent.BEANINFO_KEY);
        
        BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();

        boolean insertChildrenUsed = (beanDescriptor.getValue(InsertChildrenHandler.INSERT_CHILDREN_USED) != null);
        
        List<String> insertFacetList = (List<String>) beanDescriptor.getValue(InsertFacetHandler.INSERT_FACET_USED);
        
        if (nextHandler instanceof javax.faces.view.facelets.CompositeFaceletHandler)
        {
            for (FaceletHandler handler :
                    ((javax.faces.view.facelets.CompositeFaceletHandler)nextHandler).getHandlers())
            {
                if (handler instanceof javax.faces.view.facelets.FacetHandler)
                {
                    if (insertFacetList == null ||
                        !insertFacetList.contains(((javax.faces.view.facelets.FacetHandler)handler).getFacetName(ctx)))
                    {
                        handler.apply(ctx, c);
                    }
                }
                else if (handler instanceof InsertFacetHandler)
                {
                    if (insertFacetList == null ||
                        !insertFacetList.contains( ((InsertFacetHandler)handler).getFacetName(ctx)))
                    {
                        handler.apply(ctx, c);
                    }
                }
                else if (insertChildrenUsed)
                {
                    if (!(handler instanceof javax.faces.view.facelets.ComponentHandler ||
                            handler instanceof ComponentContainerHandler ||
                            handler instanceof TextHandler))
                    {
                        handler.apply(ctx, c);
                    }
                }
                else
                {
                    handler.apply(ctx, c);
                }
            }
        }
        else
        {
            if (nextHandler instanceof javax.faces.view.facelets.FacetHandler)
            {
                if (insertFacetList == null ||
                    !insertFacetList.contains(((javax.faces.view.facelets.FacetHandler)nextHandler).getFacetName(ctx)))
                {
                    nextHandler.apply(ctx, c);
                }
            }
            else if (nextHandler instanceof InsertFacetHandler)
            {
                if (insertFacetList == null ||
                    !insertFacetList.contains( ((InsertFacetHandler)nextHandler).getFacetName(ctx)) )
                {
                    nextHandler.apply(ctx, c);
                }
            }
            else if (insertChildrenUsed)
            {
                if (!(nextHandler instanceof javax.faces.view.facelets.ComponentHandler ||
                      nextHandler instanceof ComponentContainerHandler ||
                      nextHandler instanceof TextHandler))
                {
                    nextHandler.apply(ctx, c);
                }
            }
            else
            {
                nextHandler.apply(ctx, c);
            }
        }
        
        //Check for required facets
        Map<String, PropertyDescriptor> facetPropertyDescriptorMap = (Map<String, PropertyDescriptor>)
            beanDescriptor.getValue(UIComponent.FACETS_KEY);
        
        if (facetPropertyDescriptorMap != null)
        {
            List<String> facetsRequiredNotFound = null;
            for (Map.Entry<String, PropertyDescriptor> entry : facetPropertyDescriptorMap.entrySet())
            {
                ValueExpression requiredExpr = (ValueExpression) entry.getValue().getValue("required");
                if (requiredExpr != null)
                {
                    Boolean required = (Boolean) requiredExpr.getValue(ctx.getFacesContext().getELContext());
                    if (Boolean.TRUE.equals(required))
                    {
                        initFacetHandlersMap(ctx);
                        if (!_facetHandlersMap.containsKey(entry.getKey()))
                        {
                            if (facetsRequiredNotFound == null)
                            {
                                facetsRequiredNotFound = new ArrayList(facetPropertyDescriptorMap.size());
                            }
                            facetsRequiredNotFound.add(entry.getKey());
                        }
                        
                    }
                }
            }
            if (facetsRequiredNotFound != null && !facetsRequiredNotFound.isEmpty())
            {
                throw new TagException(getTag(), "The following facets are required by the component: "
                                                 + facetsRequiredNotFound);
            }
        }
    }
    
    protected void applyCompositeComponentFacelet(FaceletContext faceletContext, UIComponent compositeComponentBase) 
        throws IOException
    {
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(faceletContext);
        AbstractFaceletContext actx = (AbstractFaceletContext) faceletContext;
        UIPanel compositeFacetPanel
                = (UIPanel) compositeComponentBase.getFacets().get(UIComponent.COMPOSITE_FACET_NAME);
        if (compositeFacetPanel == null)
        {
            compositeFacetPanel = (UIPanel)
                faceletContext.getFacesContext().getApplication().createComponent(
                    faceletContext.getFacesContext(), UIPanel.COMPONENT_TYPE, null);
            compositeFacetPanel.getAttributes().put(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER,
                    Boolean.TRUE);
            compositeComponentBase.getFacets().put(UIComponent.COMPOSITE_FACET_NAME, compositeFacetPanel);
            
            // Set an id to the created facet component, to prevent id generation and make
            // partial state saving work without problem.
            UniqueIdVendor uniqueIdVendor = mctx.getUniqueIdVendorFromStack();
            if (uniqueIdVendor == null)
            {
                uniqueIdVendor = ComponentSupport.getViewRoot(faceletContext, compositeComponentBase);
            }
            if (uniqueIdVendor != null)
            {
                // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
                // and call createUniqueId()
                String uid = uniqueIdVendor.createUniqueId(faceletContext.getFacesContext(),
                        mctx.getSharedStringBuilder()
                        .append(compositeComponentBase.getId())
                        .append("__f_")
                        .append("cc_facet").toString());
                compositeFacetPanel.setId(uid);
            }            
        }
        
        // Before call applyCompositeComponent we need to add ajax behaviors
        // to the current compositeComponentBase. Note that super.applyNextHandler()
        // has already been called, but this point is before vdl.retargetAttachedObjects,
        // so we can't but this on ComponentTagHandlerDelegate, if we want this to be
        // applied correctly.
        Iterator<AjaxHandler> it = ((AbstractFaceletContext) faceletContext).getAjaxHandlers();
        if (it != null)
        {
            while(it.hasNext())
            {
                mctx.addAttachedObjectHandler(
                        compositeComponentBase, it.next());
            }
        }    
        
        VariableMapper orig = faceletContext.getVariableMapper();
        try
        {
            faceletContext.setVariableMapper(new VariableMapperWrapper(orig));
            actx.pushCompositeComponentClient(this);
            Resource resourceForCurrentView = faceletContext.getFacesContext().getApplication().
                getResourceHandler().createResource(_resource.getResourceName(), _resource.getLibraryName());
            if (resourceForCurrentView != null)
            {
                //Wrap it for serialization.
                resourceForCurrentView = new CompositeResouceWrapper(resourceForCurrentView);
            }
            else
            {
                //If a resource cannot be resolved it means a default for the current 
                //composite component does not exists.
                throw new TagException(getTag(), "Composite Component " + getTag().getQName() 
                        + " requires a default instance that can be found by the installed ResourceHandler.");
            }
            actx.applyCompositeComponent(compositeFacetPanel, resourceForCurrentView);
        }
        finally
        {
            actx.popCompositeComponentClient();
            faceletContext.setVariableMapper(orig);
        }
    }

    @Override
    public void setAttributes(FaceletContext ctx, Object instance)
    {
        if (instance != null)
        {
            UIComponent component = (UIComponent) instance;

            Class<?> type = instance.getClass();
            if (_mapper == null || !_lastType.equals(type))
            {
                _lastType = type;
                BeanInfo beanInfo = (BeanInfo)component.getAttributes().get(UIComponent.BEANINFO_KEY);
                _mapper = createMetaRuleset(type , beanInfo).finish();
            }
            
            _mapper.applyMetadata(ctx, instance);
        }        
    }

    protected MetaRuleset createMetaRuleset(Class<?> type, BeanInfo beanInfo)
    {
        MetaRuleset m = new CompositeMetaRulesetImpl(this.getTag(), type, beanInfo);
        // ignore standard component attributes
        m.ignore("binding").ignore("id");

        // add auto wiring for attributes
        m.addRule(CompositeComponentRule.INSTANCE);
        
        // add retarget method expression rules
        m.addRule(RetargetMethodExpressionRule.INSTANCE);
        
        if (ActionSource.class.isAssignableFrom(type))
        {
            m.addRule(ActionSourceRule.INSTANCE);
        }

        if (ValueHolder.class.isAssignableFrom(type))
        {
            m.addRule(ValueHolderRule.INSTANCE);

            if (EditableValueHolder.class.isAssignableFrom(type))
            {
                m.ignore("submittedValue");
                m.ignore("valid");
                m.addRule(EditableValueHolderRule.INSTANCE);
            }
        }
        
        return m;
    }
    
    private void initFacetHandlersMap(FaceletContext ctx)
    {
        if (_facetHandlersMap == null)
        {
            Map<String, FaceletHandler> map = new HashMap<String, FaceletHandler>();
            
            for (FaceletHandler handler : _facetHandlers)
            {
                if (handler instanceof javax.faces.view.facelets.FacetHandler )
                {
                    map.put( ((javax.faces.view.facelets.FacetHandler)handler).getFacetName(ctx), handler);
                }
                else if (handler instanceof InsertFacetHandler)
                {
                    map.put( ((InsertFacetHandler)handler).getFacetName(ctx), handler);
                }
            }
            _facetHandlersMap = map;
        }
    }
    
    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, FaceletException, ELException
    {
        if (_dynamicCompositeComponent)
        {
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            FaceletCompositionContext fcc = actx.getFaceletCompositionContext(); 
            UIComponent innerCompositeComponent = fcc.getCompositeComponentFromStack();
            
            // In a programatical addition, the code that process the composite component only takes effect
            // when the composite component is added to the view.
            Integer step = (Integer) innerCompositeComponent.getAttributes().get(CREATE_CC_ON_POST_ADD_TO_VIEW);
            if (step != null && step.intValue() == 1)
            {
                if (name != null)
                {
                    //1. Initialize map used to retrieve facets
                    if (innerCompositeComponent.getFacetCount() == 0)
                    {
                        checkFacetRequired(ctx, name);
                        return true;
                    }
                    UIComponent facet = innerCompositeComponent.getFacet(name);
                    if (facet != null)
                    {
                        // Insert facet
                        innerCompositeComponent.getFacets().remove(name);
                        parent.getFacets().put(name, facet);
                        return true;
                    }
                    else
                    {
                        checkFacetRequired(ctx, name);
                        return true;
                    }
                }
                else
                {
                    if (innerCompositeComponent.getChildCount() > 0)
                    {
                        String facetName = (String) parent.getAttributes().get(
                                org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler.KEY);
                        // Insert children
                        List<UIComponent> children = new ArrayList<UIComponent>(
                            innerCompositeComponent.getChildCount());
                        while (innerCompositeComponent.getChildCount() > 0)
                        {
                            children.add(innerCompositeComponent.getChildren().remove(0));
                        }
                        while (children.size() > 0)
                        {
                            UIComponent child = children.remove(0);
                            child.getAttributes().put(InsertChildrenHandler.INSERT_CHILDREN_USED,
                                    Boolean.TRUE);
                            if (facetName != null)
                            {
                                ComponentSupport.addFacet(ctx, parent, child, facetName);
                            }
                            else
                            { 
                                parent.getChildren().add(child);
                            }
                        }
                    }
                    return true;
                }
            }
            else if (step != null && step.intValue() > 1)
            {
                // refresh case, in facet case it is not necessary to remove/add the facet, because there
                // is no relative order (it is always on the same spot).
                if (name == null)
                {
                    String facetName = (String) parent.getAttributes().get(
                            org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler.KEY);
                    // refresh case, remember the inserted children does not have any
                    // associated tag handler, so in this case we just need to remove and add them in the same order 
                    // we found them
                    List<UIComponent> children = null;
                    if (facetName == null)
                    {
                        children = new ArrayList<UIComponent>(parent.getChildCount());
                        int i = 0;
                        while (parent.getChildCount()-i > 0)
                        {
                            UIComponent child = parent.getChildren().get(i);
                            if (Boolean.TRUE.equals(child.getAttributes().get(
                                    InsertChildrenHandler.INSERT_CHILDREN_USED)))
                            {
                                children.add(parent.getChildren().remove(i));
                            }
                            else
                            {
                                i++;
                            }
                        }
                    }
                    else
                    {
                        children = new ArrayList<UIComponent>();
                        UIComponent child = parent.getFacet(facetName);
                        if (Boolean.TRUE.equals(child.getAttributes().get(
                                    InsertChildrenHandler.INSERT_CHILDREN_USED)))
                        {
                            parent.getFacets().remove(facetName);
                            children.add(child);
                        }
                        else
                        {
                            UIComponent parentToApply = child;
                            int i = 0;
                            while (parentToApply.getChildCount()-i > 0)
                            {
                                child = parentToApply.getChildren().get(i);
                                if (Boolean.TRUE.equals(child.getAttributes().get(
                                        InsertChildrenHandler.INSERT_CHILDREN_USED)))
                                {
                                    children.add(parentToApply.getChildren().remove(i));
                                }
                                else
                                {
                                    i++;
                                }
                            }
                        }
                    }
                    while (children.size() > 0)
                    {
                        UIComponent child = children.remove(0);
                        if (facetName != null)
                        {
                            ComponentSupport.addFacet(ctx, parent, child, facetName);
                        }
                        else
                        { 
                            parent.getChildren().add(child);
                        }
                    }
                }
            }
            return true;
        }
        if (name != null)
        {
            //1. Initialize map used to retrieve facets
            if (_facetHandlers == null || _facetHandlers.isEmpty())
            {
                checkFacetRequired(ctx, name);
                return true;
            }

            initFacetHandlersMap(ctx);

            FaceletHandler handler = _facetHandlersMap.get(name);

            if (handler != null)
            {
                AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
                // Pop the current composite component on stack, so #{cc} references
                // can be resolved correctly, because they are relative to the page 
                // that define it.
                FaceletCompositionContext fcc = actx.getFaceletCompositionContext(); 
                UIComponent innerCompositeComponent = fcc.getCompositeComponentFromStack(); 
                fcc.popCompositeComponentToStack();
                // Pop the template context, so ui:xx tags and nested composite component
                // cases could work correctly 
                TemplateContext itc = actx.popTemplateContext();
                try
                {
                    handler.apply(ctx, parent);
                }
                finally
                {
                    actx.pushTemplateContext(itc);
                    fcc.pushCompositeComponentToStack(innerCompositeComponent);
                }
                return true;
                
            }
            else
            {
                checkFacetRequired(ctx, name);
                return true;
            }
        }
        else
        {
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            // Pop the current composite component on stack, so #{cc} references
            // can be resolved correctly, because they are relative to the page 
            // that define it.
            FaceletCompositionContext fcc = actx.getFaceletCompositionContext(); 
            UIComponent innerCompositeComponent = fcc.getCompositeComponentFromStack(); 
            fcc.popCompositeComponentToStack();
            // Pop the template context, so ui:xx tags and nested composite component
            // cases could work correctly 
            TemplateContext itc = actx.popTemplateContext();
            try
            {
                for (FaceletHandler handler : _componentHandlers)
                {
                    handler.apply(ctx, parent);
                }
            }
            finally
            {
                actx.pushTemplateContext(itc);
                fcc.pushCompositeComponentToStack(innerCompositeComponent);
            }
            return true;
        }
    }
    
    private void checkFacetRequired(FaceletContext ctx, String name)
    {
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        FaceletCompositionContext fcc = actx.getFaceletCompositionContext(); 
        UIComponent innerCompositeComponent = fcc.getCompositeComponentFromStack();
        
        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) innerCompositeComponent.getAttributes()
            .get(UIComponent.BEANINFO_KEY);
        
        BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
        
        Map<String, PropertyDescriptor> insertFacetPropertyDescriptorMap = (Map<String, PropertyDescriptor>)
            beanDescriptor.getValue(InsertFacetHandler.INSERT_FACET_KEYS);

        if (insertFacetPropertyDescriptorMap != null && insertFacetPropertyDescriptorMap.containsKey(name))
        {
            ValueExpression requiredExpr
                    = (ValueExpression) insertFacetPropertyDescriptorMap.get(name).getValue("required");
            
            if (requiredExpr != null &&
                Boolean.TRUE.equals(requiredExpr.getValue(ctx.getFacesContext().getELContext())))
            {
                //Insert facet associated is required, but it was not applied.
                throw new TagException(this.tag, "Cannot find facet with name '"+name+"' in composite component");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void applyNextHandlerIfNotAppliedDynamically(FaceletContext ctx, UIComponent c)
        throws IOException
    {
        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) c.getAttributes().get(UIComponent.BEANINFO_KEY);
        
        BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();

        // Since the children / facet were added programatically, there is no handler or facelets to apply.
        
        //Check for required facets
        Map<String, PropertyDescriptor> facetPropertyDescriptorMap = (Map<String, PropertyDescriptor>)
            beanDescriptor.getValue(UIComponent.FACETS_KEY);
        
        if (facetPropertyDescriptorMap != null)
        {
            List<String> facetsRequiredNotFound = null;
            for (Map.Entry<String, PropertyDescriptor> entry : facetPropertyDescriptorMap.entrySet())
            {
                ValueExpression requiredExpr = (ValueExpression) entry.getValue().getValue("required");
                if (requiredExpr != null)
                {
                    Boolean required = (Boolean) requiredExpr.getValue(ctx.getFacesContext().getELContext());
                    if (Boolean.TRUE.equals(required))
                    {
                        if (c.getFacet(entry.getKey()) == null)
                        {
                            if (facetsRequiredNotFound == null)
                            {
                                facetsRequiredNotFound = new ArrayList(facetPropertyDescriptorMap.size());
                            }
                            facetsRequiredNotFound.add(entry.getKey());
                        }
                    }
                }
            }
            if (facetsRequiredNotFound != null && !facetsRequiredNotFound.isEmpty())
            {
                throw new TagException(getTag(), "The following facets are required by the component: "
                                                 + facetsRequiredNotFound);
            }
        }
    }
}
