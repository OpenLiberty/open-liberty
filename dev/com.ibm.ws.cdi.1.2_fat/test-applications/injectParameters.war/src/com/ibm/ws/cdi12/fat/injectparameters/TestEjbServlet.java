package com.ibm.ws.cdi12.fat.injectparameters;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/TestEjb")
public class TestEjbServlet extends HttpServlet {

    @EJB
    TestEjb ejb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.getOutputStream().println(TestUtils.join(ejb.getResult()));
    }

}
