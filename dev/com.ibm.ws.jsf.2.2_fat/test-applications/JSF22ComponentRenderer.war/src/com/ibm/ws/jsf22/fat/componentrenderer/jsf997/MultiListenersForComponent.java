/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.componentrenderer.jsf997;

import java.util.Map;

import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;

@FacesComponent(value = "MultiListenersForComponent")
@ListenersFor({
               @ListenerFor(systemEventClass = PreValidateEvent.class),
               @ListenerFor(systemEventClass = PostValidateEvent.class)
})
public class MultiListenersForComponent extends HtmlInputText {

    /**
     * This code is testing a change to the JSF infrastructure that validates that listeners of
     * ComponentSystemEvents implements ComponentSystemEventListener. If this code executes
     * correctly then the validation via ComponentSystemEvent.isAppropriateListener() returned true.
     * The code here is testing multiple listeners.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        if (event instanceof PreValidateEvent) {
            requestMap.put("preValidateEvent", "preValidateEvent");
        } else if (event instanceof PostValidateEvent) {
            requestMap.put("postValidateEvent", "postValidateEvent");
        } else {
            super.processEvent(event);
        }
    }
}
