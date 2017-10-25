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
package cdi12.scopedclasses;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import cdi12.resources.GlobalState;

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
