package com.ibm.samples.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.samples.jaxws.SayHello;
import com.ibm.samples.jaxws.SayHelloService;
import com.ibm.samples.jaxws.SecuredSayHelloService;

/**
 * Servlet implementation class SayHelloServlet
 */
@WebServlet("/SayHelloServlet")
public class SayHelloServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@WebServiceRef()
	SayHelloService sayHelloService;

	@WebServiceRef()
	SecuredSayHelloService securedSayHelloService;

	private static String request1 = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
			+ "  <soap:Body>\n" + "	<ns2:sayHello xmlns:ns2=\"http://jaxws.samples.ibm.com\">\n"
			+ "		<name>user1</name>\n" + "	</ns2:sayHello>\n" + "  </soap:Body>\n" + "</soap:Envelope>";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SayHelloServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		String serviceName = request.getParameter("service");
		String warName = request.getParameter("war");
		Service service = null;

		if (username == null || username.isEmpty() || password == null || password.isEmpty() || serviceName == null
				|| serviceName.isEmpty()) {
			writer.println("Parameters named username, password, service and war are all required.");
			writer.flush();
			writer.close();
			return;
		}

		if (serviceName.equals("SayHelloService")) {
			// service = Service.create(new URL(wsdlUrl), new
			// QName("http://jaxws.samples.ibm.com", "SayHelloService"));
			service = this.sayHelloService;
		} else if (serviceName.equals("SecuredSayHelloService")) {
			service = this.securedSayHelloService;
		} else {
			writer.println("Not supported service: " + serviceName);
			writer.flush();
			writer.close();
			return;
		}

		try {
			SayHello sayHelloPort = this.getAndConfigPort(warName, service, username, password, request);
			System.out.println("warName, " + warName + " service, " + service + " username, " + username + " password, "
					+ password + " request " + request);

			writer.println(sayHelloPort.sayHello(username));
		} catch (Exception e) {
			e.printStackTrace();
			writer.println("Exception occurs: " + e.toString());
		} finally {
			writer.flush();
			writer.close();
		}
	}

	private SayHello getAndConfigPort(String warName, Service service, String username, String password,
			HttpServletRequest request) {
		SayHello sayHelloPort = null;
		String path = null;
		if (service instanceof SecuredSayHelloService) {
			sayHelloPort = ((SecuredSayHelloService) service).getSayHelloStalelessPort();
			path = "/" + warName + "/SecuredSayHelloService";
		} else {
			sayHelloPort = ((SayHelloService) service).getSayHelloStalelessPort();
			path = "/" + warName + "/SayHelloService";
		}

		String wsdlPath = path + "?wsdl";
		String host = request.getLocalAddr();
		int port = request.getLocalPort();
		BindingProvider bindProvider = (BindingProvider) sayHelloPort;
		Map<String, Object> reqCtx = bindProvider.getRequestContext();

		reqCtx.put(BindingProvider.USERNAME_PROPERTY, username);
		reqCtx.put(BindingProvider.PASSWORD_PROPERTY, password);

		reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + path);

		return sayHelloPort;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}

}
