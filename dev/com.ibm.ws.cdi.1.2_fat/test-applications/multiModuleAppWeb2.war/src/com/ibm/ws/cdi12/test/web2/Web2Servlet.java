package com.ibm.ws.cdi12.test.web2;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.lib2.BasicBean2;

@WebServlet("/")
public class Web2Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private BasicBean2 bean2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        bean2.setData("Test");

        out.println("Test Sucessful!");
    }
}
