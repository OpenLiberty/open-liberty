/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsatAppOptional.server;

import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.ws.wsatAppOptional.client.HelloImplPortProxy;
import com.ibm.ws.wsatAppOptional.utils.CommonUtils;

@WebService(endpointInterface = "com.ibm.ws.wsatAppOptional.server.Hello", wsdlLocation = "WEB-INF/wsdl/hello.wsdl")
public class HelloImpl {

	public String sayHello() throws Exception {
		printLog("********** Beigin server service sayHello without db operation **********");
		printLog("**********    End server service sayHello without db operation **********");
		return "Return sayHello without any db operation";
	}

	public String basicSayHelloToOther(String method, String server) throws Exception {
		CommonUtils.getDBCount(server);
		String helloTo = "Hello " + method;

		printLog("Begin operation " + method);
		if (method != null) {
			CommonUtils.printLog("Get server count before server database end: " + CommonUtils.getDBCount(server));
			if (method.equals("rollback")) {
				throw new RuntimeException(
						"Throw exception for rollback from server side!");
			} else if (method.equals("transcommit")) {
				// Use its own transaction to commit
				UserTransaction userTransaction = null;
				Context ctx = new InitialContext();
				String transName = "java:comp/UserTransaction";
				userTransaction = (UserTransaction) ctx.lookup(transName);
				userTransaction.begin();
				CommonUtils.insertDB(server);
				userTransaction.commit();
			} else if (method.equals("cleandb")) {
				CommonUtils.cleanDB(server);
			} else if (method.equals("countdb")) {
				CommonUtils.getDBCount(server);
			} else if (method.equals("listdb")) {
				CommonUtils.getDBList(server);
			} else if (method.startsWith("nested")) {
				String[] paras = method.split("-");
				String oper = paras[1];
				String ser = paras[2];
				String port = paras[3];
				// Can only be used for non-secure http test
				String service = "http://localhost:" + port;
				HelloImplPortProxy proxy = CommonUtils.getProxy(CommonUtils
						.getUrlFromService(service));
				String result = "sayHelloToOther: "
						+ proxy.sayHelloToOther(oper, ser);
				printLog("--- Nested request ---> " + result);
				// Also insert to its database
				CommonUtils.insertDB(server);
			} else {
				CommonUtils.insertDB(server);
			}
			CommonUtils.printLog("Get client count after server database end: " + CommonUtils.getDBCount(server));
		} else {
			printLog("No database call");
		}
		printLog("--- Server Side ---> return result: " + helloTo);
		printLog("End operation " + method);
		printLog("**********    End server service sayHelloToOther/sayHelloToOtherWithout and operation "
				+ method + " **********");
		int count = CommonUtils.getDBCount(server);
		CommonUtils
		.printLog("Get server count after server transaction end: " + count);
		CommonUtils
		.printLog("Get server list after server transaction end: "
				+ CommonUtils.getDBList(server));
		return helloTo + " " + count;
	}
	
	// For with assertion test
	public String sayHelloToOther(String method, String server) throws Exception {
		printLog("********** Beigin server service sayHelloToOther and operation "
				+ method + " **********");
		return basicSayHelloToOther(method, server);
	}
	
	// For without assertion test
	public String sayHelloToOtherWithout(String method, String server) throws Exception {
		printLog("********** Beigin server service sayHelloToOtherWithout and operation "
				+ method + " **********");
		return basicSayHelloToOther(method, server);
	}

	private void printLog(String log) {
		System.out.println(log);
		// writer.print(log + "<br/>");
	}
}
