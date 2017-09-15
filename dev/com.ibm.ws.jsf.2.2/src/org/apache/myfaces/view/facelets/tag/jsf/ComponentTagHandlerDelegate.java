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
// PI47600      wtlucy  A "class" attribute cannot be set in a custom tag
package org.apache.myfaces.view.facelets.tag.jsf;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.ValueHolder;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.Validator;
import javax.faces.view.EditableValueHolderAttachedObjectHandler;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandlerDelegate;
import javax.faces.view.facelets.ValidatorHandler;

import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.ComponentState;
import org.apache.myfaces.view.facelets.DefaultFaceletsStateManagementStrategy;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.FaceletDynamicComponentRefreshTransientBuildEvent;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguageBase;
import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;
import org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler;

/**
 *  
 * Implementation of the tag logic used in the JSF specification. 
 * 
 * @see org.apache.myfaces.view.facelets.tag.jsf.ComponentHandler
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1634160 $ $Date: 2014-10-24 23:27:39 +0000 (Fri, 24 Oct 2014) $
 *
 * @since 2.0
 */
public class ComponentTagHandlerDelegate extends TagHandlerDelegate
{
    private final static Logger log = Logger.getLogger(ComponentTagHandlerDelegate.class.getName());
    
    private static final Set<VisitHint> VISIT_HINTS_DYN_REFRESH = Collections.unmodifiableSet( 
            EnumSet.of(VisitHint.SKIP_ITERATION));

    private final ComponentHandler _delegate;

    private final String _componentType;

    private final TagAttribute _id;

    private final String _rendererType;
    
    private final ComponentBuilderHandler _componentBuilderHandlerDelegate;
    
    private final RelocatableResourceHandler _relocatableResourceHandler;

    @SuppressWarnings("unchecked")
    public ComponentTagHandlerDelegate(ComponentHandler delegate)
    {
        _delegate = delegate;
        ComponentConfig delegateComponentConfig = delegate.getComponentConfig();
        _componentType = delegateComponentConfig.getComponentType();
        _rendererType = delegateComponentConfig.getRendererType();
        _id = delegate.getTagAttribute("id");      
        
        ComponentHandler handler = _delegate;
        boolean found = false;
        while(handler != null && !found)
        {
            if (handler instanceof ComponentBuilderHandler)
            {
                found = true;
            }
            else if (handler instanceof FacesWrapper)
            {
                handler = ((FacesWrapper<? extends ComponentHandler>)handler).getWrapped();
            }
            else
            {
                handler = null;
            }
        }
        if (found)
        {
            _componentBuilderHandlerDelegate = (ComponentBuilderHandler) handler;
        }
        else
        {
            _componentBuilderHandlerDelegate = null;
        }
        
        //Check if this component is instance of RelocatableResourceHandler
        handler = _delegate;
        found = false;
        while(handler != null && !found)
        {
            if (handler instanceof RelocatableResourceHandler)
            {
                found = true;
            }
            else if (handler instanceof FacesWrapper)
            {
                handler = ((FacesWrapper<? extends ComponentHandler>)handler).getWrapped();
            }
            else
            {
                handler = null;
            }
        }
        if (found)
        {
            _relocatableResourceHandler = (RelocatableResourceHandler) handler;
        }
        else
        {
            // Check if the component is a relocatable component done overriding the tag handler
            if (_componentType != null && _rendererType != null &&
                (_rendererType.equals("javax.faces.resource.Script") ||
                 _rendererType.equals("javax.faces.resource.Stylesheet")) &&
                _componentType.equals(UIOutput.COMPONENT_TYPE))
            {
                _relocatableResourceHandler = ComponentRelocatableResourceHandler.INSTANCE;
            }   
            else
            {
                _relocatableResourceHandler = null;
            }
        }
    }

