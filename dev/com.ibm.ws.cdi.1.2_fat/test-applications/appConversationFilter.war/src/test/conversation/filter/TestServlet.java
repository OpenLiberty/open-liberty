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

import java.io.IOException;

import javax.enterprise.context.Conversation;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/test")
public class TestServlet extends HttpServlet {

    public static final String OP_BEGIN = "begin";

    public static final String OP_STATUS = "status";

    @Inject
    ConversationBean bean;

    @Inject
    Conversation conversation;

    @Inject
    ConversationActiveState conversationState;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/plain");
        String op = req.getParameter("op");

        if (OP_BEGIN.equals(op)) {
            conversation.begin();
            bean.testIt();
            resp.getWriter().write(conversation.getId());
        } else if (OP_STATUS.equals(op)) {
            if (conversation.isTransient()) {
                resp.sendError(500, "No long running conversation");
            } else {
                resp.getWriter().write(String.valueOf(conversationState.getActive()));
            }
        } else {
            throw new ServletException("Unknown operation: " + op);
        }
    }

}
