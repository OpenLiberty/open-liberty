package com.ibm.was.wssample.sei.echo;

import javax.jws.WebService;

@WebService(serviceName = "EchoService",
            portName = "EchoPort",
            endpointInterface = "com.ibm.was.wssample.sei.echo.EchoInterface",
            targetNamespace = "http://echo.sei.wssample.was.ibm.com")
public class EchoImpl implements EchoInterface {

	@Override
	public String echo(String name) {
		return ("JAX-WS==>>" + name);
	}

}
