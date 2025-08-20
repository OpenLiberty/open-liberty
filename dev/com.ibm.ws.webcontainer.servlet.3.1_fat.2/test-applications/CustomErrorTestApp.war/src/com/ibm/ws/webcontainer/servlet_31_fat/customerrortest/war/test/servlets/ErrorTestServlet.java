package com.ibm.ws.webcontainer.servlet_31_fat.customerrortest.war.test.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ErrorTestServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Return a 500 Internal Server Error
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       "Intentional failure to trigger 500");
    }
}
