package com.ibm.ws.cdi12.test.implicitEJB.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.implicitEJB.InjectedBean1;

@WebServlet("/remove")
public class RemoveServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private InjectedBean1 bean1;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        bean1.removeEJB();

        out.println("EJB Removed!");
    }
}