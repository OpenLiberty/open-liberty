package batch.fat.web;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns = { "/ServerKillerServlet" })
public class ServerKillerServlet extends HttpServlet{

	private final static Logger logger = Logger.getLogger(ServerKillerServlet.class.getName());
	
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response){
		logger.info("Killing the server");
		Runtime.getRuntime().halt(9);
	}
	
	
}
