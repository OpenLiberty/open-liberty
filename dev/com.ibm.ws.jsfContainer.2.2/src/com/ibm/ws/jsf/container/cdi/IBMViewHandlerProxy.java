/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.cdi;

import java.util.List;
import java.util.Map;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.context.ConversationContext;
import org.jboss.weld.context.http.HttpConversationContext;
import org.jboss.weld.jsf.FacesUrlTransformer;

// TODO: This class is mainly a duplicate of the Weld class org.jboss.weld.jsf.ConversationAwareViewHandler
// except for one line of code.  Instead of duplicating the class, we should pull the CAVH
// class from weld at build-time and copy it here.  Then we can write a class that extends the CAVH.
// This will allow us to automatically pick up any changes that are delivered into new versions of weld's CAVH
public class IBMViewHandlerProxy extends ViewHandlerWrapper {

    private static enum Source {
        ACTION,
        BOOKMARKABLE,
        REDIRECT,
        RESOURCE
    }

    private final String contextID;
    private final ViewHandler delegate;
    private volatile ConversationContext conversationContext;
    private static final ThreadLocal<Source> source = new ThreadLocal<Source>();
    private String contextId;

    public IBMViewHandlerProxy(ViewHandler delegate, String contextID) {
        this.delegate = delegate;
        this.contextID = contextID;
    }

    /**
     * Get conversation context.
     *
     * @return the conversation context
     */
    private ConversationContext getConversationContext(String id) {
        if (conversationContext == null) {
            synchronized (this) {
                if (conversationContext == null) {
                    Container container = Container.instance(id);
                    conversationContext = container.deploymentManager().instance().select(HttpConversationContext.class).get();
                }
            }
        }
        return conversationContext;
    }

    /**
     * Allow the delegate to produce the action URL. If the conversation is
     * long-running, append the conversation id request parameter to the query
     * string part of the URL, but only if the request parameter is not already
     * present.
     * <p/>
     * This covers form actions Ajax calls, and redirect URLs (which we want)
     * and link hrefs (which we don't)
     *
     * @see {@link ViewHandler#getActionURL(FacesContext, String)}
     */
    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        // This line is the only piece of Liberty-specific code
        facesContext.getAttributes().put(Container.CONTEXT_ID_KEY, contextID);
        //return super.getActionURL(facesContext, viewId);

        if (contextId == null) {
            if (facesContext.getAttributes().containsKey(Container.CONTEXT_ID_KEY)) {
                contextId = (String) facesContext.getAttributes().get(Container.CONTEXT_ID_KEY);
            } else {
                contextId = RegistrySingletonProvider.STATIC_INSTANCE;
            }
        }
        String actionUrl = super.getActionURL(facesContext, viewId);
        final ConversationContext ctx = getConversationContext(contextId);
        if (ctx.isActive() && !getSource().equals(Source.BOOKMARKABLE) && !ctx.getCurrentConversation().isTransient()) {
            return new FacesUrlTransformer(actionUrl, facesContext)
                            .appendConversationIdIfNecessary(getConversationContext(contextId).getParameterName(), ctx.getCurrentConversation().getId())
                            .getUrl();
        } else {
            return actionUrl;
        }
    }

    private Source getSource() {
        if (source.get() == null) {
            return Source.ACTION;
        } else {
            return source.get();
        }
    }

    @Override
    public String getBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
        try {
            source.set(Source.BOOKMARKABLE);
            return super.getBookmarkableURL(context, viewId, parameters, includeViewParams);
        } finally {
            source.remove();
        }
    }

    @Override
    public String getRedirectURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
        try {
            source.set(Source.REDIRECT);
            return super.getRedirectURL(context, viewId, parameters, includeViewParams);
        } finally {
            source.remove();
        }
    }

    @Override
    public String getResourceURL(FacesContext context, String path) {
        try {
            source.set(Source.RESOURCE);
            return super.getResourceURL(context, path);
        } finally {
            source.remove();
        }
    }

    @Override
    public ViewHandler getWrapped() {
        return delegate;
    }
}
