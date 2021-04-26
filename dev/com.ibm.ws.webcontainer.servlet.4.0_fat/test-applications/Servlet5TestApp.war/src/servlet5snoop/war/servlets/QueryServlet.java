package servlet5snoop.war.servlets;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.PrintWriter;
import java.io.IOException;

/*
* Tests allowQueryParamWithNoEqual for servlet-5.0 and higher 
*/
public class QueryServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String t = request.getParameter("test");

        if(t != null && t.equals("")){
            out.println("For query 'test', the value is an empty string (expected).");
        } else {
            out.println("ERROR! test = " + t + ".");
        }

    }

}