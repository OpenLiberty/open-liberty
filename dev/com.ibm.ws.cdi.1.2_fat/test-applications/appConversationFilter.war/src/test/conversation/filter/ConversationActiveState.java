/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package test.conversation.filter;

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
            active = false;
        }
    }

    public boolean getActive() {
        return active;
    }

}
