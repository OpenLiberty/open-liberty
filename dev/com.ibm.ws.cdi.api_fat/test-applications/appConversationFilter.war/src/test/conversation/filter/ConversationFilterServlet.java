/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
public class ConversationFilterServlet extends HttpServlet {

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

        System.out.println("ConversationFilterServlet op:" + op);

        if (OP_BEGIN.equals(op)) {
            conversation.begin();
            bean.testIt();
            String id = conversation.getId();
            System.out.println("ConversationFilterServlet New ID:" + id);
            resp.getWriter().write(id);
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
