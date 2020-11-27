package com.ibm.was.wssample.sei.ping;



@javax.jws.WebService (endpointInterface="com.ibm.was.wssample.sei.ping.PingService12PortType", targetNamespace="http://com/ibm/was/wssample/sei/ping/", serviceName="PingService12", portName="PingService12Port", wsdlLocation="WEB-INF/wsdl/Ping12.wsdl")
@javax.xml.ws.BindingType (value=javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class PingSOAP12Impl{

    public void pingOperation(PingStringInput parameter) {
		if (parameter != null) {
			try {
				System.out.println(">> SERVICE: SOAP12 Ping Input String '"+parameter.getPingInput()+"'");
			} catch (Exception e) {
				System.out.println(">> SERVICE: SOAP12 Ping ERROR Exception "+e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println(">> SERVICE: ERROR - SOAP12 Ping Missing Input String");
		}
    }

}