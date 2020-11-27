package com.ibm.was.wssample.sei.ping;



@javax.jws.WebService (endpointInterface="com.ibm.was.wssample.sei.ping.PingServicePortType", targetNamespace="http://com/ibm/was/wssample/sei/ping/", serviceName="PingService", portName="PingServicePort")
public class PingSOAPImpl{

    public void pingOperation(PingStringInput parameter) {
		if (parameter != null) {
			try {
				System.out.println(">> SERVICE: SOAP11 Ping Input String '"+parameter.getPingInput()+"'");
			} catch (Exception e) {
				System.out.println(">> SERVICE: SOAP11 Ping ERROR Exception "+e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println(">> SERVICE: ERROR - SOAP12 Ping Missing Input String");
		}       
    }

}