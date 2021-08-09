/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jsf;

import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;

import org.jboss.weld.Container;
import org.jboss.weld.module.jsf.ConversationAwareViewHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class IBMViewHandler extends ConversationAwareViewHandler {

    private static final TraceComponent tc = Tr.register(IBMViewHandler.class);

    private final String contextID;

    public IBMViewHandler(ViewHandler delegate, String contextID) {
        super(delegate);
        this.contextID = contextID;
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
        facesContext.getAttributes().put(Container.CONTEXT_ID_KEY, contextID);
        return super.getActionURL(facesContext, viewId);
    }
}
