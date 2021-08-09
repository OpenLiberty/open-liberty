/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.spec1430;

import java.util.Map;

import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;

/**
 * This is a simple test component that is being used to test that
 * the @ListenerFor and @ResourceDependency annotations are @Repeatable.
 *
 * Rather than having to use @ListenersFor({}) and @ResourceDependencies({})
 *
 */
@FacesComponent(createTag = true, namespace = "test")
@ListenerFor(systemEventClass = PreValidateEvent.class)
@ListenerFor(systemEventClass = PostValidateEvent.class)
@ResourceDependency(library = "css", name = "test-style.css")
@ResourceDependency(library = "css", name = "test-style2.css")
public class TestComponent extends HtmlInputText {

    @Override
    public void processEvent(ComponentSystemEvent event) {
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
