package com.ibm.ws.cdi12.test.web1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.shared.NonInjectedHello;

@WebServlet("/noinjection")
public class NoInjectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final NonInjectedHello foo = new NonInjectedHello();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        out.println("Hello from shared library class? :" + foo.areYouThere("Iain"));
    }
}
