/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.api.fat.apps.conversationfilter;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

@RequestScoped
public class ConversationActiveState {

    @Inject
    private BeanManager beanManager;

    private boolean active;

    public void checkActive() {
        try {
            active = beanManager.getContext(ConversationScoped.class).isActive();
        } catch (ContextNotActiveException expected) {
            System.out.println("Context Not Active");
            active = false;
        }
    }

    public boolean getActive() {
        return active;
    }

}
