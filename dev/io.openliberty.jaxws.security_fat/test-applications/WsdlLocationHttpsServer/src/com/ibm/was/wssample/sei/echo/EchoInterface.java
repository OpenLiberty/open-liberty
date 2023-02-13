package com.ibm.was.wssample.sei.echo;

import javax.jws.WebService;

@WebService(name = "EchoService", targetNamespace = "http://echo.sei.wssample.was.ibm.com")
public interface EchoInterface {
	
	public String echo(String name);

}
