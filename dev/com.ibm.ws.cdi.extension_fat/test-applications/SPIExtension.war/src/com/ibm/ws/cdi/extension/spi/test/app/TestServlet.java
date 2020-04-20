package com.ibm.ws.cdi.extension.spi.test.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.extension.spi.test.bundle.extension.MyExtensionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.MyClassString;

@WebServlet("/")
public class TestServlet extends HttpServlet {

    @Inject
    MyExtensionString extensionString;

    @Inject
    MyClassString classString;

    @Inject
    DummyBean db;

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        db.iExist();

        PrintWriter pw = response.getWriter();
        pw.println("Test Results:");
        pw.println(extensionString.toString());
        pw.println(classString.toString());

    }
}
