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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.el.MethodExpression;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.AjaxBehaviorListener;
import javax.faces.view.BehaviorHolderAttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;
import org.apache.myfaces.view.facelets.tag.composite.InsertChildrenHandler;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.ui.DecorateHandler;
import org.apache.myfaces.view.facelets.tag.ui.IncludeHandler;
import org.apache.myfaces.view.facelets.tag.ui.InsertHandler;

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
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1545492 $ $Date: 2013-11-26 01:19:50 +0000 (Tue, 26 Nov 2013) $
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
    public final static String STANDARD_JSF_AJAX_LIBRARY_LOADED
            = "org.apache.myfaces.STANDARD_JSF_AJAX_LIBRARY_LOADED";

    /**
     * 
     */
    @JSFFaceletAttribute(name = "disabled", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.Boolean")
    private final TagAttribute _disabled;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "event", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute _event;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "execute", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.Object")
    private final TagAttribute _execute;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "immediate", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.Boolean")
    private final TagAttribute _immediate;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "listener", className = "javax.el.MethodExpression",
            deferredMethodSignature = "public void m(javax.faces.event.AjaxBehaviorEvent evt) "
                                      + "throws javax.faces.event.AbortProcessingException")
    private final TagAttribute _listener;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "onevent", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute _onevent;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "onerror", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute _onerror;

    /**
     * 
     */
    @JSFFaceletAttribute(name = "render", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.Object")
    private final TagAttribute _render;
    /**
     * 
     */
    @JSFFaceletAttribute(name = "delay", className = "javax.el.ValueExpression",
                         deferredValueType = "java.lang.String")
    private final TagAttribute _delay;
    
    @JSFFaceletAttribute(name = "resetValues", className = "javax.el.ValueExpression",
            deferredValueType = "java.lang.Boolean")
    private final TagAttribute _resetValues;
    
    private final boolean _wrapMode;

    public AjaxHandler(TagConfig config)
    {
        super(config);
        _disabled = getAttribute("disabled");
        _event = getAttribute("event");
        _execute = getAttribute("execute");
        _immediate = getAttribute("immediate");
        _listener = getAttribute("listener");
        _onerror = getAttribute("onerror");
        _onevent = getAttribute("onevent");
        _render = getAttribute("render");
        _delay = getAttribute("delay");
        _resetValues = getAttribute("resetValues");
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
        Collection<FaceletHandler> compHandlerList = 
            TagHandlerUtils.findNextByType(nextHandler, ComponentHandler.class, 
                    InsertChildrenHandler.class, InsertHandler.class, DecorateHandler.class, IncludeHandler.class);
        
        _wrapMode = !compHandlerList.isEmpty();
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        //Apply only if we are creating a new component
        if (!ComponentHandler.isNew(parent))
        {
            if (_wrapMode)
            {
                AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
                // In this case it will be only applied to components inserted by 
                // c:if or related tags, in other cases, ComponentTagHandlerDelegate should
                // not reapply ajax tag.
                actx.pushAjaxHandlerToStack(this);
                nextHandler.apply(ctx, parent);
                actx.popAjaxHandlerToStack();
            }
            return;
        }
        if (_wrapMode)
        {
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            // Push and pop this ajax handler to the stack, to delegate the
            // call to applyAttachedObject to ComponentTagHandlerDelegate
            // The default one proposed here is
            // use a different stack on DefaultFaceletContext.applyCompositeComponent,
            // so components inside composite:implementation tag will not be
            // affected by f:ajax outsider handlers.
            actx.pushAjaxHandlerToStack(this);
            nextHandler.apply(ctx, parent);
            actx.popAjaxHandlerToStack();
        }
        else
        {
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
                mctx.addAttachedObjectHandler(
                        parent, this);
            }
            else
            {
                throw new TagException(this.tag,
                        "Parent is not composite component or of type ClientBehaviorHolder, type is: "
                                + parent);
            }
        }
        
        registerJsfAjaxDefaultResource(ctx, parent);
    }
    
    public static void registerJsfAjaxDefaultResource(FaceletContext ctx, UIComponent parent)
    {
        // Register the standard ajax library on the current page in this way:
        //
        // <h:outputScript name="jsf.js" library="javax.faces" target="head"/>
        //
        // If no h:head component is in the page, we must anyway render the script inline,
        // so the only way to make sure we are doing this is add a outputScript component.
        // Note that call directly UIViewRoot.addComponentResource or use a listener 
        // does not work in this case, because check this condition will requires 
        // traverse the whole tree looking for h:head component.
        FacesContext facesContext = ctx.getFacesContext();
        if (!facesContext.getAttributes().containsKey(STANDARD_JSF_AJAX_LIBRARY_LOADED))
        {
            UIComponent outputScript = facesContext.getApplication().
                createComponent(facesContext, "javax.faces.Output", "javax.faces.resource.Script");
            outputScript.getAttributes().put(JSFAttr.NAME_ATTR, ResourceUtils.JSF_JS_RESOURCE_NAME);
            outputScript.getAttributes().put(JSFAttr.LIBRARY_ATTR, ResourceUtils.JAVAX_FACES_LIBRARY_NAME);
            outputScript.getAttributes().put(JSFAttr.TARGET_ATTR, "head");

            //AbstractFaceletContext actx = (AbstractFaceletContext) ctx;

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
            facesContext.getAttributes().put(STANDARD_JSF_AJAX_LIBRARY_LOADED, Boolean.TRUE);
        }
    }

    /**
     * ViewDeclarationLanguage.retargetAttachedObjects uses it to check
     * if the the target to be processed is applicable for this handler
     */
    public String getEventName()
    {
        if (_event == null)
        {
            return null;
        }
        else
        {
            if (_event.isLiteral())
            {
                return _event.getValue();
            }
            else
            {
                FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().
                        getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
                return (String) _event.getValueExpression(faceletContext, String.class).getValue(faceletContext);
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
    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context
                .getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);

        // cast to a ClientBehaviorHolder
        ClientBehaviorHolder cvh = (ClientBehaviorHolder) parent;
        
        String eventName = null;
        if (_event != null)
        {
            if (_event.isLiteral())
            {
                eventName = _event.getValue();
            }
            else
            {
                eventName = (String) _event.getValueExpression(faceletContext, String.class).getValue(faceletContext);
            }
        }
        if (eventName == null)
        {
            eventName = cvh.getDefaultEventName();
            if (eventName == null)
            {
                if (_wrapMode)
                {
                    // No eventName defined, we can't apply this tag to this component, because
                    // there is no event defined to attach it, but since we are in wrap mode
                    // we have here the case that the component could not be the target
                    // for this attached object.
                    return;
                }
                else
                {
                    throw new TagAttributeException(_event,
                            "eventName could not be defined for f:ajax tag with no wrap mode.");
                }
            }
        }
        else if (!cvh.getEventNames().contains(eventName))
        {
            if (_wrapMode)
            {
                // The current component does not implement the event selected,
                // this ajax behavior cannot be applied, but we can't throw any exception
                // since we are in wrap mode and we have here the case that the 
                // component could not be the target for this attached object.
                return;
            }
            else
            {
                throw new TagAttributeException(_event,
                        "event it is not a valid eventName defined for this component");
            }
        }
        
        Map<String, List<ClientBehavior>> clientBehaviors = cvh.getClientBehaviors();

        List<ClientBehavior> clientBehaviorList = clientBehaviors.get(eventName);
        if (clientBehaviorList != null && !clientBehaviorList.isEmpty())
        {
            for (ClientBehavior cb : clientBehaviorList)
            {
                if (cb instanceof AjaxBehavior)
                {
                    // The most inner one has been applied, so according to 
                    // jsf 2.0 spec section 10.4.1.1 it is not necessary to apply
                    // this one, because the inner one has precendece over
                    // the outer one.
                    return;
                }
            }
        }

        AjaxBehavior ajaxBehavior = createBehavior(context);

        if (_disabled != null)
        {
            if (_disabled.isLiteral())
            {
                ajaxBehavior.setDisabled(_disabled.getBoolean(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("disabled", _disabled
                        .getValueExpression(faceletContext, Boolean.class));
            }
        }
        if (_execute != null)
        {
            ajaxBehavior.setValueExpression("execute", _execute
                    .getValueExpression(faceletContext, Object.class));
        }
        if (_immediate != null)
        {
            if (_immediate.isLiteral())
            {
                ajaxBehavior
                        .setImmediate(_immediate.getBoolean(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("immediate", _immediate
                        .getValueExpression(faceletContext, Boolean.class));
            }
        }
        if (_listener != null)
        {
            MethodExpression expr = _listener.getMethodExpression(
                    faceletContext, Void.TYPE, AJAX_BEHAVIOR_LISTENER_SIG);
            AjaxBehaviorListener abl = new AjaxBehaviorListenerImpl(expr);
            ajaxBehavior.addAjaxBehaviorListener(abl);
        }
        if (_onerror != null)
        {
            if (_onerror.isLiteral())
            {
                ajaxBehavior.setOnerror(_onerror.getValue(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("onerror", _onerror
                        .getValueExpression(faceletContext, String.class));
            }
        }
        if (_onevent != null)
        {
            if (_onevent.isLiteral())
            {
                ajaxBehavior.setOnevent(_onevent.getValue(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("onevent", _onevent
                        .getValueExpression(faceletContext, String.class));
            }
        }
        if (_render != null)
        {
            ajaxBehavior.setValueExpression("render", _render
                    .getValueExpression(faceletContext, Object.class));
        }
        if (_delay != null)
        {
            if (_delay.isLiteral())
            {
                ajaxBehavior.setDelay(_delay.getValue(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("delay", _delay
                    .getValueExpression(faceletContext, String.class));
            }
        }
       if (_resetValues != null)
        {
            if (_resetValues.isLiteral())
            {
                ajaxBehavior
                        .setResetValues(_resetValues.getBoolean(faceletContext));
            }
            else
            {
                ajaxBehavior.setValueExpression("resetValues", _resetValues
                        .getValueExpression(faceletContext, Boolean.class));
            }
        }
        cvh.addClientBehavior(eventName, ajaxBehavior);
    }

    protected AjaxBehavior createBehavior(FacesContext context)
    {
        return (AjaxBehavior) context.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
    }

    /**
     * The documentation says this attribute should not be used since it is not
     * taken into account. Instead, getEventName is used on 
     * ViewDeclarationLanguage.retargetAttachedObjects.
     */
    public String getFor()
    {
        return null;
    }

    /**
     * Wraps a method expression in a AjaxBehaviorListener
     */
    public final static class AjaxBehaviorListenerImpl implements
            AjaxBehaviorListener, PartialStateHolder
    {
        private MethodExpression _expr;
        private boolean _transient;
        private boolean _initialStateMarked;
        
        public AjaxBehaviorListenerImpl ()
        {
        }
        
        public AjaxBehaviorListenerImpl(MethodExpression expr)
        {
            _expr = expr;
        }

        public void processAjaxBehavior(AjaxBehaviorEvent event)
                throws AbortProcessingException
        {
            _expr.invoke(FacesContext.getCurrentInstance().getELContext(),
                    new Object[] { event });
        }

        public boolean isTransient()
        {
            return _transient;
        }

        public void restoreState(FacesContext context, Object state)
        {
            if (state == null)
            {
                return;
            }
            _expr = (MethodExpression) state;
        }

        public Object saveState(FacesContext context)
        {
            if (initialStateMarked())
            {
                return null;
            }
            return _expr;
        }

        public void setTransient(boolean newTransientValue)
        {
            _transient = newTransientValue;
        }
        
        public void clearInitialState()
        {
            _initialStateMarked = false;
        }

        public boolean initialStateMarked()
        {
            return _initialStateMarked;
        }

        public void markInitialState()
        {
            _initialStateMarked = true;
        }
    }
}