    /**
     * Method handles UIComponent tree creation in accordance with the JSF 1.2 spec.
     * <ol>
     * <li>First determines this UIComponent's id by calling {@link #getId(FaceletContext) getId(FaceletContext)}.</li>
     * <li>Search the parent for an existing UIComponent of the id we just grabbed</li>
     * <li>If found, {@link FaceletCompositionContext#markForDeletion(UIComponent) mark} its children for deletion.</li>
     * <li>If <i>not</i> found, call {@link #createComponent(FaceletContext) createComponent}.
     * <ol>
     * <li>Only here do we apply
     * {@link javax.faces.view.facelets.TagHandler#setAttributes(FaceletCompositionContext, Object) attributes}</li>
     * <li>Set the UIComponent's id</li>
     * <li>Set the RendererType of this instance</li>
     * </ol>
     * </li>
     * <li>Now apply the nextHandler, passing the UIComponent we've created/found.</li>
     * <li>Now add the UIComponent to the passed parent</li>
     * <li>Lastly, if the UIComponent already existed (found),
     * then {@link #finalizeForDeletion(FaceletCompositionContext, UIComponent) finalize}
     * for deletion.</li>
     * </ol>
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext,
     * javax.faces.component.UIComponent)
     * 
     * @throws TagException
     *             if the UIComponent parent is null
     */
    @SuppressWarnings("unchecked")
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        // make sure our parent is not null
        if (parent == null)
        {
            throw new TagException(_delegate.getTag(), "Parent UIComponent was null");
        }
        
        FacesContext facesContext = ctx.getFacesContext();

        // possible facet scoped
        String facetName = this.getFacetName(ctx, parent);

        // our id
        String id = ctx.generateUniqueId(_delegate.getTagId());

        // Cast to use UniqueIdVendor stuff
        FaceletCompositionContext mctx = (FaceletCompositionContext) FaceletCompositionContext.getCurrentInstance(ctx);
                
        // grab our component
        UIComponent c = null;
        //boolean componentFoundInserted = false;

        //Used to preserve the original parent. Note when the view is being refreshed, the real parent could be
        //another component.
        UIComponent oldParent = parent;
        
        if (mctx.isRefreshingSection())
        {
            if (_relocatableResourceHandler != null)
            {
                c = _relocatableResourceHandler.findChildByTagId(ctx, parent, id);
            }
            else
            {
                if (facetName != null)
                {
                    c = ComponentSupport.findChildInFacetByTagId(parent, id, facetName);
                }
                else
                {
                    c = ComponentSupport.findChildInChildrenByTagId(parent, id);
                }
            }
        }
        boolean componentFound = false;
        if (c != null)
        {
            componentFound = true;
            // Check if the binding needs dynamic refresh and if that so, invoke the refresh from this location, to
            // preserve the same context
            if (_delegate.getBinding() != null &&
                c.getAttributes().containsKey(
                    FaceletDynamicComponentRefreshTransientBuildEvent.DYNAMIC_COMPONENT_BINDING_NEEDS_REFRESH))
            {
                VisitContext visitContext = (VisitContext) mctx.getVisitContextFactory().
                    getVisitContext(facesContext, null, VISIT_HINTS_DYN_REFRESH);
                c.visitTree(visitContext, new PublishFaceletDynamicComponentRefreshTransientBuildCallback());
            }
            
            mctx.incrementUniqueComponentId();
            
            // mark all children for cleaning
            if (log.isLoggable(Level.FINE))
            {
                log.fine(_delegate.getTag() + " Component[" + id + "] Found, marking children for cleanup");
            }

            // The call for mctx.markForDeletion(c) is always necessary, because
            // component resource relocation occur as an effect of PostAddToViewEvent,
            // so at this point it is unknown if the component was relocated or not.
            mctx.markForDeletion(c);

            if (_relocatableResourceHandler != null)
            {
                mctx.markRelocatableResourceForDeletion(c);
            }
        }
        else
        {
            c = this.createComponent(ctx);
            if (log.isLoggable(Level.FINE))
            {
                log.fine(_delegate.getTag() + " Component[" + id + "] Created: " + c.getClass().getName());
            }
            
            _delegate.setAttributes(ctx, c);

            // mark it owned by a facelet instance
            c.getAttributes().put(ComponentSupport.MARK_CREATED, id);

            if (facesContext.isProjectStage(ProjectStage.Development))
            {
                c.getAttributes().put(UIComponent.VIEW_LOCATION_KEY,
                        _delegate.getTag().getLocation());
            }

            // assign our unique id
            if (this._id != null)
            {
                mctx.incrementUniqueComponentId();
                c.setId(this._id.getValue(ctx));
            }
            else
            {
                String componentId = mctx.generateUniqueComponentId();
                UniqueIdVendor uniqueIdVendor = mctx.getUniqueIdVendorFromStack();
                if (uniqueIdVendor == null)
                {
                    uniqueIdVendor = facesContext.getViewRoot();
                    
                    if (uniqueIdVendor == null)
                    {
                        // facesContext.getViewRoot() returns null here if we are in
                        // phase restore view, so we have to try to get the view root
                        // via the method in ComponentSupport and our parent
                        uniqueIdVendor = ComponentSupport.getViewRoot(ctx, parent);
                    }
                }
                if (uniqueIdVendor != null)
                {
                    // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
                    // and call createUniqueId()
                    String uid = uniqueIdVendor.createUniqueId(facesContext, componentId);
                    c.setId(uid);
                }
            }

            if (this._rendererType != null)
            {
                c.setRendererType(this._rendererType);
            }

            // hook method
            _delegate.onComponentCreated(ctx, c, parent);
            
            if (_relocatableResourceHandler != null && 
                _relocatableResourceHandler instanceof ComponentRelocatableResourceHandler)
            {
                UIComponent parentCompositeComponent
                        = mctx.getCompositeComponentFromStack();
                if (parentCompositeComponent != null)
                {
                    c.getAttributes().put(CompositeComponentELUtils.LOCATION_KEY,
                            parentCompositeComponent.getAttributes().get(CompositeComponentELUtils.LOCATION_KEY));
                }
            }
            
            if (mctx.isRefreshingTransientBuild() && _relocatableResourceHandler != null)
            {
                mctx.markRelocatableResourceForDeletion(c);
            }
        }
        c.pushComponentToEL(facesContext, c);

