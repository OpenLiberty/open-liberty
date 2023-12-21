/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi;

import java.util.List;
import java.util.Map;

import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;

/**
 * This interface allows us to reference
 * https://github.com/weld/core/blob/master/modules/jsf/src/main/java/org/jboss/weld/module/jsf/ConversationAwareViewHandler.java
 * outside of the CDI bundles and in a CDI version neutral manner.
 */
public interface LibertyConversationAwareViewHandler {

    public String getActionURL(FacesContext facesContext, String viewId);

    public String getBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters,
                                     boolean includeViewParams);

    public String getRedirectURL(FacesContext context, String viewId, Map<String, List<String>> parameters,
                                 boolean includeViewParams);

    public String getResourceURL(FacesContext context, String path);

    public ViewHandler getWrapped();
}
