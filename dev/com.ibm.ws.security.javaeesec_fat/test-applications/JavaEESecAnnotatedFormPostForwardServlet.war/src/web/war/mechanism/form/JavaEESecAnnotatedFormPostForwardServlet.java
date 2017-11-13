//

package web.war.mechanisms.form;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=true, useForwardToLoginExpression="#{elForward}"))
@LdapIdentityStoreDefinition(bindDn="cn=root", bindDnPassword="security")
public class JavaEESecAnnotatedFormPostForwardServlet extends HttpServlet {

    /**
      */
    public void init(ServletConfig config) throws ServletException
    {
       super.init(config);
    }

    /**
     * Send get request to processRequest to do the work.
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *            indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some
     *            sort has occurred.
     */ 
    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws ServletException, IOException 
    {
        processRequest( req, res );
    }
  
    /**
     * Send post request to processRequest to do the work.
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *            indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some
     *            sort has occurred.
     */ 
    public void doPost( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException 
    {
        processRequest( req, res );
    }
  
    /**
     * Determine the type of address book operation requested and call the
     * appropriate helper routine.
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *            indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some 
     *            sort has occurred.
     */ 
    void processRequest( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException 
    {
        String op = req.getParameter("operation");
        if (op != null && op.equals("Add")) {
            // Get details for new Address Book Entry.
            String fName = req.getParameter("firstName");
            String lName = req.getParameter("lastName");
            String eMail = req.getParameter("eMailAddr");
            String phone = req.getParameter("phoneNum");
            PrintWriter out = res.getWriter();
            out.println("Hi " + req.getRemoteUser());
            out.println("firstName : " + fName);
            out.println(", lastName : "  + lName);
            out.println(", eMailAddr : " + eMail);
            out.println(", phoneNum : "  + phone);
        } else {
            // do nothing.
            PrintWriter out = res.getWriter();
            out.println("Hi " + req.getRemoteUser());
            out.println(", opeation : " + op); 
        }
    }
}

