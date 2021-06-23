package com.ibm.ws.jaxws.wsat.testservice.impl;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(wsdlLocation = "WEB-INF/wsdl/SimpleEchoProviderService.wsdl", serviceName = "SimpleEchoProviderService", targetNamespace = "http://simpleEchoProvider.impl.testservice.wsat.jaxws.ws.ibm.com/")
@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)
public class SimpleEchoProvider implements Provider<SOAPMessage> {
	public SOAPMessage invoke(SOAPMessage request) {
		try {
			SOAPElement echoSoapElement = (SOAPElement) request.getSOAPBody()
					.getChildElements().next();
			SOAPElement arg0SoapElement = (SOAPElement) echoSoapElement
					.getChildElements().next();
			String requestEchoValue = arg0SoapElement.getChildNodes().item(0)
					.getNodeValue();

			MessageFactory messageFactory = MessageFactory
					.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPHeader header = soapMessage.getSOAPPart().getEnvelope()
					.getHeader();
			SOAPElement ele = header.addChildElement(new QName(
					"http://docs.oasis-open.org/ws-tx/wscoor/2006/06",
					"CoordinationType", "wscoor"));
			ele.addAttribute(new QName(
					"http://schemas.xmlsoap.org/soap/envelope/",
					"mustUnderstand", "SOAP-ENV"), "1");
			ele.setTextContent("http://docs.oasis-open.org/ws-tx/wsat/2006/06");
			SOAPElement soapElement = soapMessage
					.getSOAPBody()
					.addChildElement(
							new QName(
									"http://impl.testservice.wsat.jaxws.ws.ibm.com/",
									"echoResponse", "tns"));
			soapElement.addChildElement("return").addTextNode(
					"Echo Response [" + requestEchoValue + "]");
			return soapMessage;
		} catch (SOAPException e) {
			e.printStackTrace();
		}
		return null;
	}
}