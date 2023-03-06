/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.corba.web.war;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;

@Stateful
@LocalBean
public class MyIIOPClientBean extends MyIIOPClientServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public ORB getORB() throws NamingException {
        return ((ORB) new InitialContext().lookup("java:comp/ORB"));
    }
}
