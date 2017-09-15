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
package org.apache.myfaces.view.facelets;

import java.io.IOException;
import java.util.Iterator;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;


/**
 * This class contains methods that belongs to original FaceletContext shipped in
 * facelets code before 2.0, but does not take part from api, so are considered 
 * implementation details. This includes methods related to template handling
 * feature of facelets (called by ui:composition, ui:define and ui:insert).
 * 
 * The methods here are only used by the current implementation and the intention
 * is not expose it as public api.
 * 
 * Aditionally, it also contains methods used by the current implementation for
 * implement new features, like composite components and UniqueIdVendor support.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1576792 $ $Date: 2014-03-12 15:57:38 +0000 (Wed, 12 Mar 2014) $
 * 
 * @since 2.0
 */
public abstract class AbstractFaceletContext extends FaceletContext
{    
    /**
     * Return the current FaceletCompositionContext instance from this
     * build.
     * 
     * @return the current FaceletCompositionContext instance
     */
    public abstract FaceletCompositionContext getFaceletCompositionContext();
    
    /**
     * Push the passed TemplateClient onto the stack for Definition Resolution
     * @param client
     * @see TemplateClient
     */
    public abstract void pushClient(TemplateClient client);

    /**
     * Pop the last added pushed TemplateClient
     * @see TemplateClient
     */
    public abstract TemplateManager popClient(TemplateClient client);
    
    /**
     * Pop the last added extended TemplateClient
     * @param client
     */
    public abstract TemplateManager popExtendedClient(TemplateClient client);

    public abstract void extendClient(TemplateClient client);

    /**
     * This method will walk through the TemplateClient stack to resolve and
     * apply the definition for the passed name.
     * If it's been resolved and applied, this method will return true.
     * 
     * @param parent the UIComponent to apply to
     * @param name name or null of the definition you want to apply
     * @return true if successfully applied, otherwise false
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract boolean includeDefinition(UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException;
    
    /**
     * Apply the facelet referenced by a url containing a composite component
     * definition to the current UIComponent. In other words, apply the section
     * composite:implementation in the facelet to the current component.
     * 
     * We need to do this here because DefaultFacelet is the one who has and
     * handle the current FaceletFactory instance.
     * 
     * @param parent
     * @param url
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract void applyCompositeComponent(UIComponent parent, Resource resource)
            throws IOException, FaceletException, FacesException, ELException;

    /**
     * Return a descending iterator containing the ajax handlers to be applied
     * to an specific component that implements ClientBehaviorHolder interface,
     * according to the conditions specified on jsf 2.0 spec section 10.4.1.1.
     * 
     * @since 2.0
     */
    public abstract Iterator<AjaxHandler> getAjaxHandlers();
    
    /**
     * @since 2.0
     */
    public abstract void popAjaxHandlerToStack();
    
    /**
     * @since 2.0
     */
    public abstract void pushAjaxHandlerToStack(AjaxHandler parent);
    
    /**
     * Check if this build is for build composite component metadata
     * 
     * @return
     * @since 2.0
     */
    public abstract boolean isBuildingCompositeComponentMetadata();

    /**
     * Pop the current composite component template client, removing the
     * current template context from stack.
     * 
     * @since 2.0.1
     */
    public abstract void popCompositeComponentClient();

    /**
     * Push the composite component tag handler identified by client on 
     * template context stack, triggering the creation of a new empty 
     * TemplateContext, that will be used to resolve templates used 
     * on that component later.
     * 
     * @since 2.0.1
     */
    public abstract void pushCompositeComponentClient(final TemplateClient client);

    /**
     * Push the passed template context instance onto the stack, so all
     * slots to be resolved (using includeDefinition() call) will take
     * into account the information there.
     * 
     * @since 2.0.1
     */
    public abstract void pushTemplateContext(TemplateContext templateContext);

    /**
     * Pop the passed template context instance from stack. This method is
     * used by CompositeComponentResourceTagHandler to resolve templates
     * according to the composite component level it is necessary.
     * 
     * @since 2.0.1
     */
    public abstract TemplateContext popTemplateContext();

    /**
     * This method resolve the current definition to be added by cc:insertChildren
     * or cc:insertFacet.
     *  
     * @since 2.0.1
     */
    public abstract boolean includeCompositeComponentDefinition(UIComponent parent, String name)
        throws IOException, FaceletException, FacesException, ELException;
    
    /**
     * Return the current template context
     * 
     * @since 2.0.8
     * @return
     */
    public TemplateContext getTemplateContext()
    {
        return null;
    }

    /**
     * Push the passed page context instance onto the stack.
     * 
     * @since 2.0.8
     * @param client
     */
    public void pushPageContext(PageContext client)
    {
    }
    
    /**
     * Pop the passed page context instance onto the stack.
     * 
     * @since 2.0.8
     * @param client
     */
    public PageContext popPageContext()
    {
        return null;
    }
    
    /**
     * Return the current page context
     * 
     * @since 2.0.8
     * @return
     */
    public PageContext getPageContext()
    {
        return null;
    }
    
    /**
     * Check if a variable has been resolved by this variable mapper
     * or any parent "facelets contextual" variable mapper.
     * 
     * @return
     * @since 2.0.8
     */
    public boolean isAnyFaceletsVariableResolved()
    {
        return true;
    }
    
    public boolean isAllowCacheELExpressions()
    {
        return false;
    }
    
    /**
     * Indicates an expression will be resolved, so preparations
     * should be done to detect if a contextual variable has been resolved.
     * 
     * @since 2.0.8
     */
    public void beforeConstructELExpression()
    {
    }
    
    /**
     * Cleanup all initialization done for construct an EL Expression.
     * 
     * @since 2.0.8
     */
    public void afterConstructELExpression()
    {
    }

    /**
     * Return the mode used to decide whether to cache or not EL expressions
     * 
     * @since 2.0.8
     */
    public ELExpressionCacheMode getELExpressionCacheMode()
    {
        return ELExpressionCacheMode.noCache;
    }
    
    public String generateUniqueFaceletTagId(String count, String base)
    {
        return count;
    }
}
