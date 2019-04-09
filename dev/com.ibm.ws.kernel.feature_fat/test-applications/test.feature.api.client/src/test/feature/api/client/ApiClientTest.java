/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package test.feature.api.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/apiClient")
public class ApiClientTest extends HttpServlet {
    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Class<?> someAPIClass = Class.forName("test.feature.api.SomeAPI");
            response.getOutputStream().println(getClass().getSimpleName() + ":" + someAPIClass.newInstance().toString());
        } catch (ClassNotFoundException e) {
            response.getOutputStream().println("FAILED");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

}
