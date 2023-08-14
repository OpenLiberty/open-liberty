/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.endtoend.client.endtoend;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebServlet({ "/EndToEndClientServlet" })
public class EndToEndClientServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private static String TEST_NAME_PARAM = "testName";

    @Resource
    UserTransaction userTransaction;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("Servlet: " + request.getRequestURI());
        System.out.println("Test: " + request.getParameter(TEST_NAME_PARAM));

        final Enumeration<?> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            final String param = (String) params.nextElement();

            if (!TEST_NAME_PARAM.equals(param)) {
                System.out.println(param + ": " + request.getParameter(param));
            }
        }

        final String result = get(request);
        
        response.getWriter().println(result);
    }

	protected String get(HttpServletRequest request) throws ServletException, IOException {
		String finalOutput = "";
		try {
			System.out.println("begin try-catch");
			String type = request.getParameter(TEST_NAME_PARAM);
			System.out.println("==============Test type: " + type
					+ "================");
			String BASE_URL = request.getParameter("baseurl");
			if (BASE_URL == null || BASE_URL.equals(""))
				BASE_URL = "http://localhost:8010";
			String BASE_URL2 = request.getParameter("baseurl2");
			if (BASE_URL2 == null || BASE_URL2.equals(""))
				BASE_URL2 = "http://localhost:8010";
			
			URL wsdlLocation = new URL(BASE_URL
					+ "/endtoend/HelloImplTwowayService?wsdl");
			HelloImplTwowayService service = new HelloImplTwowayService(
					wsdlLocation);
			HelloImplTwoway proxy = service.getHelloImplTwowayPort();
			BindingProvider bind = (BindingProvider) proxy;
			Map<String, Object> requestContext = bind.getRequestContext();
			requestContext.put(
					"javax.xml.ws.service.endpoint.address",
					BASE_URL + "/endtoend/HelloImplTwowayService");
			TxTestUtils.setTimeouts(requestContext, 300000);
			if (type.equals("testTwoServerCommit") || type.equals("testFeatureDynamic")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_COMMIT);
				if (result == true) {
					System.out.println("Reply from server: "
							+ proxy.sayHello("commitclear", XAResourceImpl.DIRECTION_COMMIT));
					userTransaction.commit();
					System.out.println("client user transaction commit");
					finalOutput = "Finish Twoway message";
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testTwoServerCommitClientVotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("rollback",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						System.out.println("Reply from server: "
								+ proxy.sayHello("commit", XAResourceImpl.DIRECTION_ROLLBACK));
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
			} else if (type.equals("testTwoServerCommitProviderVotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("commit",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						System.out.println("Reply from server: "
							+ proxy.sayHello("rollback", XAResourceImpl.DIRECTION_ROLLBACK));
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput =  "Get expect RollbackException: " + e.toString();
				}
			}else if (type.equals("testTwoServerRollback")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_ROLLBACK);
				if (result == true) {
					System.out.println("Reply from server: "
							+ proxy.sayHello("commit", XAResourceImpl.DIRECTION_ROLLBACK));
					userTransaction.rollback();
					System.out.println("client user transaction rollbak");
					finalOutput = "Finish Twoway message";
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testTwoServerTwoCallCommit")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_COMMIT);
				if (result == true) {
					finalOutput = proxy.callAnother(BASE_URL2,"commit","commit",XAResourceImpl.DIRECTION_COMMIT);
					System.out.println("Reply from server: "
							+ finalOutput);
					userTransaction.commit();
					System.out.println("client user transaction commit");
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testThreeServerTwoCallCommit")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_COMMIT);
				if (result == true) {
					finalOutput = proxy.callAnother(BASE_URL2,"commit","commitclear",XAResourceImpl.DIRECTION_COMMIT);
					System.out.println("Reply from server: "
							+ finalOutput);
					userTransaction.commit();
					System.out.println("client user transaction commit");
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testTwoServerTwoCallCoordinatorVotingRollback")) {
				try {
					userTransaction.begin();
					System.out.println("userTransaction.begin()");
					boolean result = enlistXAResource("rollback",
							XAResourceImpl.DIRECTION_ROLLBACK);
					System.out.println("enlistXAResourse(rollback, XAResourceImpl.DIRECTION_ROLLBACK): " + result);
					if (result == true) {
						System.out.println("call proxy.callAnother");
						finalOutput = proxy.callAnother(BASE_URL2, "commit",
								"commit", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
				System.out.println("Get finalOutput: " + finalOutput);
			}else if (type.equals("testThreeServerTwoCallCoordinatorVotingRollback")) {
				try {
					userTransaction.begin();
					System.out.println("userTransaction.begin()");
					boolean result = enlistXAResource("rollback",
							XAResourceImpl.DIRECTION_ROLLBACK);
					System.out.println("enlistXAResourse(rollback, XAResourceImpl.DIRECTION_ROLLBACK): " + result);
					if (result == true) {
						System.out.println("call proxy.callAnother");
						finalOutput = proxy.callAnother(BASE_URL2, "commit",
								"commitclear", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
				System.out.println("Get finalOutput: " + finalOutput);
			}else if (type.equals("testTwoServerTwoCallParticipant1VotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("commit",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						finalOutput = proxy.callAnother(BASE_URL2, "rollback",
								"commit", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
			}else if (type.equals("testThreeServerTwoCallParticipant1VotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("commit",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						finalOutput = proxy.callAnother(BASE_URL2, "rollback",
								"commitclear", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
			}else if (type.equals("testTwoServerTwoCallParticipant2VotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("commit",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						finalOutput = proxy.callAnother(BASE_URL2, "commit",
								"rollback", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
			}else if (type.equals("testThreeServerTwoCallParticipant2VotingRollback")) {
				try {
					userTransaction.begin();
					boolean result = enlistXAResource("commit",
							XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == true) {
						finalOutput = proxy.callAnother(BASE_URL2, "commit",
								"rollbackclear", XAResourceImpl.DIRECTION_ROLLBACK);
						System.out.println("Reply from server: " + finalOutput);
						userTransaction.commit();
						System.out.println("client user transaction commit");
						finalOutput = "Finish Twoway message";
					} else {
						finalOutput = "EnlistXAResource failed.";
					}
				} catch (RollbackException e) {
					finalOutput = "Get expect RollbackException: "
							+ e.toString();
				}
			}else if (type.equals("testTwoServerTwoCallRollback")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_ROLLBACK);
				if (result == true) {
					finalOutput = proxy.callAnother(BASE_URL2,"commit","commit",XAResourceImpl.DIRECTION_ROLLBACK);
					System.out.println("Reply from server: "
							+ finalOutput);
					userTransaction.rollback();
					System.out.println("client user transaction rollbak");
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testThreeServerTwoCallRollback")) {
				userTransaction.begin();
				boolean result = enlistXAResource("commit", XAResourceImpl.DIRECTION_ROLLBACK);
				if (result == true) {
					finalOutput = proxy.callAnother(BASE_URL2,"commit","commitclear",XAResourceImpl.DIRECTION_ROLLBACK);
					System.out.println("Reply from server: "
							+ finalOutput);
					userTransaction.rollback();
					System.out.println("client user transaction rollbak");
				} else {
					finalOutput = "EnlistXAResource failed.";
				}
			}else if (type.equals("testNoOptionalNoTransaction")) {
				// "-1" is used for informing provider of not enlisting XAResourse
				System.out.println("Reply from server: " + proxy.sayHello("commit", -1));
				finalOutput = "Finish Twoway message";
			}
		} catch (Exception e) {
			finalOutput = "Client catch exception: " + e.toString();
			e.printStackTrace();
		}

		System.out.println("end dispatch");
		return "<html><header></header>"
						+ "<body>" + finalOutput + "</body></html>";
	}

	private boolean enlistXAResource(String vote, int expectedDirection){
		boolean result = false;
		try {
			XAResourceImpl.clear();
			final ExtendedTransactionManager TM = TransactionManagerFactory
					.getTransactionManager();
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes;
			if (vote.equals("rollback")) {
				xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(

						xaResInfo).setPrepareAction(XAException.XA_RBROLLBACK);
			} else {
				xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo);
			}
			final int recoveryId = TM.registerResourceInfo(XAResourceInfoFactory.filter,
					xaResInfo);
			xaRes.setExpectedDirection(expectedDirection);
			result = TM.enlist(xaRes, recoveryId);
		} catch (XAResourceNotAvailableException e) {
			System.out.println("Catch XAResourceNotAvailableException:" + e.toString());
			return false;
		} catch (IllegalStateException e) {
			System.out.println("Catch IllegalStateException:" + e.toString());
		} catch (RollbackException e) {
			System.out.println("Catch RollbackException:" + e.toString());
		} catch (SystemException e) {
			System.out.println("Catch SystemException:" + e.toString());
		}
		return result;
	}
}
