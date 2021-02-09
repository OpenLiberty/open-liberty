/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.defaultdecorator;

import java.io.Serializable;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.Conversation;
import javax.inject.Inject;

@Decorator
public abstract class ConversationDecorator implements Conversation, Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    @Delegate
    Conversation c;

    @Override
    public boolean isTransient() {
        DefaultDecoratorServlet.setOutput("decorating");
        return c.isTransient();
    }

}