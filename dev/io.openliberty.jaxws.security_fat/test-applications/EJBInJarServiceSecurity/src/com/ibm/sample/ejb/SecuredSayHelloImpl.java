package com.ibm.sample.ejb;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService(serviceName = "SecuredSayHelloService", portName = "SayHelloStalelessPort", endpointInterface = "com.ibm.sample.ejb.SayHelloInterface", targetNamespace = "http://jaxws.samples.ibm.com")
@DeclareRoles({ "role12", "role34", "role21" })
@RolesAllowed("role12")
public class SecuredSayHelloImpl implements SayHelloInterface {

	@Override
	public String sayHello(String name) {
		return "Hello " + name + " from secured ejb web service.";
	}

}
