package com.ibm.ws.wsat.fat.server;

import javax.jws.WebService;

@WebService(wsdlLocation="WEB-INF/wsdl/WSATAssertion.wsdl")
public class HelloImplWSATAssertion{
	public String sayHello(){
		return "Hello World!";
	}
	
	public String sayHelloToOther(String username){
		return "Hello " + username;
	}
}
