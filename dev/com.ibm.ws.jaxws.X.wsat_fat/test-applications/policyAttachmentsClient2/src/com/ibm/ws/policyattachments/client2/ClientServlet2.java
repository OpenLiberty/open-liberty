package com.ibm.ws.policyattachments.client2;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import com.ibm.ws.policyattachments.client2.service1.HelloService1PortProxy;
import com.ibm.ws.policyattachments.client2.service2.HelloService2PortProxy;

/**
 * Servlet implementation class ClientServlet
 */
@WebServlet("/ClientServlet2")
public class ClientServlet2 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ClientServlet2() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		PrintWriter writer = response.getWriter();

		Enumeration<?> enu = request.getParameterNames();
		while (enu.hasMoreElements()) {
			String paraName = (String) enu.nextElement();
			System.out.println("Get parameter: " + paraName + " - "
					+ request.getParameter(paraName));
		}

		String appName = request.getParameter("app");
		String methodName = request.getParameter("method");
		
		String result = "";
		if (appName.equals("policyAttachmentsService1")) {
			try {
				String service = "http://localhost:8091/policyAttachmentsService1/HelloService1?wsdl";
				QName serviceName = new QName(
						"http://service1.policyattachments.ws.ibm.com/",
						"HelloService1");
				HelloService1PortProxy proxy = new HelloService1PortProxy(
						new URL(service), serviceName);
				if(methodName.equals("helloWithoutPolicy")){
					result = proxy.helloWithoutPolicy();
				} else if(methodName.equals("helloWithPolicy")){
					result = proxy.helloWithPolicy();
				} else if(methodName.equals("helloWithOptionalPolicy")){
					result = proxy.helloWithOptionalPolicy();
				} else if(methodName.equals("helloWithYouWant")){
					result = proxy.helloWithYouWant();
				}
				writer.println(result);
				System.out.println(result);
			} catch (Exception e) {
				writer.println(e.getMessage());
			}
		} else if (appName.equals("policyAttachmentsService2")) {
			try {
				String service = "http://localhost:8091/policyAttachmentsService2/HelloService2?wsdl";
				QName serviceName = new QName(
						"http://service2.policyattachments.ws.ibm.com/",
						"HelloService2");
				HelloService2PortProxy proxy = new HelloService2PortProxy(
						new URL(service), serviceName);
				if(methodName.equals("helloWithoutPolicy")){
					result = proxy.helloWithoutPolicy();
				} else if(methodName.equals("helloWithPolicy")){
					result = proxy.helloWithPolicy();
				} else if(methodName.equals("helloWithOptionalPolicy")){
					result = proxy.helloWithOptionalPolicy();
				} else if(methodName.equals("helloWithYouWant")){
					result = proxy.helloWithYouWant();
				}
				writer.println(result);
				System.out.println(result);
			} catch (Exception e) {
				writer.println(e.getMessage());
			}
		} else {
			writer.println("No method invoked");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
