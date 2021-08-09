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
package com.ibm.ws.wsat.ejbcdi.utils;

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
import javax.xml.ws.WebServiceException;

import com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServicePortProxy;
import com.ibm.ws.wsat.ejbcdi.client.ejb.EJBServicePortProxy;
import com.ibm.ws.wsat.ejbcdi.client.normal.NormalServicePortProxy;

public class CommonUtils {
	@SuppressWarnings("unused")

	public static String appName = "wsatEJBCDIApp";

	public static void executeService(final String service,
			final String method, String server) throws Exception {
		if (service == null || service.equals("")) {
			printLog("---> serverurl is null, ignore service");
		} else {
			final String servername = server.substring(0, server.length() - 2);
			printLog("server is " + servername);
			String type = server
					.substring(server.length() - 2, server.length());
			String result = "";
			if (type.equals("cr")) {
				printLog("Use proxy way for cdi + web service call with REQUIRED");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOther(method, servername);
			} else if (type.equals("cw")) {
				printLog("Use proxy way for cdi + web service call with REQUIRES_NEW");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOtherWithRequiresNew(method, servername);
			} else if (type.equals("cm")) {
				printLog("Use proxy way for cdi + web service call with MANDATORY");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOtherWithMandatory(method, servername);
			} else if (type.equals("cn")) {
				printLog("Use proxy way for cdi + web service call with NEVER");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOtherWithNever(method, servername);
			} else if (type.equals("cs")) {
				printLog("Use proxy way for cdi + web service call with SUPPORTS");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOtherWithSupports(method, servername);
			} else if (type.equals("co")) {
				printLog("Use proxy way for cdi + web service call with NOT_SUPPORTED");
				CDIServicePortProxy proxy = getCDIProxy(getUrlFromService(service, "CDI"));

				result = "testCDIHelloToOther: "
						+ proxy.testCDISayHelloToOtherWithNotSupported(method, servername);
			} else if (type.equals("er")) {
				printLog("Use proxy way for ejb + web service call with REQUIRED");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOther(method, servername);
			} else if (type.equals("ew")) {
				printLog("Use proxy way for ejb + web service call with REQUIRES_NEW");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOtherWithRequiresNew(method, servername);
			} else if (type.equals("em")) {
				printLog("Use proxy way for ejb + web service call with MANDATORY");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOtherWithMandatory(method, servername);
			} else if (type.equals("en")) {
				printLog("Use proxy way for ejb + web service call with NEVER");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOtherWithNever(method, servername);
			} else if (type.equals("es")) {
				printLog("Use proxy way for ejb + web service call with SUPPORTS");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOtherWithSupports(method, servername);
			} else if (type.equals("eo")) {
				printLog("Use proxy way for ejb + web service call with NOT_SUPPORTED");
				EJBServicePortProxy proxy = getEJBProxy(getUrlFromService(service, "EJB"));

				result = "testEJBHelloToOther: "
						+ proxy.testEJBSayHelloToOtherWithNotSupported(method, servername);
			} else if (type.equals("wn")) {
				printLog("Use proxy way for normal web service call");
				NormalServicePortProxy proxy = getNormalProxy(getUrlFromService(service, "Normal"));

				result = "normalHelloToOtherWithNever: "
						+ proxy.normalSayHelloToOther(method, servername);
			} else {
				printLog("Unknown client call way, you can use cx/ex/n for cdi/ejb/normal in the end of servername: server1en");
				throw new MalformedURLException(
						"Unknown client call way, you can use cx/ex/n for cdi/ejb/normal in the end of servername: server1en");
			}

			printLog("--- Client side request ---> " + result);
		}

	}

	public static String getUrlFromService(String service, String type) {
		String serviceUrl = service + "/" + appName + "/" + type + "ServiceService?wsdl";
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

	public static String sayHello(String method, String server) throws NamingException, SQLException {
		printLog("********** Begin server service normalSayHelloToOther and operation "
				+ method + " **********");
		
		getDBCount(server);
		String helloTo = "Hello " + method;

		printLog("Begin operation " + method);
		if (method != null) {
			printLog("Get server count before server database end: " + CommonUtils.getDBCount(server));
			if (method.equals("rollback")) {
				insertDB(server);
				throw new WebServiceException(
						"Throw exception for rollback from server side!");
			} else if (method.equals("cleandb")) {
				cleanDB(server);
			} else if (method.equals("countdb")) {
				getDBCount(server);
			} else if (method.equals("listdb")) {
				getDBList(server);
			} else {
				insertDB(server);
			}
			printLog("Get client count after server database end: " + CommonUtils.getDBCount(server));
		} else {
			printLog("No database call");
		}
		printLog("--- Server Side ---> return result: " + helloTo);
		printLog("End operation " + method);
		printLog("**********    End server normalSayHelloToOther and operation "
				+ method + " **********");
		int count = CommonUtils.getDBCount(server);
		printLog("Get server count after server transaction end: " + count);
		printLog("Get server list after server transaction end: "
				+ CommonUtils.getDBList(server));
		return helloTo + " " + count;
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

		CommonUtils.printLog("| withouttrans=" + withoutTrans
				+ ": [withouttrans=true]");
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
			e.printStackTrace();
			if (clientTrans != null && clientTrans.equals("commitincatch")){
				CommonUtils.printLog("Get Exception but also commit: " + e.getMessage());
				try {
					 userTransaction.commit();
				} catch (Exception e1) {
					 CommonUtils.printLog("Get Exception when commit in exception: " + e1.getMessage());
					 e1.printStackTrace();
				}
				e.printStackTrace();
			}
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

	public static CDIServicePortProxy getCDIProxy(String service)
			throws MalformedURLException {
		QName serviceName = new QName("http://server.ejbcdi.wsat.ws.ibm.com/",
				"CDIServiceService");
		CDIServicePortProxy proxy = new CDIServicePortProxy(new URL(service),
				serviceName);

		// ClientProxy.getClient(proxy._getDescriptor().getProxy());
		BindingProvider bind = (BindingProvider) proxy._getDescriptor()
				.getProxy();
		bind.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				service);

		return proxy;
	}

	public static EJBServicePortProxy getEJBProxy(String service)
			throws MalformedURLException {
		QName serviceName = new QName("http://server.ejbcdi.wsat.ws.ibm.com/",
				"EJBServiceService");
		EJBServicePortProxy proxy = new EJBServicePortProxy(new URL(service),
				serviceName);

		// ClientProxy.getClient(proxy._getDescriptor().getProxy());
		BindingProvider bind = (BindingProvider) proxy._getDescriptor()
				.getProxy();
		bind.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				service);

		return proxy;
	}
	
	public static NormalServicePortProxy getNormalProxy(String service)
			throws MalformedURLException {
		QName serviceName = new QName("http://server.ejbcdi.wsat.ws.ibm.com/",
				"NormalServiceService");
		NormalServicePortProxy proxy = new NormalServicePortProxy(new URL(service),
				serviceName);

		// ClientProxy.getClient(proxy._getDescriptor().getProxy());
		BindingProvider bind = (BindingProvider) proxy._getDescriptor()
				.getProxy();
		bind.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				service);

		return proxy;
	}
}
