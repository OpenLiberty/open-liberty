package cdi12.helloworld.extension.test;

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

/**
 *
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class HelloWorldExtensionTestServlet extends HttpServlet {

    @Inject
    HelloWorldExtensionBean hello;
    private static EventMetadata beanStartMetaData;

    private static EventMetadata beanStopMetaData;

    private static final long serialVersionUID = 8549700799591343964L;

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {

        if (beanStartMetaData == null) {

            System.out.println("Initialize Event request scope is happening");

            beanStartMetaData = em;

        }

    }

    public static void onStop(@Observes @Destroyed(RequestScoped.class) Object e, EventMetadata em) {

        if (beanStopMetaData == null) {

            System.out.println("Stop Event request scope is happening");

            beanStopMetaData = em;

        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.write(beanStartMetaData.getQualifiers().toString());
        pw.flush();
        pw.close();
    }

}
