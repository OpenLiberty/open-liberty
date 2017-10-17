/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
        DefaultDecoratorServlet.addOutput("decorating");
        return c.isTransient();
    }

}