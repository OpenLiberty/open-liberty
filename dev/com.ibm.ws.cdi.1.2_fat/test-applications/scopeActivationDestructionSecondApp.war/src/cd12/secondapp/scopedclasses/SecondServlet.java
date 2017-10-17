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
package cd12.secondapp.scopedclasses;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/SecondServlet")
public class SecondServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    ApplicationScopedBean asb;

    private static String startupMsg = "";
    private static String serverURL = null;
    private final String CHARSET = "UTF-8";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        asb.doSomething();

        serverURL = "http://" + request.getServerName() + ":" + request.getServerPort() + "/PrideRock/BeanLifecycle";

        String query = String.format("key=%s&value=%s",
                                     URLEncoder.encode("Applicaiton Scoped Bean", CHARSET),
                                     URLEncoder.encode(startupMsg, CHARSET));

        URLConnection connection = new URL(serverURL).openConnection();
        connection.setDoOutput(true); // Triggers POST.
        connection.setRequestProperty("Accept-Charset", CHARSET);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(query.getBytes(CHARSET));
        } finally {
            if (output != null) {
                output.flush();
                output.close();
                output = null;
            }
        }

        InputStream responseTwo = connection.getInputStream();

        java.util.Scanner s = new java.util.Scanner(responseTwo).useDelimiter("\\A");
        String responseTwoStr = s.hasNext() ? s.next() : "";

        PrintWriter pw = response.getWriter();
        pw.write("message sent to " + connection.getURL());
        pw.write("Reply: " + responseTwoStr);

        startupMsg = "Don't mind me";
    }

    public void stopApplicationScoped(@Observes @Destroyed(ApplicationScoped.class) Object e) throws MalformedURLException, IOException, NullPointerException {
        String endMsg = "STOP";

        if (serverURL == null) {
            throw (new NullPointerException());
        }

        System.out.println("MYTEST applicaiton stopped attempting to send to " + serverURL);

        String query = String.format("key=%s&value=%s",
                                     URLEncoder.encode("Applicaiton Scoped Bean", CHARSET),
                                     URLEncoder.encode(endMsg, CHARSET));

        URLConnection connection = new URL(serverURL).openConnection();
        connection.setDoOutput(true); // Triggers POST.
        connection.setRequestProperty("Accept-Charset", CHARSET);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(query.getBytes(CHARSET));
        } finally {
            if (output != null) {
                output.flush();
                output.close();
                output = null;
            }
        }

        InputStream responseTwo = connection.getInputStream();

        java.util.Scanner s = new java.util.Scanner(responseTwo).useDelimiter("\\A");
        String responseTwoStr = s.hasNext() ? s.next() : "";

    }

    public void startApplicationScoped(@Observes @Initialized(ApplicationScoped.class) Object e) throws MalformedURLException, IOException, NullPointerException {
        startupMsg = "START";
    }
}
