package com.ibm.samples.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import com.ibm.samples.jaxws2.SayHello;
import com.ibm.samples.jaxws2.SayHelloServiceOne;
import com.ibm.samples.jaxws2.SayHelloServiceThree;
import com.ibm.samples.jaxws2.SayHelloServiceTwo;

/**
 * Servlet implementation class SayHelloServlet
 */
@WebServlet("/SayHelloServlet")
public class SayHelloServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @WebServiceRef
    SayHelloServiceOne sayHelloServiceOne;

    @WebServiceRef
    SayHelloServiceTwo sayHelloServiceTwo;

    @WebServiceRef
    SayHelloServiceThree sayHelloServiceThree;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SayHelloServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String serviceName = request.getParameter("service");
        String warName = request.getParameter("war");
        Service service = null;

        if (username == null || username.isEmpty()
            || password == null || password.isEmpty()
            || serviceName == null || serviceName.isEmpty()) {
            writer.println("Parameters named username, password, service and war are all required.");
            writer.flush();
            writer.close();
            return;
        }

        if (serviceName.equals("SayHelloServiceOne")) {
            service = this.sayHelloServiceOne;
        } else if (serviceName.equals("SayHelloServiceTwo")) {
            service = this.sayHelloServiceTwo;
        } else if (serviceName.equals("SayHelloServiceThree")) {
            service = this.sayHelloServiceThree;
        } else {
            writer.println("Not supported service: " + serviceName);
            writer.flush();
            writer.close();
            return;
        }

        try {
            SayHello sayHelloPort = this.getAndConfigPort(warName, service, username, password, request);
            writer.println(sayHelloPort.sayHello(username));
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("Exception occurs: " + e.toString());
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private SayHello getAndConfigPort(String warName, Service service, String username, String password, HttpServletRequest request) {
        SayHello sayHelloPort = null;
        String path = null;
        if (service instanceof SayHelloServiceOne) {
            sayHelloPort = ((SayHelloServiceOne) service).getSayHelloImplOnePort();
            path = "/" + warName + "/SayHelloServiceOne";
        } else if (service instanceof SayHelloServiceTwo) {
            sayHelloPort = ((SayHelloServiceTwo) service).getSayHelloImplTwoPort();
            path = "/" + warName + "/SayHelloServiceTwo";
        } else {
            sayHelloPort = ((SayHelloServiceThree) service).getSayHelloImplThreePort();
            path = "/" + warName + "/SayHelloServiceThree";
        }

        String host = request.getLocalAddr();
        int port = request.getLocalPort();

        BindingProvider bindProvider = (BindingProvider) sayHelloPort;
        Map<String, Object> reqCtx = bindProvider.getRequestContext();

        reqCtx.put(BindingProvider.USERNAME_PROPERTY, username);
        reqCtx.put(BindingProvider.PASSWORD_PROPERTY, password);
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + path);

        return sayHelloPort;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

}
