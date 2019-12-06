/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.client.threaded;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.UserTransactionFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;
import com.ibm.ws.wsat.ut.util.AbstractTestServlet;

@WebServlet({ "/ThreadedClientServlet" })
public class ThreadedClientServlet extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private static String BASE_URL = "";
	private static Object o = new Object();
	private int count;

	private static final String filter = "(testfilter=jon)";
	private static AtomicInteger xaresourceindex = new AtomicInteger(0);
	private static AtomicInteger completedCount = new AtomicInteger(0);
	private static AtomicInteger failedCount = new AtomicInteger(0);

	class TestClass implements Runnable {

		private MultiThreaded proxy;
		private URL location;
		private int count;

		public TestClass(MultiThreaded s, URL wsdlLocation, int count) {
			this.proxy = s;
			this.location = wsdlLocation;
			this.count = count;
		}

		@Override
		public void run() {
			try {
				System.out.println("Thread " + count + ": " + " Thread Start!!!");
				UserTransaction userTransaction = UserTransactionFactory
						.getUserTransaction();
				try {
					userTransaction.begin();
				} catch (Exception e) {
					failedCount.incrementAndGet();
					e.printStackTrace();
					return;
				}

				System.out.println("Thread " + count + ": " + "userTransaction.begin()");
				int index = xaresourceindex.getAndIncrement();
				boolean result = enlistXAResouce(index);
				System.out.println("Thread " + count + ": " + "enlistXAResouce("+index+") 1: " + result);
				if (result == false) {
					try {
						userTransaction.rollback();
					} catch (Exception e) {
						e.printStackTrace();
					}
					failedCount.incrementAndGet();
					System.out.println("Thread " + count + ": " + "Enlist XAResouce failed");
					return;
				}

				String response;
				try {
					BindingProvider bind = (BindingProvider) proxy;
					bind.getRequestContext().put(
							"javax.xml.ws.service.endpoint.address",
							BASE_URL + "/threadedServer/MultiThreadedService");
					bind.getRequestContext().put("thread.local.request.context",
							"true");
					bind.getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", 3000);
					bind.getRequestContext().put("com.sun.xml.internal.ws.request.timeout", 3000);

					System.out.println("Thread " + count + ": " + "Get service from: " + location);
					response = proxy.invoke();
				} catch (Exception e) {
					try {
						userTransaction.rollback();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					failedCount.incrementAndGet();
					e.printStackTrace();
					return;
				}

				System.out.println("Thread " + count + ": " + "proxy.invoke(): " + response);
				if (response.contains("failed")) {
					failedCount.incrementAndGet();
					System.out.println("Thread " + count + ": " + "Got failed in web service response.");
					return;
				}

				try {
					System.out.println("Thread " + count + ": " + "userTransaction.commit()");
					userTransaction.commit();
				} catch (Exception e) {
					failedCount.incrementAndGet();
					System.out.println("If we get here we've probably started getting timeouts: Thread " + count + ": " + e.getMessage());
					e.printStackTrace();
				}
			} finally {
				completedCount.incrementAndGet();
				System.out.println("Thread " + count + ": " + " Thread End!!!");
			}
		}
	}

	protected String get(HttpServletRequest request) throws ServletException, IOException {
		BASE_URL = request.getParameter("baseurl");
		count = Integer.parseInt(request.getParameter("count"));

		StringBuilder sb = new StringBuilder();

		System.out.println("begin dispatch");
		URL wsdlLocation = new URL(BASE_URL
				+ "/threadedServer/MultiThreadedService?wsdl");
		MultiThreadedService service = new MultiThreadedService(
				wsdlLocation);
		MultiThreaded proxy = service.getMultiThreadedPort();
		BindingProvider bind = (BindingProvider) proxy;
		bind.getRequestContext().put(
				"javax.xml.ws.service.endpoint.address",
				BASE_URL + "/threadedServer/MultiThreadedService");
		try {
			boolean clearResult = proxy.clearXAResource();
			System.out.println("proxy.clearXAResource(): " + clearResult);
			if (clearResult == false) {
				return "<html><header></header><body>Fail to clear XAResources.</body></html>";
			}
			ExecutorService es = Executors.newFixedThreadPool(count);
			for (int i = 1; i <= count; i++) {
				System.out.println("Start Thread " + i);

				es.submit(new TestClass(proxy, wsdlLocation, i));
			}

			while (completedCount.get() < count) {
				System.out
				.println("Current completedCount = " + completedCount.get());
				Thread.sleep(1000);
			}
			es.shutdown();

			sb.append("<html><header></header><body>completedCount = ")
			  .append(completedCount)
			  .append(". transactionCount = ")
			  .append(XAResourceImpl.transactionCount())
			  .append(". failedCount = ")
			  .append(failedCount.get())
			  .append("</body></html>");

			System.out.println("Result = " + sb.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			proxy.clearXAResource();
			XAResourceImpl.printState();
			System.out.println("end dispatch");
		}
		
		return sb.toString();
	}

	private boolean enlistXAResouce(int count) {
		try {
			final ExtendedTransactionManager TM = TransactionManagerFactory
					.getTransactionManager();
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(count);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo).setExpectedDirection(XAResourceImpl.DIRECTION_EITHER);
			final int recoveryId = TM.registerResourceInfo(filter,
					xaResInfo);
			boolean result = TM.enlist(xaRes, recoveryId);
			if (result == false) {
				System.out.println("Enlist XAResource failed.");
				return false;
			}
		} catch (Exception e) {
			System.out.println("Get exception in enlistXAResouces :"
					+ e.toString());
			return false;
		}
		return true;
	}

	public static void sync() throws InterruptedException {
		synchronized (o) {
			System.out.println("sync(): " +	completedCount.incrementAndGet());
			o.wait(60000);
		}
	}
}
