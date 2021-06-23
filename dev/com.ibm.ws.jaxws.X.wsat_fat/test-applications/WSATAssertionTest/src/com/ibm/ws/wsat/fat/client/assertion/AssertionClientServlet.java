package com.ibm.ws.wsat.fat.client.assertion;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;

@WebServlet({ "/AssertionClientServlet" })
public class AssertionClientServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        try {
            String BASE_URL = request.getParameter("baseurl");
            if (BASE_URL == null || BASE_URL.equals(""))
                BASE_URL = "http://localhost:8010";
            Hello proxy;
            String endpointAddress = "";
            URL wsdlLocation = new URL(BASE_URL
                                       + "/WSATAssertionTest/HelloImplWSATAssertionService?wsdl");
            HelloImplWSATAssertionService service = new HelloImplWSATAssertionService(
                            wsdlLocation);
            endpointAddress = "HelloImplWSATAssertionService";
            proxy = service.getHelloImplWSATAssertionPort();
            BindingProvider bind = (BindingProvider) proxy;
            bind.getRequestContext().put(
                                         "javax.xml.ws.service.endpoint.address",
                                         BASE_URL + "/WSATAssertionTest/" + endpointAddress);

            System.out.println("Reply from server: " + proxy.sayHello());
            response.getWriter().println(
                                         "<html><header></header><body> Reply from server: "
                                                         + proxy.sayHello() + "</body></html>");

            response.getWriter().flush();
        } catch (Exception e) {
            response.getWriter().println(
                                         "<html><header></header><body> Client catch exception: "
                                                         + e.toString() + "</body></html>");
            e.printStackTrace();
        }

    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {}
}
