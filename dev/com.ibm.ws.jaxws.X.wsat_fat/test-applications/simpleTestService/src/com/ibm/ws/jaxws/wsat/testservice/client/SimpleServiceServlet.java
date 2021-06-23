package com.ibm.ws.jaxws.wsat.testservice.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

@WebServlet("/SimpleServiceServlet")
public class SimpleServiceServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8305921017784778199L;

	private static final String NAMESPACE = "http://impl.testservice.wsat.jaxws.ws.ibm.com/";

	private String hostname;

	private String port;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		port = request.getParameter("port");
		hostname = request.getParameter("hostname");
		String serviceName = request.getParameter("service");
		String warName = request.getParameter("war").replace(".war", "");

		if (hostname == null || hostname.isEmpty() || port == null
				|| port.isEmpty() || warName == null || warName.isEmpty()
				|| serviceName == null || serviceName.isEmpty()) {
			writer.println("Parameters named port, hostname, service and war are all required.");
			writer.flush();
			writer.close();
			return;
		}

		if (serviceName.equals("SimpleImplService")) {
			invokeSimpleImplService(warName, serviceName, writer);
		} else if (serviceName.equals("SimpleEcho")) {
			invokeSimpleEchoService(warName, serviceName, writer);
		} else {
			writer.println("Not supported service: " + serviceName);
			writer.flush();
			writer.close();
		}
	}

	private void invokeSimpleEchoService(String warName, String serviceName,
			PrintWriter writer) {
		String echoParameter = "Hello";
		String requestMessageValue = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:echo xmlns:ns2=\"http://simpleEchoProvider.impl.testservice.wsat.jaxws.ws.ibm.com/\"><arg0>${placeHolder}</arg0></ns2:echo></soap:Body></soap:Envelope>"
				.replace("${placeHolder}", echoParameter);
		Source requestMessageSource = new StreamSource(new StringReader(
				requestMessageValue));
		SOAPMessage requestMessage = null;
		try {
			MessageFactory factory = MessageFactory.newInstance();
			requestMessage = factory.createMessage();
			requestMessage.getSOAPPart().setContent(requestMessageSource);
			requestMessage.saveChanges();

			Service service = Service.create(new QName("http://abc", "abc"));
			service.addPort(
					new QName("http://abc", "anyPort"),
					"http://schemas.xmlsoap.org/wsdl/soap/http",
					new StringBuilder("http://").append(hostname).append(":")
							.append(port).append("/").append(warName)
							.append("/SimpleEchoProviderService").toString());
			Dispatch<SOAPMessage> dispatch = service.createDispatch(new QName(
					"http://abc", "anyPort"), SOAPMessage.class,
					Service.Mode.MESSAGE);
			dispatch.invoke(requestMessage);
		} catch (Exception e) {
			writer.println(e.getMessage());
		} finally {
			writer.flush();
			writer.close();
		}

	}

	private void invokeSimpleImplService(String warName, String serviceName,
			PrintWriter writer) throws ServletException, IOException {
		StringBuilder sBuilder = new StringBuilder("http://").append(hostname)
				.append(":").append(port).append("/").append(warName)
				.append("/").append(serviceName).append("?wsdl");

		QName qname = new QName(NAMESPACE, serviceName);
		URL wsdlLocation = new URL(sBuilder.toString());
		Service service = Service.create(wsdlLocation, qname);

		QName portType = new QName(NAMESPACE, "SimplePort");
		service.addPort(portType, SOAPBinding.SOAP11HTTP_BINDING,
				new StringBuilder("http://").append(hostname).append(":")
						.append(port).append("/").append(warName).append("/")
						.append(serviceName).toString());
		Dispatch<SOAPMessage> dispatch = service.createDispatch(portType,
				SOAPMessage.class, Service.Mode.MESSAGE);
		try {
			SOAPMessage message = MessageFactory.newInstance(
					SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();

			SOAPHeader header = message.getSOAPPart().getEnvelope().getHeader();
			SOAPElement ele = header.addChildElement(new QName(
					"http://docs.oasis-open.org/ws-tx/wscoor/2006/06",
					"CoordinationType", "wscoor"));
			ele.addAttribute(new QName(
					"http://schemas.xmlsoap.org/soap/envelope/",
					"mustUnderstand", "SOAP-ENV"), "1");
			ele.setTextContent("http://docs.oasis-open.org/ws-tx/wsat/2006/06");
			SOAPBody b = message.getSOAPBody();
			SOAPElement bele = b.addChildElement(new QName(
					"http://testservice.wsat.jaxws.ws.ibm.com/", "echo", "a"));
			bele.addChildElement(new QName("arg0")).setTextContent("Hello");
			dispatch.invoke(message);
		} catch (Exception e) {
			writer.println(e.getMessage());
		} finally {
			writer.flush();
			writer.close();
		}

	}

}
