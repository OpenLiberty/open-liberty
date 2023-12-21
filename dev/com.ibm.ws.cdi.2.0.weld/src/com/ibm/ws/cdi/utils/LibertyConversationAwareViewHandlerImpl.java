/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.utils;

import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;

import org.jboss.weld.Container;
import org.jboss.weld.module.jsf.ConversationAwareViewHandler;

import com.ibm.ws.cdi.LibertyConversationAwareViewHandler;

public class LibertyConversationAwareViewHandlerImpl extends ConversationAwareViewHandler implements LibertyConversationAwareViewHandler {

    private final String contextId;

    public LibertyConversationAwareViewHandlerImpl(ViewHandler viewHandler, String contextId) {
        super(viewHandler);
        this.contextId = contextId;
    }

    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        facesContext.getAttributes().put(Container.CONTEXT_ID_KEY, contextId);
        return super.getActionURL(facesContext, viewId);
    }
}
