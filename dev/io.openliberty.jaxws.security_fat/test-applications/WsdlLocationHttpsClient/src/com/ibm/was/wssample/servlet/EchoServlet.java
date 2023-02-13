package com.ibm.was.wssample.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.was.wssample.sei.echo.EchoService_Service;

@WebServlet("/EchoServlet")
public class EchoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String securePort = request.getParameter("securePort");

        // Set WSDL location to HTTPS URL
        EchoService_Service echoS = new EchoService_Service(new URL("https://localhost:" + securePort + "/WsdlLocationHttpsServer/EchoService?wsdl"));
        PrintWriter out = response.getWriter();
        out.println(echoS.getEchoPort().echo("HttpsWsdlLocation"));
    }
}
