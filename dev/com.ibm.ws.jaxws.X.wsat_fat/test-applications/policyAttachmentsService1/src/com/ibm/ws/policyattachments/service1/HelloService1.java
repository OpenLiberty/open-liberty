package com.ibm.ws.policyattachments.service1;


import javax.jws.WebService;

@WebService(serviceName="HelloService1")
public class HelloService1 {
	public String helloWithoutPolicy(){
		return "helloWithoutPolicy invoked";
	}
	
	public String helloWithPolicy(){
		return "helloWithPolicy invoked";
	}
	
	public String helloWithOptionalPolicy(){
		return "helloWithOptionalPolicy invoked";
	}
	
	public String helloWithYouWant(){
		return "helloWithYouWant invoked";
	}
}
