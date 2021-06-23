package com.ibm.ws.jaxws.wsat.testservice.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.wsat.testservice.Simple;

@WebService(endpointInterface = "com.ibm.ws.jaxws.wsat.testservice.Simple")
public class SimpleImpl implements Simple {

	@Override
	public String echo(String msg) {
		return msg;
	}

}
