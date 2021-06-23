package com.ibm.ws.policyattachments.service2;

import javax.jws.WebService;

@WebService(serviceName="HelloService2")
public class HelloService2 {
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
