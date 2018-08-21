package com.ibm.microprofile.illegal.optional.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@WebServlet("/")
public class MyServlet extends HttpServlet {
    private static final long serialVersionUID = 4450293693761251848L;


    @Inject
    BeanHolder bh;
    
    @SuppressWarnings("unchecked")
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");

        PrintWriter writer = resp.getWriter();
        writer.println("Hi!");
        writer.println(bh.getProp().orElse("No Prop"));
        writer.close();
    }
}
