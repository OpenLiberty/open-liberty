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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.view.ActionSource2AttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.spi.InjectionProviderException;
import org.apache.myfaces.spi.InjectionProviderFactory;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.util.ReflectionUtil;

import com.ibm.ws.jsf.spi.WASInjectionProvider;
import com.ibm.ws.managedobject.ManagedObject;

/**
 * Register an ActionListener instance on the UIComponent associated with the closest parent UIComponent custom action.
 * <p/> See <a target="_new"
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/actionListener.html">tag documentation</a>.
 * 
 * @see javax.faces.event.ActionListener
 * @see javax.faces.component.ActionSource
 * @author Jacob Hookom
 * @version $Id: ActionListenerHandler.java 1194861 2011-10-29 10:02:34Z struberg $
 */
@JSFFaceletTag(
            name = "f:actionListener",
            bodyContent = "empty", 
            tagClass="org.apache.myfaces.taglib.core.ActionListenerTag")
public final class ActionListenerHandler extends TagHandler
    implements ActionSource2AttachedObjectHandler 
{

    private final static class LazyActionListener implements ActionListener, Serializable
    {

        private static final long serialVersionUID = -9202120013153262119L;

        private final String type;
        private final ValueExpression binding;

        public LazyActionListener(String type, ValueExpression binding)
        {
            this.type = type;
            this.binding = binding;
        }

        public void processAction(ActionEvent event) throws AbortProcessingException
        {
            ActionListener instance = null;
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null)
            {
                return;
            }
            if (this.binding != null)
            {
                instance = (ActionListener) binding.getValue(faces.getELContext());
            }
            if (instance == null && this.type != null)
            {
                try
                {
                    // CDI 1.2 : use inject to create the object instance.
                    Class listenerClass = ReflectionUtil.forName(this.type);
                    instance = injectAndPostConstruct(listenerClass, faces.getExternalContext());
                }
                catch (Exception e)
                {
                    throw new AbortProcessingException("Couldn't Lazily instantiate ValueChangeListener", e);
                }
                if (this.binding != null)
                {
                    binding.setValue(faces.getELContext(), instance);
                }
            }
            if (instance != null)
            {
                instance.processAction(event);
            }
        }
    }

    private final TagAttribute binding;

    private final String listenerType;

    /**
     * @param config
     */
    public ActionListenerHandler(TagConfig config)
    {
        super(config);
        this.binding = this.getAttribute("binding");
        TagAttribute type = this.getAttribute("type");
        if (type != null)
        {
            if (!type.isLiteral())
            {
                throw new TagAttributeException(type, "Must be a literal class name of type ActionListener");
            }
            else
            {
                // test it out
                try
                {
                    ReflectionUtil.forName(type.getValue());
                }
                catch (ClassNotFoundException e)
                {
                    throw new TagAttributeException(type, "Couldn't qualify ActionListener", e);
                }
            }
            this.listenerType = type.getValue();
        }
        else
        {
            this.listenerType = null;
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
            applyAttachedObject(ctx.getFacesContext(), parent);
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
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);

        ActionSource as = (ActionSource) parent;
        ValueExpression b = null;
        if (this.binding != null)
        {
            b = this.binding.getValueExpression(faceletContext, ActionListener.class);
        }
        ActionListener listener = new LazyActionListener(this.listenerType, b);
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

    /**
     * 
     * @param Klass - klass of object to be created
     * @param eContext - externalContext
     * @return instance of Klass
     * @throws InjectionProviderException
     * 
     *  CDI 1.2 support
     *  Uses CDIInjectionPorvider to create the object so that if object uses constructor injection it works.
     */
    private static ActionListener injectAndPostConstruct(Class Klass, ExternalContext eContext) throws InjectionProviderException {

        WASInjectionProvider injectionProvider = (WASInjectionProvider) InjectionProviderFactory.getInjectionProviderFactory(eContext).getInjectionProvider(eContext);

        ManagedObject mo = null;
        boolean disableFaceletActionListenerPreDestroy = MyfacesConfig.getCurrentInstance(eContext).isDisableFaceletActionListenerPreDestroy();

        // If disableFaceletActionListenerPreDestroy is enabled (set to true),
        // do not add the ActionListener instance to the Injected Bean list in the application map.
        // This prevents the Injected Bean list to grow and PreDestroy in ActionListener is never called.
        if (disableFaceletActionListenerPreDestroy) {
            mo = (ManagedObject) injectionProvider.inject(Klass, true, null);
        }
        else {
            mo = (ManagedObject) injectionProvider.inject(Klass, true, eContext);
        }

        return (ActionListener) mo.getObject();

    }
}