        if (c instanceof UniqueIdVendor)
        {
            mctx.pushUniqueIdVendorToStack((UniqueIdVendor)c);
        }
        
        if (mctx.isDynamicComponentTopLevel())
        {
            mctx.setDynamicComponentTopLevel(false);
            _delegate.applyNextHandler(ctx, c);
            mctx.setDynamicComponentTopLevel(true);
        }
        else
        {
            // first allow c to get populated
            _delegate.applyNextHandler(ctx, c);
        }
        
        boolean oldProcessingEvents = facesContext.isProcessingEvents();
        // finish cleaning up orphaned children
        if (componentFound && !mctx.isDynamicComponentTopLevel())
        {
            mctx.finalizeForDeletion(c);

            //if (!componentFoundInserted)
            //{
                if (mctx.isRefreshingSection())
                {
                    facesContext.setProcessingEvents(false);
                    if (_relocatableResourceHandler != null &&
                        parent != null && !parent.equals(c.getParent()))
                    {
                        // Replace parent with the relocated parent.
                        parent = c.getParent();
                        // Since we changed the parent, the facetName becomes invalid, because it points
                        // to the component before relocation. We need to find the right facetName (if any) so we can
                        // refresh the component properly.
                        UIComponent c1 = ComponentSupport.findChildInChildrenByTagId(parent, id);
                        if (c1 == null)
                        {
                            facetName = ComponentSupport.findChildInFacetsByTagId(parent, id);
                        }
                        else
                        {
                            facetName = null;
                        }
                    }
                    ComponentSupport.setCachedFacesContext(c, facesContext);
                }
                if (facetName == null)
                {
                    parent.getChildren().remove(c);
                }
                else
                {
                    ComponentSupport.removeFacet(ctx, parent, c, facetName);
                }
                if (mctx.isRefreshingSection())
                {
                    ComponentSupport.setCachedFacesContext(c, null);
                    facesContext.setProcessingEvents(oldProcessingEvents);
                }
            //}
        }


        if (!componentFound)
        {
            if (c instanceof ClientBehaviorHolder && !UIComponent.isCompositeComponent(c))
            {
                Iterator<AjaxHandler> it = ((AbstractFaceletContext) ctx).getAjaxHandlers();
                if (it != null)
                {
                    while(it.hasNext())
                    {
                        it.next().applyAttachedObject(facesContext, c);
                    }
                }
            }
            
            if (c instanceof EditableValueHolder)
            {
                // add default validators here, because this feature 
                // is only available in facelets (see MYFACES-2362 for details)
                addEnclosingAndDefaultValidators(ctx, mctx, facesContext, (EditableValueHolder) c);
            }
        }
        
