package io.openliberty.cdi40.internal.utils;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.FacesContext;

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
