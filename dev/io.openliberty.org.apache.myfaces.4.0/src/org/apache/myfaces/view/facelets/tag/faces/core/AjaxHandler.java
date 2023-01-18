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
package org.apache.myfaces.view.facelets.tag.faces.core;

import java.io.IOException;
import java.util.Collection;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.UIComponent;
import static jakarta.faces.component.UINamingContainer.getSeparatorChar;
import jakarta.faces.component.UniqueIdVendor;
import jakarta.faces.component.behavior.AjaxBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.AjaxBehaviorListener;
import jakarta.faces.view.BehaviorHolderAttachedObjectHandler;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.FaceletHandler;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagException;
import jakarta.faces.view.facelets.TagHandler;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;
import org.apache.myfaces.view.facelets.tag.composite.InsertChildrenHandler;
import org.apache.myfaces.view.facelets.tag.faces.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.ui.DecorateHandler;
import org.apache.myfaces.view.facelets.tag.ui.IncludeHandler;
import org.apache.myfaces.view.facelets.tag.ui.InsertHandler;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;
import org.apache.myfaces.view.facelets.tag.composite.ClientBehaviorRedirectEventComponentWrapper;

/**
 * This tag creates an instance of AjaxBehavior, and associates it with the nearest 
 * parent UIComponent that implements ClientBehaviorHolder interface. This tag can
 * be used on single or composite components.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * <p>
 * According to the documentation, the tag handler implementing this tag should meet
 * the following conditions:  
 * </p>
 * <ul>
 * <li>Since this tag attach objects to UIComponent instances, and those instances 
 * implements Behavior interface, this component should implement 
 * BehaviorHolderAttachedObjectHandler interface.</li>
 * <li>f:ajax does not support binding property. In theory we should do something similar
 * to f:convertDateTime tag does: extends from ConverterHandler and override setAttributes
 * method, but in this case BehaviorTagHandlerDelegate has binding property defined, so
 * if we extend from BehaviorHandler we add binding support to f:ajax.</li>
 * <li>This tag works as a attached object handler, but note on the api there is no component
 * to define a target for a behavior. See comment inside apply() method.</li>
 * </ul>
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
@JSFFaceletTag(name = "f:ajax")
public class AjaxHandler extends TagHandler implements
        BehaviorHolderAttachedObjectHandler
{

    public final static Class<?>[] AJAX_BEHAVIOR_LISTENER_SIG = new Class<?>[] { AjaxBehaviorEvent.class };
    
    /**
     * Constant used to check if in the current build view it has been rendered the standard jsf javascript
     * library. It is necessary to remove this key from facesContext attribute map after build, to keep
     * working this code for next views to be built.
     */
    public final static String FACES_JS_DYNAMICALLY_ADDED
            = "org.apache.myfaces.FACES_JS_DYNAMICALLY_ADDED";

    @JSFFaceletAttribute(name = "disabled", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.Boolean")
    private final TagAttribute disabled;

    @JSFFaceletAttribute(name = "event", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute event;

    @JSFFaceletAttribute(name = "execute", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.Object")
    private final TagAttribute execute;

    @JSFFaceletAttribute(name = "immediate", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.Boolean")
    private final TagAttribute immediate;

    @JSFFaceletAttribute(name = "listener", className = "jakarta.el.MethodExpression",
            deferredMethodSignature = "public void m(jakarta.faces.event.AjaxBehaviorEvent evt) "
                                      + "throws jakarta.faces.event.AbortProcessingException")
    private final TagAttribute listener;

    @JSFFaceletAttribute(name = "onevent", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute onevent;

    @JSFFaceletAttribute(name = "onerror", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute onerror;

    @JSFFaceletAttribute(name = "render", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.Object")
    private final TagAttribute render;

    @JSFFaceletAttribute(name = "delay", className = "jakarta.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute delay;
    
    @JSFFaceletAttribute(name = "resetValues", className = "jakarta.el.ValueExpression",
            deferredValueType = "java.lang.Boolean")
    private final TagAttribute resetValues;
    
    private final boolean wrappingMode;

    public AjaxHandler(TagConfig config)
    {
        super(config);
        disabled = getAttribute("disabled");
        event = getAttribute("event");
        execute = getAttribute("execute");
        immediate = getAttribute("immediate");
        listener = getAttribute("listener");
        onerror = getAttribute("onerror");
        onevent = getAttribute("onevent");
        render = getAttribute("render");
        delay = getAttribute("delay");
        resetValues = getAttribute("resetValues");

        // According to the spec, this tag works in two different ways:
        // 1. Apply an ajax behavior for a selected component in this way
        //    <x:component><f:ajax ..../></x:component>
        // 2. Apply an ajax behavior for a group of components inside it
        //   <f:ajax ....><x:componentA .../><x:componentB .../></f:ajax>
        //
        // The first problem is how to discriminate if f:ajax tag is on a
        // "leaf" or if contain other components.
        //
        // One option is use the same strategy to cache instance for 
        // <composite:interface> handler: traverse the tree for instances of 
        // ComponentHandler. If it is found, wrapMode is used otherwise
        // suppose f:ajax is the one wrapped by a component.
        Collection<FaceletHandler> compHandlerList = TagHandlerUtils.findNextByType(nextHandler,
                ComponentHandler.class, InsertChildrenHandler.class, InsertHandler.class,
                DecorateHandler.class, IncludeHandler.class);
        wrappingMode = !compHandlerList.isEmpty();
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        if (wrappingMode)
        {
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            // In this case it will be only applied to components inserted by 
            // c:if or related tags, in other cases, ComponentTagHandlerDelegate should
            // not reapply ajax tag.
            actx.pushAjaxHandlerToStack(this);
            nextHandler.apply(ctx, parent);
            actx.popAjaxHandlerToStack();
            
            registerFacesJsResource(ctx, parent);
        }
        else
        {
            //Apply only if we are creating a new component
            if (!ComponentHandler.isNew(parent))
            {
                return;
            }

            if (parent instanceof ClientBehaviorHolder)
            {
                //Apply this handler directly over the parent
                applyAttachedObject(ctx.getFacesContext(), parent);
            }
            else if (UIComponent.isCompositeComponent(parent))
            {
                FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
                // It is supposed that for composite components, this tag should
                // add itself as a target, but note that on whole api does not exists
                // some tag that expose client behaviors as targets for composite
                // components. In RI, there exists a tag called composite:clientBehavior,
                // but does not appear on spec or javadoc, maybe because this could be
                // understand as an implementation detail, after all there exists a key
                // called AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY that could be
                // used to create a tag outside jsf implementation to attach targets.
                mctx.addAttachedObjectHandler(parent, this);
            }
            else
            {
                throw new TagException(this.tag,
                        "Parent is not composite component or of type ClientBehaviorHolder; Type is: " + parent);
            }
            
            registerFacesJsResource(ctx, parent);
        }
    }
    
    public static void registerFacesJsResource(FaceletContext ctx, UIComponent parent)
    {
        // Register the standard ajax library on the current page in this way:
        //
        // <h:outputScript name="faces.js" library="jakarta.faces" target="head"/>
        //
        // If no h:head component is in the page, we must anyway render the script inline,
        // so the only way to make sure we are doing this is add a outputScript component.
        // Note that call directly UIViewRoot.addComponentResource or use a listener 
        // does not work in this case, because check this condition will requires 
        // traverse the whole tree looking for h:head component.
        FacesContext facesContext = ctx.getFacesContext();
        if (!facesContext.getAttributes().containsKey(FACES_JS_DYNAMICALLY_ADDED))
        {
            UIComponent outputScript = facesContext.getApplication().
                createComponent(facesContext, "jakarta.faces.Output", ResourceUtils.DEFAULT_SCRIPT_RENDERER_TYPE);
            outputScript.getAttributes().put(ComponentAttrs.NAME_ATTR, ResourceHandler.FACES_SCRIPT_RESOURCE_NAME);
            outputScript.getAttributes().put(ComponentAttrs.LIBRARY_ATTR, ResourceHandler.FACES_SCRIPT_LIBRARY_NAME);
            outputScript.getAttributes().put(ComponentAttrs.TARGET_ATTR, "head");

            // Since this component will be relocated, we need a generated clientId from the
            // viewRoot, so when this one is relocated, its parent will be this UIViewRoot instance
            // and prevent a duplicate id exception.
            UniqueIdVendor uniqueIdVendor = ComponentSupport.getViewRoot(ctx, parent);
            // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
            // and call createUniqueId()
            String uid = uniqueIdVendor.createUniqueId(ctx.getFacesContext(),null);
            outputScript.setId(uid);
            
            parent.getChildren().add(outputScript);
            
            if (FaceletCompositionContext.getCurrentInstance(ctx).isMarkInitialState())
            {
                //Call it only if we are using partial state saving
                outputScript.markInitialState();
            }            
            facesContext.getAttributes().put(FACES_JS_DYNAMICALLY_ADDED, Boolean.TRUE);
        }
    }

    /**
     * ViewDeclarationLanguage.retargetAttachedObjects uses it to check
     * if the the target to be processed is applicable for this handler
     */
    @Override
    public String getEventName()
    {
        if (event == null)
        {
            return null;
        }
        else
        {
            if (event.isLiteral())
            {
                return event.getValue();
            }
            else
            {
                FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().
                        getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
                return (String) event.getValueExpression(faceletContext, String.class).getValue(faceletContext);
            }
        }
    }

    /**
     * This method should create an AjaxBehavior object and attach it to the
     * parent component.
     * 
     * Also, it should check if the parent can apply the selected AjaxBehavior
     * to the selected component through ClientBehaviorHolder.getEventNames() or
     * ClientBehaviorHolder.getDefaultEventName()
     */
    @Override
    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        FaceletContext faceletContext =
                (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
        ClientBehaviorHolder cvh = (ClientBehaviorHolder) parent;

        String eventName = null;
        if (event != null)
        {
            if (event.isLiteral())
            {
                eventName = event.getValue();
            }
            else
            {
                eventName = (String) event.getValueExpression(faceletContext, String.class).getValue(faceletContext);
            }
        }
        if (eventName == null)
        {
            eventName = cvh.getDefaultEventName();
            if (eventName == null)
            {
                if (wrappingMode)
                {
                    // No eventName defined, we can't apply this tag to this component, because
                    // there is no event defined to attach it, but since we are in wrap mode
                    // we have here the case that the component could not be the target
                    // for this attached object.
                    return;
                }
                else
                {
                    throw new TagAttributeException(event,
                            "eventName could not be defined for f:ajax tag with no wrap mode.");
                }
            }
        }
        else if (!cvh.getEventNames().contains(eventName))
        {
            if (wrappingMode)
            {
                // The current component does not implement the event selected,
                // this ajax behavior cannot be applied, but we can't throw any exception
                // since we are in wrap mode and we have here the case that the 
                // component could not be the target for this attached object.
                return;
            }
            else
            {
                throw new TagAttributeException(event,
                        "event it is not a valid eventName defined for this component");
            }
        }

        AjaxBehavior ajaxBehavior = (AjaxBehavior) context.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
        setAttribute(faceletContext, ajaxBehavior, disabled, Boolean.class, (v) -> ajaxBehavior.setDisabled(v));
        setAttribute(faceletContext, ajaxBehavior, execute, Object.class);
        setAttribute(faceletContext, ajaxBehavior, immediate, Boolean.class, (v) -> ajaxBehavior.setImmediate(v));
        setAttribute(faceletContext, ajaxBehavior, onerror, String.class, (v) -> ajaxBehavior.setOnerror(v));
        setAttribute(faceletContext, ajaxBehavior, onevent, String.class, (v) -> ajaxBehavior.setOnevent(v));
        setAttribute(faceletContext, ajaxBehavior, render, Object.class);
        setAttribute(faceletContext, ajaxBehavior, delay, String.class, (v) -> ajaxBehavior.setDelay(v));
        setAttribute(faceletContext, ajaxBehavior, resetValues, Boolean.class, (v) -> ajaxBehavior.setResetValues(v));
        if (listener != null)
        {
            MethodExpression expr = listener.getMethodExpression(
                    faceletContext, Void.TYPE, AJAX_BEHAVIOR_LISTENER_SIG);
            AjaxBehaviorListener abl = new AjaxBehaviorListenerImpl(expr);
            ajaxBehavior.addAjaxBehaviorListener(abl);
        }

        // remap @this to the composite targets
        if (parent instanceof ClientBehaviorRedirectEventComponentWrapper)
        {
            ValueExpression targets = ((ClientBehaviorRedirectEventComponentWrapper) parent).getTargets();
            if (targets != null)
            {
                String targetClientIds = (String) targets.getValue(context.getELContext());
                if (targetClientIds != null)
                {
                    String separatorChar = String.valueOf(getSeparatorChar(context));

                    Collection<String> execute = ajaxBehavior.getExecute();              
                    if (execute.isEmpty() || execute.contains("@this"))
                    {
                        Collection<String> newExecute = new ArrayList<>(execute);
                        newExecute.remove("@this");
                        for (String id : targetClientIds.trim().split(" +"))
                        {
                            newExecute.add("@this" + separatorChar + id);
                        }
                        ajaxBehavior.setExecute(newExecute);
                    }

                    Collection<String> render = ajaxBehavior.getRender();              
                    if (render.isEmpty() || render.contains("@this"))
                    {
                        Collection<String> newRender = new ArrayList<>(render);
                        newRender.remove("@this");
                        for (String id : targetClientIds.trim().split(" +"))
                        {
                            newRender.add("@this" + separatorChar + id);
                        }
                        ajaxBehavior.setRender(newRender);
                    }
                }
            }
        }

        cvh.addClientBehavior(eventName, ajaxBehavior);
    }

    protected <T> void setAttribute(FaceletContext faceletContext, AjaxBehavior behavior, TagAttribute attr,
            Class<T> type)
    {
        setAttribute(faceletContext, behavior, attr, type, null);
    }
    
    protected <T> void setAttribute(FaceletContext faceletContext, AjaxBehavior behavior, TagAttribute attr,
            Class<T> type, Consumer<T> setter)
    {
        if (attr != null)
        {
            if (!attr.isLiteral() || setter == null)
            {
                behavior.setValueExpression(attr.getLocalName(), attr.getValueExpression(faceletContext, type));
            }
            else
            {
                if (type == Boolean.class)
                {
                    ((Consumer<Boolean>) setter).accept(attr.getBoolean(faceletContext));
                }
                else
                {
                    setter.accept((T) attr.getValue(faceletContext));
                }
            }
        }
    }

    /**
     * The documentation says this attribute should not be used since it is not
     * taken into account. Instead, getEventName is used on 
     * ViewDeclarationLanguage.retargetAttachedObjects.
     */
    @Override
    public String getFor()
    {
        return null;
    }
}
