/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War.beans;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War.resources.GlobalState;

/**
 *
 */
@ConversationScoped
public class ConversationScopedBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    private Conversation conversation;

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

    public static void onStart(@Observes @Initialized(ConversationScoped.class) Object e) {
        GlobalState.recordConversatoinStart();
    }

    public static void onStop(@Observes @Destroyed(ConversationScoped.class) Object e) {
        GlobalState.recordConversatoinStop();
    }

}
