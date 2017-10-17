package servlets;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import client.services.SayHello;
import client.services.SayHelloPojoService;

@WebServlet("/TestWebServicesServlet")
@SuppressWarnings("serial")
public class TestWebServicesServlet extends HttpServlet {
    private static final String PROVIDER_CONTEXT_ROOT = "/resourceWebServicesProvider";

    private static final QName POJO_PORT_QNAME = new QName("http://ibm.com/ws/jaxws/cdi/", "SayHelloPojoPort");

    @WebServiceRef(name = "service/SayHelloPojoService")
    SayHelloPojoService pojoService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        String userName = req.getParameter("user");
        if (userName == null) {
            userName = "Bobby";
        }

        System.out.println("The test case is: " + "cdi injection + webservices");
        Writer out = null;
        try {
            String resultString = "";
            out = resp.getWriter();
            SayHello sayHelloPort = getAndConfigClient(req, SayHello.class);
            resultString = sayHelloPort.sayHello(userName);

            out.write(resultString);
        } catch (Exception e) {
            out.write(getThrowableMessage(e));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAndConfigClient(HttpServletRequest req, Class<T> clazz) {

        String schema = req.getParameter("schema");
        String host = req.getLocalName();
        String port = req.getParameter("port");
        String requestPath = req.getParameter("path");
        if (schema == null) {
            schema = req.getScheme();
        }
        if (port == null) {
            port = String.valueOf(req.getLocalPort());
        }
        if (requestPath == null) {
            requestPath = "/SayHelloPojoService";
        }

        T client = null;
        client = (T) pojoService.getSayHelloPojoPort();
        BindingProvider provider = (BindingProvider) client;

        StringBuilder sBuilder = new StringBuilder(schema).append("://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(PROVIDER_CONTEXT_ROOT)
                        .append(requestPath);
        String urlPath = sBuilder.toString();
        System.out.println(clazz.getSimpleName() + ": The request web service url is: " + urlPath);
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlPath);

        return client;
    }

    private String createMessage(String userName) {
        StringBuilder sBuilder = new StringBuilder("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">")
                        .append("<soap:Body>")
                        .append("<ns2:sayHello xmlns:ns2=\"http://ibm.com/ws/jaxws/cdi/\">")
                        .append("<arg0>").append(userName).append("</arg0>")
                        .append("</ns2:sayHello>")
                        .append("</soap:Body>")
                        .append("</soap:Envelope>");
        return sBuilder.toString();

    }

    private String getThrowableMessage(Throwable origThrowable) {
        StringBuilder twBuilder = new StringBuilder();

        Throwable tmp = null;
        do {
            twBuilder.append(origThrowable.getMessage())
                            .append("\n");
            tmp = origThrowable;
        } while (null != origThrowable.getCause() && (origThrowable = origThrowable.getCause()) != tmp);

        return twBuilder.substring(0, twBuilder.length() - 1);
    }
}
