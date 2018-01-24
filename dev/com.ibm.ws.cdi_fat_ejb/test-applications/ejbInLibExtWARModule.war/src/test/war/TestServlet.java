package test.war;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.ejb.EJB1;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/")
public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		PrintWriter pw = response.getWriter();
		String jndiName = "java:global/ejbInLibExt.ear/ejbInLibExtEJBModule/EJB1Impl!test.ejb.EJB1";

		try{
			InitialContext ctx = new InitialContext();
			EJB1 ejb1 = (EJB1) ctx.lookup(jndiName);
			
			String data = ejb1.getData();
			pw.println(data);
		}
		catch(Throwable e){
			e.printStackTrace(pw);
		}
		
	}


}
