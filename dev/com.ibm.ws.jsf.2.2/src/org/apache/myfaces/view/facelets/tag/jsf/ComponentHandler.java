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
package org.apache.myfaces.view.facelets.tag.jsf;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;

import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

/**
 * Implementation of the tag logic used in the JSF specification. This is your golden hammer for wiring UIComponents to
 * Facelets.
 * 
 * @deprecated Use javax.faces.view.facelets.ComponentHandler instead
 * @author Jacob Hookom
 * @version $Id: ComponentHandler.java 1189343 2011-10-26 17:53:36Z struberg $
 */
@Deprecated
public class ComponentHandler extends MetaTagHandler
{

    //private final static Logger log = Logger.getLogger("facelets.tag.component");
    private final static Logger log = Logger.getLogger(ComponentHandler.class.getName());

    private final TagAttribute binding;

    private final String componentType;

    private final TagAttribute id;

    private final String rendererType;

    public ComponentHandler(ComponentConfig config)
    {
        super(config);
        this.componentType = config.getComponentType();
        this.rendererType = config.getRendererType();
        this.id = this.getAttribute("id");
        this.binding = this.getAttribute("binding");
    }

    /**
     * Method handles UIComponent tree creation in accordance with the JSF 1.2 spec.
     * <ol>
     * <li>First determines this UIComponent's id by calling {@link #getId(FaceletContext) getId(FaceletContext)}.</li>
     * <li>Search the parent for an existing UIComponent of the id we just grabbed</li>
     * <li>If found, {@link #markForDeletion(UIComponent) mark} its children for deletion.</li>
     * <li>If <i>not</i> found, call {@link #createComponent(FaceletContext) createComponent}.
     * <ol>
     * <li>Only here do we apply {@link ObjectHandler#setAttributes(FaceletContext, Object) attributes}</li>
     * <li>Set the UIComponent's id</li>
     * <li>Set the RendererType of this instance</li>
     * </ol>
     * </li>
     * <li>Now apply the nextHandler, passing the UIComponent we've created/found.</li>
     * <li>Now add the UIComponent to the passed parent</li>
     * <li>Lastly, if the UIComponent already existed (found), then {@link #finalizeForDeletion(UIComponent) finalize}
     * for deletion.</li>
     * </ol>
     * 
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, javax.faces.component.UIComponent)
     * 
     * @throws TagException
     *             if the UIComponent parent is null
     */
    public final void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException
    {
        // make sure our parent is not null
        if (parent == null)
        {
            throw new TagException(this.tag, "Parent UIComponent was null");
        }

        // possible facet scoped
        String facetName = this.getFacetName(ctx, parent);

        // our id
        String id = ctx.generateUniqueId(this.tagId);

        // grab our component
        UIComponent c = ComponentSupport.findChildByTagId(parent, id);
        boolean componentFound = false;
        if (c != null)
        {
            componentFound = true;
            // mark all children for cleaning
            if (log.isLoggable(Level.FINE))
            {
                log.fine(this.tag + " Component[" + id + "] Found, marking children for cleanup");
            }
            ComponentSupport.markForDeletion(c);
        }
        else
        {
            c = this.createComponent(ctx);
            if (log.isLoggable(Level.FINE))
            {
                log.fine(this.tag + " Component[" + id + "] Created: " + c.getClass().getName());
            }
            this.setAttributes(ctx, c);

            // mark it owned by a facelet instance
            c.getAttributes().put(ComponentSupport.MARK_CREATED, id);

            // assign our unique id
            if (this.id != null)
            {
                c.setId(this.id.getValue(ctx));
            }
            else
            {
                UIViewRoot root = ComponentSupport.getViewRoot(ctx, parent);
                if (root != null)
                {
                    String uid = root.createUniqueId();
                    c.setId(uid);
                }
            }

            if (this.rendererType != null)
            {
                c.setRendererType(this.rendererType);
            }

            // hook method
            this.onComponentCreated(ctx, c, parent);
        }

        // first allow c to get populated
        this.applyNextHandler(ctx, c);

        // finish cleaning up orphaned children
        if (componentFound)
        {
            ComponentSupport.finalizeForDeletion(c);

            if (facetName == null)
            {
                parent.getChildren().remove(c);
            }
        }

        this.onComponentPopulated(ctx, c, parent);

        // add to the tree afterwards
        // this allows children to determine if it's
        // been part of the tree or not yet
        if (facetName == null)
        {
            parent.getChildren().add(c);
        }
        else
        {
            parent.getFacets().put(facetName, c);
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
        // TODO: REFACTOR - "facelets.FACET_NAME" should be a constant somewhere, used to be in FacetHandler
        //                  from real Facelets
        return (String) parent.getAttributes().get("facelets.FACET_NAME");
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
        UIComponent c = null;
        FacesContext faces = ctx.getFacesContext();
        Application app = faces.getApplication();
        if (this.binding != null)
        {
            ValueExpression ve = this.binding.getValueExpression(ctx, Object.class);
            
            c = app.createComponent(ve, faces, this.componentType);
            if (c != null)
            {
                c.setValueExpression("binding", ve);
            }
        }
        else
        {
            c = app.createComponent(this.componentType);
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
        if (this.id != null)
        {
            return this.id.getValue(ctx);
        }
        return ctx.generateUniqueId(this.tagId);
    }

    @Override
    protected MetaRuleset createMetaRuleset(Class type)
    {
        /*MetaRuleset m = super.createMetaRuleset(type);

        // ignore standard component attributes
        m.ignore("binding").ignore("id");

        // add auto wiring for attributes
        m.addRule(ComponentRule.Instance);

        // if it's an ActionSource
        if (ActionSource.class.isAssignableFrom(type))
        {
            m.addRule(ActionSourceRule.Instance);
        }

        // if it's a ValueHolder
        if (ValueHolder.class.isAssignableFrom(type))
        {
            m.addRule(ValueHolderRule.Instance);

            // if it's an EditableValueHolder
            if (EditableValueHolder.class.isAssignableFrom(type))
            {
                m.ignore("submittedValue");
                m.ignore("valid");
                m.addRule(EditableValueHolderRule.Instance);
            }
        }

        return m;*/
        
        // FIXME: Implement correctly
        // temporally restore code
        MetaRuleset m = new MetaRulesetImpl(this.tag, type);
        // ignore standard component attributes
        m.ignore("binding").ignore("id");

        // add auto wiring for attributes
        m.addRule(ComponentRule.INSTANCE);

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
        
        return m;
    }

    /**
     * A hook method for allowing developers to do additional processing once Facelets creates the component. The
     * 'setAttributes' method is still perferred, but this method will provide the parent UIComponent before it's been
     * added to the tree and before any children have been added to the newly created UIComponent.
     * 
     * @param ctx
     * @param c
     * @param parent
     */
    protected void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent)
    {
        // do nothing
    }

    protected void onComponentPopulated(FaceletContext ctx, UIComponent c, UIComponent parent)
    {
        // do nothing
    }

    protected void applyNextHandler(FaceletContext ctx, UIComponent c) throws IOException, FacesException, ELException
    {
        // first allow c to get populated
        this.nextHandler.apply(ctx, c);
    }
}
