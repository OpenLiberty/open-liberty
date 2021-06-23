package com.ibm.ws.jaxws.wsat.testservice;

import javax.jws.WebService;

@WebService(name = "Simple")
public interface Simple {
	public String echo(String msg);

}
