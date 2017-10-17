package com.ibm.ws.cdi12.fat.injectparameters;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/TestCdiBean")
public class TestCdiBeanServlet extends HttpServlet {

    @Inject
    private TestCdiBean testCdiBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.getOutputStream().println(TestUtils.join(testCdiBean.getResult()));
    }

}
