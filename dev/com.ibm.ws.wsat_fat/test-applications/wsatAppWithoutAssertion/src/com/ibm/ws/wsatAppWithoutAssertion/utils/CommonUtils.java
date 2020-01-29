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
package com.ibm.ws.wsatAppWithoutAssertion.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.ibm.ws.wsatAppWithoutAssertion.client.Hello;
import com.ibm.ws.wsatAppWithoutAssertion.client.HelloImplPortProxy;
import com.ibm.ws.wsatAppWithoutAssertion.client.HelloImplService;

public class CommonUtils {
	@SuppressWarnings("unused")

	public static String appName = "wsatAppWithoutAssertion";

	public static HelloImplPortProxy getProxy(String service)
			throws MalformedURLException {
		QName serviceName = new QName("http://server.wsatAppWithoutAssertion.ws.ibm.com/",
				"HelloImplService");
		HelloImplPortProxy proxy = new HelloImplPortProxy(new URL(service),
				serviceName);

		// ClientProxy.getClient(proxy._getDescriptor().getProxy());
		BindingProvider bind = (BindingProvider) proxy._getDescriptor()
				.getProxy();
		bind.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				service);

		return proxy;
	}

	public static void executeService(String service, String method,
			String server) throws MalformedURLException {
		if (service == null || service.equals("")) {
			printLog("---> serverurl is null, ignore service");
		} else {
			String servername = server.substring(0, server.length() - 1);
			printLog("server is " + servername);
			String type = server
					.substring(server.length() - 1, server.length());
			String result = "";
			if (type.equals("d")) {
				// Use Dispatch way
				// Mi will provide the Dispatch test code
				printLog("Use Dispatch way for web service call");
				throw new MalformedURLException(
						"Mi will provide the Dispatch test code");
			} else if (type.equals("l")) {
				// Use local wsdl way
				printLog("Use local wsdl way for web service call");
				URL LOCAL_SERVICE_WSDL_ADDRESS = HelloImplService.class
						.getResource("/WEB-INF/wsdl/hello.wsdl");
				String SERVICE_ADDRESS = service + "/" + appName
						+ "/HelloImplService";

				// use local wsdl file to go through basic auth
				HelloImplService helloService = new HelloImplService(
						LOCAL_SERVICE_WSDL_ADDRESS, new QName(
								"http://server.wsatAppWithoutAssertion.ws.ibm.com/",
								"HelloImplService"));
				Hello helloPort = helloService.getHelloImplPort();
				((BindingProvider) helloPort).getRequestContext().put(
						BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
						SERVICE_ADDRESS);

				helloPort.sayHelloToOther(method, servername);
			} else if (type.equals("p")) {
				// Use proxy way
				printLog("Use proxy way for web service call");
				HelloImplPortProxy proxy = getProxy(getUrlFromService(service));

				if (method == null || method.equals("")) {
					result = "sayHello: " + proxy.sayHello();
				} else {
					result = "sayHelloToOther: "
							+ proxy.sayHelloToOther(method, servername);
				}
			} else {
				printLog("Unknown client call way, you can use p, l or d in the end of servername: server1p");
				throw new MalformedURLException("Unknown client call way, you can use p, l or d in the end of servername: server1p");
			}

			printLog("--- Client side request ---> " + result);
		}

	}

	public static String getUrlFromService(String service) {
		String serviceUrl = service + "/" + appName + "/HelloImplService?wsdl";
		printLog("[Service location: " + serviceUrl + "]");
		return serviceUrl;
	}

	public static void printLog(String log) {
		System.out.println(log);
		// comment for FAT test
		// if (writer != null) {
		// writer.print(log + "\n<br>");
		// }
	}

	public static void insertDB(String type) throws NamingException,
			SQLException {
		String num = getNum(type);
		Connection wsatDbConnection = null;
		Statement statement = null;

		try {
			Context ctx = new InitialContext();
			DataSource wsatDatabase = (DataSource) ctx
					.lookup("jdbc/wsatDataSource" + num);
			CommonUtils.printLog("Before " + type + " database call");
			wsatDbConnection = wsatDatabase.getConnection();
			statement = wsatDbConnection.createStatement();

			String primaryKey = ""
					+ (System.currentTimeMillis() % 10000000000l);
			CommonUtils.printLog("---> primary key is " + primaryKey);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd H:m:s");
			String time = format.format(new Date());
			statement.execute("insert into wsatTable" + num + " values ('"
					+ type + primaryKey + "','" + time + "')");

			CommonUtils.printLog("---> insert into wsatTable" + num
					+ " values ('" + type + primaryKey + "','" + time + "')");
			CommonUtils.printLog("After " + type + " database call");
		} finally {
			if (statement != null) {
				statement.close();
			}
			if (wsatDbConnection != null) {
				wsatDbConnection.close();
			}
		}
	}

	public static int getDBCount(String type) throws NamingException,
			SQLException {
		String num = getNum(type);
		Connection wsatDbConnection = null;
		Statement statement = null;
		try {
			Context ctx = new InitialContext();
			DataSource wsatDatabase = (DataSource) ctx
					.lookup("jdbc/wsatDataSource" + num);
			wsatDbConnection = wsatDatabase.getConnection();
			statement = wsatDbConnection.createStatement();
			ResultSet rs = statement
					.executeQuery("select count(*) from wsatTable" + num);
			rs.next();
			int count = rs.getInt(1);
			CommonUtils.printLog("---> get count from wsatTable" + num + ": "
					+ count);
			return count;
		} finally {
			if (statement != null) {
				statement.close();
			}
			if (wsatDbConnection != null) {
				wsatDbConnection.close();
			}
		}
	}

	public static String getDBList(String type) throws NamingException,
			SQLException {
		String result = "getDBList from " + type + ":\n";
		String num = getNum(type);
		Connection wsatDbConnection = null;
		Statement statement = null;
		try {
			Context ctx = new InitialContext();
			DataSource wsatDatabase = (DataSource) ctx
					.lookup("jdbc/wsatDataSource" + num);
			wsatDbConnection = wsatDatabase.getConnection();
			statement = wsatDbConnection.createStatement();
			ResultSet rs = statement.executeQuery("select * from wsatTable"
					+ num);
			while (rs.next()) {
				result += " >{" + rs.getString(1) + "|" + rs.getString(2)
						+ "}\n";
			}
		} finally {
			if (statement != null) {
				statement.close();
			}
			if (wsatDbConnection != null) {
				wsatDbConnection.close();
			}
		}
		return result;
	}

	public static void cleanDB(String type) throws NamingException,
			SQLException {
		String num = getNum(type);
		Connection wsatDbConnection = null;
		Statement statement = null;
		try {
			Context ctx = new InitialContext();
			DataSource wsatDatabase = (DataSource) ctx
					.lookup("jdbc/wsatDataSource" + num);
			wsatDbConnection = wsatDatabase.getConnection();
			statement = wsatDbConnection.createStatement();
			statement.execute("delete from wsatTable" + num);
			CommonUtils.printLog("---> delete from wsatTable" + num);
		} finally {
			if (statement != null) {
				statement.close();
			}
			if (wsatDbConnection != null) {
				wsatDbConnection.close();
			}
		}
	}

	public static void createTable(String type) throws NamingException,
			SQLException {
		String num = getNum(type);
		Connection wsatDbConnection = null;
		Statement statement = null;
		try {
			Context ctx = new InitialContext();
			DataSource wsatDatabase = (DataSource) ctx
					.lookup("jdbc/wsatDataSource" + num);
			wsatDbConnection = wsatDatabase.getConnection();
			statement = wsatDbConnection.createStatement();
			statement
					.executeUpdate("create table wsatTable"
							+ num
							+ "(id varchar(20) primary key not null,value varchar(60))");
			CommonUtils.printLog("---> no table, create wsatTable" + num);
		} catch (Exception e) {
			CommonUtils.printLog("---> " + e.getMessage()
					+ ", so skip creating table");
		} finally {
			if (statement != null) {
				statement.close();
			}
			if (wsatDbConnection != null) {
				wsatDbConnection.close();
			}
		}
	}

	public static void initTable(String type) throws NamingException,
			SQLException {
		String num = getNum(type);
		CommonUtils.printLog("---------- Init DB wsatTable" + num
				+ " start ----------");
		try {
			cleanDB(type);
		} catch (Exception e) {
			createTable(type);
		}
		CommonUtils.printLog("---------- Init DB wsatTable" + num
				+ " end ----------");
	}

	public static String getNum(String type) {
		String num = "-1";
		if (type.equals("client")) {
			num = "0";
		} else if (type.startsWith("server")) {
			// Change to use same num with client
			num = type.substring("server".length(), type.length());
		}
		return num;
	}

	public static String mainLogic(HttpServletRequest request) {
		String serverurl = "server";

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CommonUtils.printLog("\n\n!!![" + df.format(new Date())
				+ "] WS-AT Test Application Logging from "
				+ request.getLocalAddr() + ":" + request.getLocalPort());
		CommonUtils.printLog("Request URL: " + CommonUtils.getUrl(request));

		// String service = "http://localhost:9080/wsatApp/HelloImplService";
		// String service1 = request.getParameter("serverurl1");
		// String service2 = request.getParameter("serverurl2");

		Map<String, String> servers = new HashMap<String, String>();
		Enumeration<?> enu = request.getParameterNames();
		while (enu.hasMoreElements()) {
			String paraName = (String) enu.nextElement();
			CommonUtils.printLog("Get parameter: " + paraName + " - "
					+ request.getParameter(paraName));
			if (paraName.startsWith(serverurl)) {
				servers.put(paraName, request.getParameter(paraName));
			}
		}
		String clientTrans = request.getParameter("client");
		String withoutTrans = request.getParameter("withouttrans");

		CommonUtils
				.printLog("\n|-------------------- README --------------------|");

		Set<String> setServers = servers.keySet();
		for (Iterator<String> iter = setServers.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String value = (String) servers.get(key);
			CommonUtils.printLog("| " + key + "=" + value);
		}
		CommonUtils.printLog("| client=" + clientTrans);

		CommonUtils
				.printLog("| Server URL example: [server1p=commit:http://localhost:9081, won't request if null]");
		CommonUtils
				.printLog("| Server Operation type: [d: DIspatch, l:local wsdl, p or other: client proxy]");
		CommonUtils
				.printLog("| Server Operation example: [commit, rollback, transcommit, cleandb, countdb, listdb or nested:commit:server1:http://localhost:9083 for sayHelloToOther method, null for sayHello method]");

		CommonUtils
				.printLog("| Client Operation example: [commit, rollback, exception, setrollbackonly or cleandb, countdb, listdb]");
		CommonUtils.printLog("| withouttrans=" + withoutTrans + ": [withouttrans=true]");
		CommonUtils
				.printLog("|------------------------------------------------|\n");
		CommonUtils.printLog("Please see more detail in Liberty log...\n");

		CommonUtils.printLog("========== Begin ClientServlet ==========");
		CommonUtils.printLog("[Client location: " + request.getLocalAddr()
				+ ":" + request.getLocalPort() + "]");

		UserTransaction userTransaction = null;
		String transName = "java:comp/UserTransaction";
		try {
			CommonUtils.printLog("Begin try-catch...");

			if (withoutTrans != null && withoutTrans.equals("true")) {
				CommonUtils.printLog("Test without user transaction");
			} else {
				Context ctx = new InitialContext();
				userTransaction = (UserTransaction) ctx.lookup(transName);
				userTransaction.begin();
				CommonUtils.printLog("Client user transaction (" + transName
						+ ") begin...");
				CommonUtils.printLog("Client user transaction getStatus: "
						+ userTransaction.getStatus());
			}

			CommonUtils
					.printLog("Get client count before client database call: "
							+ CommonUtils.getDBCount("client"));
			CommonUtils
					.printLog("Get client list before client transaction end: "
							+ CommonUtils.getDBList("client"));

			CommonUtils
					.printLog("---------- Client db invoke begin ----------");
			CommonUtils.insertDB("client");
			CommonUtils.printLog("---------- Client db invoke end ----------");
			CommonUtils
					.printLog("Get client count after client database call: "
							+ CommonUtils.getDBCount("client") + "\n");

			// Web Service call
			for (Iterator<String> iter = setServers.iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				String value = (String) servers.get(key);
				String oper = value.substring(0, value.indexOf(":"));
				String url = value.substring(oper.length() + 1, value.length());
				CommonUtils.printLog("---------- " + key
						+ " invoke begin ----------");

				CommonUtils.executeService(url, oper, key);
				CommonUtils.printLog("---------- " + key
						+ " invoke end ----------\n");
			}

			CommonUtils
					.printLog("---------- Client transaction final operation begin ----------\n");

			if (withoutTrans != null && withoutTrans.equals("true")) {
				CommonUtils
						.printLog("Without transactoin, so ignore all client transaction action in the end");
			} else {
				CommonUtils
						.printLog("Client user transaction getStatus before "
								+ clientTrans + ": "
								+ userTransaction.getStatus());
				if (clientTrans != null) {
					if (clientTrans.equals("rollback")) {
						userTransaction.rollback();
					} else if (clientTrans.equals("exception")) {
						throw new RuntimeException(
								"Throw new RuntimeException from client side!");
					} else if (clientTrans.equals("setrollbackonly")) {
						CommonUtils
								.printLog("userTransaction.setRollbackOnly from client side to test Rollback Excetion!\n");
						userTransaction.setRollbackOnly();
					} else if (clientTrans.equals("cleandb")) {
						CommonUtils.cleanDB("client");
						userTransaction.commit();
					} else if (clientTrans.equals("countdb")) {
						CommonUtils.getDBCount("client");
						userTransaction.commit();
					} else if (clientTrans.equals("listdb")) {
						CommonUtils.getDBList("client");
						userTransaction.commit();
					} else {
						userTransaction.commit();
					}
				} else {
					userTransaction.commit();
				}
				CommonUtils.printLog("Client user transaction (" + transName
						+ ") " + clientTrans + "...");
				CommonUtils.printLog("Client user transaction getStatus after "
						+ clientTrans + ": " + userTransaction.getStatus());
			}
			CommonUtils
					.printLog("---------- Client transaction final operation end ----------");
		} catch (Exception e) {
			CommonUtils.printLog("Get Exception: " + e.getMessage());
			// CommonUtils.printLog("Get Exception but also commit: " +
			// e.getMessage());
			// try {
			// userTransaction.commit();
			// } catch (Exception e1) {
			// CommonUtils.printLog("Get Exception when commit in exception: " +
			// e1.getMessage());
			// e1.printStackTrace();
			// }
			e.printStackTrace();
			return e.getMessage();
		}

		CommonUtils.printLog("End try-catch...");
		CommonUtils.printLog("==========    End ClientServlet ==========");
		try {
			CommonUtils
					.printLog("Get client count after client transaction end: "
							+ CommonUtils.getDBCount("client"));
			CommonUtils
					.printLog("Get client list after client transaction end: "
							+ CommonUtils.getDBList("client"));
		} catch (Exception e) {
			CommonUtils.printLog("Get Exception when final get db count: "
					+ e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}
		CommonUtils.printLog("\n\n");
		return "Success";
	}

	public static String getUrl(HttpServletRequest req) {
		String reqUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString(); // d=789
		if (queryString != null) {
			reqUrl += "?" + queryString;
		}
		return reqUrl;
	}
}