        _delegate.onComponentPopulated(ctx, c, oldParent);

        if (!mctx.isDynamicComponentTopLevel() || !componentFound)
        {
            if (componentFound && mctx.isRefreshingSection())
            {
                facesContext.setProcessingEvents(false);
                ComponentSupport.setCachedFacesContext(c, facesContext);
            }
            if (facetName == null)
            {
                parent.getChildren().add(c);
            }
            else
            {
                ComponentSupport.addFacet(ctx, parent, c, facetName);
            }
            if (componentFound && mctx.isRefreshingSection())
            {
                ComponentSupport.setCachedFacesContext(c, null);
                facesContext.setProcessingEvents(oldProcessingEvents);
            }
        }

        if (c instanceof UniqueIdVendor)
        {
            mctx.popUniqueIdVendorToStack();
        }

        c.popComponentFromEL(facesContext);
        
        if (mctx.isMarkInitialState())
        {
            //Call it only if we are using partial state saving
            c.markInitialState();
        }
    }
    
    /**
     * Return the Facet name we are scoped in, otherwise null
     * 
     * @param ctx
     * @return
     */
    protected final String getFacetName(FaceletContext ctx, UIComponent parent)
    {
        return (String) parent.getAttributes().get(FacetHandler.KEY);
    }

    /**
     * If the binding attribute was specified, use that in conjuction with our componentType String variable to call
     * createComponent on the Application, otherwise just pass the componentType String. <p /> If the binding was used,
     * then set the ValueExpression "binding" on the created UIComponent.
     * 
     * @see Application#createComponent(javax.faces.el.ValueBinding, javax.faces.context.FacesContext, java.lang.String)
     * @see Application#createComponent(java.lang.String)
     * @param ctx
     *            FaceletContext to use in creating a component
     * @return
     */
    protected UIComponent createComponent(FaceletContext ctx)
    {
        if (_componentBuilderHandlerDelegate != null)
        {
            // the call to Application.createComponent(FacesContext, Resource)
            // is delegated because we don't have here the required Resource instance
            return _componentBuilderHandlerDelegate.createComponent(ctx);
        }
        UIComponent c = null;
        FacesContext faces = ctx.getFacesContext();
        Application app = faces.getApplication();
        if (_delegate.getBinding() != null)
        {
            ValueExpression ve = _delegate.getBinding().getValueExpression(ctx, Object.class);
            if (PhaseId.RESTORE_VIEW.equals(faces.getCurrentPhaseId()))
            {
                if (!ve.isReadOnly(faces.getELContext()))
                {
                    try
                    {
                        // force reset it is an easy and cheap way to allow "binding" attribute to work on 
                        // view scope beans or flow scope beans (using a transient variable)
                        ve.setValue(faces.getELContext(), null);
                    }
                    catch (Exception e)
                    {
                        // ignore
                    }
                }
            }
            if (this._rendererType == null)
            {
                c = app.createComponent(ve, faces, this._componentType);
            }
            else
            {
                c = app.createComponent(ve, faces, this._componentType, this._rendererType);
            }
            if (c != null)
            {
                c.setValueExpression("binding", ve);
                
                if (!ve.isReadOnly(faces.getELContext()))
                {
                    ComponentSupport.getViewRoot(ctx, c).getAttributes().put("oam.CALL_PRE_DISPOSE_VIEW", Boolean.TRUE);
                    c.subscribeToEvent(PreDisposeViewEvent.class, new ClearBindingValueExpressionListener());
                }
                
                if (c.getChildCount() > 0 || c.getFacetCount() > 0)
                {
                    // In this case, this component is used to hold a subtree that is generated
                    // dynamically. In this case, the best is mark this component to be restored
                    // fully, because this ensures the state is correctly preserved. Note this
                    // is only necessary when the component has additional children or facets,
                    // because those components requires an unique id provided by createUniqueId(),
                    // and this ensures stability of the generated ids.
                    c.getAttributes().put(DefaultFaceletsStateManagementStrategy.COMPONENT_ADDED_AFTER_BUILD_VIEW,
                                          ComponentState.REMOVE_ADD);
                    
                    if (FaceletViewDeclarationLanguageBase.isDynamicComponentNeedsRefresh(ctx.getFacesContext()))
                    {
                        FaceletViewDeclarationLanguageBase.resetDynamicComponentNeedsRefreshFlag(
                                ctx.getFacesContext());
                        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
                        if (mctx.isUsingPSSOnThisView())
                        {
                            FaceletViewDeclarationLanguage.cleanTransientBuildOnRestore(faces);
                        }
                        else
                        {
                            FaceletViewDeclarationLanguageBase.activateDynamicComponentRefreshTransientBuild(faces);
                        }
                        //
                        // Mark top binding component to be dynamically refreshed. In that way, facelets algorithm
                        // will be able to decide if the component children requires to be refreshed dynamically 
                        // or not.
                        c.getAttributes().put(
                                FaceletDynamicComponentRefreshTransientBuildEvent.
                                    DYNAMIC_COMPONENT_BINDING_NEEDS_REFRESH,
                                Boolean.TRUE);
                    }
                }
            }
        }
        else
        {
            // According to the, spec call the second alternative with null rendererType gives
            // the same result, but without the unnecesary call for FacesContext.getCurrentInstance().
            // Saves 1 call per component without rendererType (f:viewParam, h:column, f:selectItem, ...)
            // and it does not have any side effects (the spec javadoc mentions in a explicit way
            // that rendererType can be null!).
            /*
            if (this._rendererType == null)
            {
                c = app.createComponent(this._componentType);
            }
            else
            {*/
                c = app.createComponent(faces, this._componentType, this._rendererType);
            //}
        }
        return c;
    }

    /**
     * If the id TagAttribute was specified, get it's value, otherwise generate a unique id from our tagId.
     * 
     * @see TagAttribute#getValue(FaceletContext)
     * @param ctx
     *            FaceletContext to use
     * @return what should be a unique Id
     */
    protected String getId(FaceletContext ctx)
    {
        if (this._id != null)
        {
            return this._id.getValue(ctx);
        }
        return ctx.generateUniqueId(_delegate.getTagId());
    }

    @Override
    public MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset m = new MetaRulesetImpl(_delegate.getTag(), type);
        // ignore standard component attributes
        m.ignore("binding").ignore("id");

        // add auto wiring for attributes
        m.addRule(ComponentRule.INSTANCE);
        
        // add special rule for passthrough attributes
        m.addRule(PassthroughRuleImpl.INSTANCE);

        // if it's an ActionSource
        if (ActionSource.class.isAssignableFrom(type))
        {
            m.addRule(ActionSourceRule.INSTANCE);
        }

        // if it's a ValueHolder
        if (ValueHolder.class.isAssignableFrom(type))
        {
            m.addRule(ValueHolderRule.INSTANCE);

            // if it's an EditableValueHolder
            if (EditableValueHolder.class.isAssignableFrom(type))
            {
                m.ignore("submittedValue");
                m.ignore("valid");
                m.addRule(EditableValueHolderRule.INSTANCE);
            }
        }
        // PI47600: allow the class attribute to be set in custom tags 
        m.alias("class", "styleClass");
        return m;
    }
    
    /**
     * Add the default Validators to the component.
     * Also adds all validators specified by enclosing <f:validateBean> tags
     * (e.g. the BeanValidator if it is not a default validator).
     *
     * @param context The FacesContext.
     * @param mctx the AbstractFaceletContext
     * @param component The EditableValueHolder to which the validators should be added
     */
    private void addEnclosingAndDefaultValidators(FaceletContext ctx, 
                                      FaceletCompositionContext mctx, 
                                      FacesContext context, 
                                      EditableValueHolder component)
    {
        // add all enclosing validators, because they have precedence over default validators.
        Iterator<Map.Entry<String, EditableValueHolderAttachedObjectHandler>> enclosingValidatorIds =
            mctx.getEnclosingValidatorIdsAndHandlers();
        if (enclosingValidatorIds != null)
        {
            while (enclosingValidatorIds.hasNext())
            {
                Map.Entry<String, EditableValueHolderAttachedObjectHandler> entry = enclosingValidatorIds.next();
                addEnclosingValidator(context, component, entry.getKey(), entry.getValue());
            }
        }
        // add all defaultValidators
        Map<String, String> defaultValidators = context.getApplication().getDefaultValidatorInfo();
        if (defaultValidators != null && defaultValidators.size() != 0)
        {
            for (Map.Entry<String, String> entry : defaultValidators.entrySet())
            {
                if (!mctx.containsEnclosingValidatorId(entry.getKey()))
                {
                    addDefaultValidator(ctx, mctx, context, component, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void addDefaultValidator(FaceletContext ctx, FaceletCompositionContext mctx, FacesContext context, 
            EditableValueHolder component, String validatorId, String validatorClassName)
    {
        Validator enclosingValidator = null;
        
        if (validatorClassName == null)
        {
            // we have no class name for validators of enclosing <f:validateBean> tags
            // --> we have to create it to get the class name
            // note that normally we can use this instance later anyway!
            enclosingValidator = context.getApplication().createValidator(validatorId);
            validatorClassName = enclosingValidator.getClass().getName();
        }
        
        // check if the validator is already registered for the given component
        // this happens if <f:validateBean /> is nested inside the component on the view
        Validator validator = null;
        for (Validator v : component.getValidators())
        {
            if (v.getClass().getName().equals(validatorClassName))
            {
                // found
                validator = v;
                break;
            }
        }
        
        if (validator == null)
        {
            if (shouldAddDefaultValidator(ctx, mctx, component, validatorId))
            {
                if (enclosingValidator != null)
                {
                    // we can use the instance from before
                    validator = enclosingValidator;
                }
                else
                {
                    // create it
                    validator = context.getApplication().createValidator(validatorId);
                }
                // add the validator to the component
                component.addValidator(validator);
            }
            else
            {
                // we should not add the validator
                return;
            }
        }
        
        // special things to configure for a BeanValidator
        if (validator instanceof BeanValidator)
        {
            BeanValidator beanValidator = (BeanValidator) validator;
            
            // check the validationGroups
            String validationGroups =  beanValidator.getValidationGroups();
            if (validationGroups == null 
                    || validationGroups.matches(BeanValidator.EMPTY_VALIDATION_GROUPS_PATTERN))
            {
                // no validationGroups available
                // --> get the validationGroups from the stack
                //String stackGroup = mctx.getFirstValidationGroupFromStack();
                //if (stackGroup != null)
                //{
                //    validationGroups = stackGroup;
                //}
                //else
                //{
                    // no validationGroups on the stack
                    // --> set the default validationGroup
                    validationGroups = javax.validation.groups.Default.class.getName();
                //}
                beanValidator.setValidationGroups(validationGroups);
            }
        }
    }

    /**
     * Determine if the validator with the given validatorId should be added.
     *
     * @param validatorId The validatorId.
     * @param facesContext The FacesContext.
     * @param mctx the AbstractFaceletContext
     * @param component The EditableValueHolder to which the validator should be added.
     * @return true if the Validator should be added, false otherwise.
     */
    @SuppressWarnings("unchecked")
    private boolean shouldAddDefaultValidator(FaceletContext ctx, FaceletCompositionContext mctx,
                                              EditableValueHolder component, 
                                              String validatorId)
    {
        // check if the validatorId is on the exclusion list on the component
        List<String> exclusionList 
                = (List<String>) ((UIComponent) component).getAttributes()
                        .get(ValidatorTagHandlerDelegate.VALIDATOR_ID_EXCLUSION_LIST_KEY);
        if (exclusionList != null)
        {
            for (String excludedId : exclusionList)
            {
                if (excludedId.equals(validatorId))
                {
                    return false;
                }
            }
        }
        
        // check if the validatorId is on the exclusion list on the stack
        /*
        Iterator<String> it = mctx.getExcludedValidatorIds();
        if (it != null)
        {            
            while (it.hasNext())
            {
                String excludedId = it.next();
                if (excludedId.equals(validatorId))
                {
                    return false;
                }
            }
        }*/
        Iterator<Map.Entry<String, EditableValueHolderAttachedObjectHandler>> enclosingValidatorIds =
            mctx.getEnclosingValidatorIdsAndHandlers();
        if (enclosingValidatorIds != null)
        {
            while (enclosingValidatorIds.hasNext())
            {
                Map.Entry<String, EditableValueHolderAttachedObjectHandler> entry = enclosingValidatorIds.next();
                boolean validatorIdAvailable = entry.getKey() != null && !"".equals(entry.getKey());
                if (validatorIdAvailable && entry.getKey().equals(validatorId))
                {
                    if (((ValidatorHandler)((FacesWrapper<ValidatorHandler>)entry.getValue()).getWrapped())
                            .isDisabled(ctx))
                    {
                        return false;
                    }
                }
            }
        }
        
        // Some extra rules are required for Bean Validation.
        if (validatorId.equals(BeanValidator.VALIDATOR_ID))
        {
            if (!ExternalSpecifications.isBeanValidationAvailable())
            {
                // the BeanValidator was added as a default-validator, but
                // bean validation is not available on the classpath.
                // --> log a warning about this scenario.
                log.log(Level.WARNING, "Bean validation is not available on the " +
                        "classpath, thus the BeanValidator will not be added for " +
                        "the component " + component);
                return false;
            }
        }

        // By default, all default validators should be added
        return true;
    }

    private void addEnclosingValidator(FacesContext context, 
            EditableValueHolder component, String validatorId, 
            EditableValueHolderAttachedObjectHandler attachedObjectHandler)
    {
        if (shouldAddEnclosingValidator(component, validatorId))
        {
            if (attachedObjectHandler != null)
            {
                attachedObjectHandler.applyAttachedObject(context, (UIComponent) component);
            }
            else
            {
                Validator validator = null;
                // create it
                validator = context.getApplication().createValidator(validatorId);

                // special things to configure for a BeanValidator
                if (validator instanceof BeanValidator)
                {
                    BeanValidator beanValidator = (BeanValidator) validator;
                    
                    // check the validationGroups
                    String validationGroups =  beanValidator.getValidationGroups();
                    if (validationGroups == null 
                            || validationGroups.matches(BeanValidator.EMPTY_VALIDATION_GROUPS_PATTERN))
                    {
                        // no validationGroups available
                        // --> get the validationGroups from the stack
                        //String stackGroup = mctx.getFirstValidationGroupFromStack();
                        //if (stackGroup != null)
                        //{
                        //    validationGroups = stackGroup;
                        //}
                        //else
                        //{
                            // no validationGroups on the stack
                            // --> set the default validationGroup
                            validationGroups = javax.validation.groups.Default.class.getName();
                        //}
                        beanValidator.setValidationGroups(validationGroups);
                    }
                }
                
                // add the validator to the component
                component.addValidator(validator);
            }
        }
    }

    /**
     * Determine if the validator with the given validatorId should be added.
     * 
     * The difference here with shouldAddEnclosingValidator is the inner one has
     * precedence over the outer one, so a disable="true" over the same outer 
     * validator, the inner one should ignore this condition. 
     * 
     * @param mctx
     * @param facesContext
     * @param component
     * @param validatorId
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean shouldAddEnclosingValidator(
            EditableValueHolder component, 
            String validatorId)
    {
        // check if the validatorId is on the exclusion list on the component
        List<String> exclusionList = (List<String>) ((UIComponent) component)
                .getAttributes()
                .get(ValidatorTagHandlerDelegate.VALIDATOR_ID_EXCLUSION_LIST_KEY);
        if (exclusionList != null)
        {
            for (String excludedId : exclusionList)
            {
                if (excludedId.equals(validatorId))
                {
                    return false;
                }
            }
        }

        // Some extra rules are required for Bean Validation.
        if (validatorId.equals(BeanValidator.VALIDATOR_ID) &&
            !ExternalSpecifications.isBeanValidationAvailable())
        {
            // the BeanValidator was added as a default-validator, but
            // bean validation is not available on the classpath.
            // --> log a warning about this scenario.
            log.log(Level.WARNING,
                    "Bean validation is not available on the "
                            + "classpath, thus the BeanValidator will not be added for "
                            + "the component " + component);
            return false;
        }

        // By default, all default validators should be added
        return true;
    }
    
    private static class PublishFaceletDynamicComponentRefreshTransientBuildCallback implements VisitCallback
    {
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            context.getFacesContext().getApplication().publishEvent(
                    context.getFacesContext(), FaceletDynamicComponentRefreshTransientBuildEvent.class, 
                    target.getClass(), target);
            return VisitResult.ACCEPT;
        }
    }
}
