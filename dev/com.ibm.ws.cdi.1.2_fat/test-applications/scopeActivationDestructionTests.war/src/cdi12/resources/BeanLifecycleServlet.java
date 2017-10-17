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
package cdi12.resources;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi12.scopedclasses.ConversationScopedBean;
import cdi12.scopedclasses.RequestScopedBean;
import cdi12.scopedclasses.SessionScopedBean;

/**
 *
 */
@WebServlet("/BeanLifecycle")
public class BeanLifecycleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    ConversationScopedBean csb;

    @Inject
    RequestScopedBean rsb;

    @Inject
    SessionScopedBean ssb;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();

        csb.doSomething();
        rsb.doSomething();
        ssb.doSomething();

        pw.print(GlobalState.buildString());

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] keys = request.getParameterMap().get("key");
        String[] values = request.getParameterMap().get("value");

        String key = "";
        String value = "";

        if (keys != null) {
            key = keys[0];
        }
        if (values != null) {
            value = values[0];
        }

        System.out.println("MYTEST - got message key:" + key + "value:" + value);

        //Don't mind me is to prevent issues if the second servlet is poked into sending multiple messages
        //independent of it catching start up and shut down events.
        if (key.equals("Applicaiton Scoped Bean") && !value.equals("Don't mind me") &&
            (value.equals("START") || value.equals("STOP"))) {

            Move m = Move.valueOf(value);

            if (m == Move.START) {
                GlobalState.recordApplicationStart();
            } else {
                GlobalState.recordApplicationStop();
            }

        }

        PrintWriter pw = response.getWriter();
        pw.write("recieved :" + key + " " + value);
    }
}
