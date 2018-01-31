package web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AutoServlet extends HttpServlet {
    public static final String AutoMessage = "This is AutoServlet.";

    /**
     * A simple servlet that when it received a request it simply outputs the message
     * as defined by the static field.
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println(AutoMessage);
        writer.flush();
        writer.close();
    }
}
