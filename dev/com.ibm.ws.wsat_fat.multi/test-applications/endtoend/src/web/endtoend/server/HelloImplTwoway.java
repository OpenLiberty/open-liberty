/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package web.endtoend.server;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.jws.WebService;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;
import web.endtoend.client.endtoend.HelloImplTwowayService;

@WebService(wsdlLocation="WEB-INF/wsdl/HelloImplTwowayService.wsdl")
public class HelloImplTwoway{
    public String sayHello(String vote, int expectedDirection){
        System.out.println("sayHello("+vote+", "+expectedDirection+")");
        if (expectedDirection < 0) {
            return "Hello, this is HelloImplTwowayService";
        } else {
            boolean result = false;
            try {
                if(vote.endsWith("clear")) {
                    XAResourceImpl.clear();
                }
                final ExtendedTransactionManager TM = TransactionManagerFactory
                        .getTransactionManager();
                final Serializable xaResInfo = XAResourceInfoFactory
                        .getXAResourceInfo();
                XAResourceImpl xaRes;
                if (vote.startsWith("rollback")) {
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
                return "Catch XAResourceNotAvailableException:" + e.toString();
            } catch (IllegalStateException e) {
                return "Catch IllegalStateException:" + e.toString();
            } catch (RollbackException e) {
                return "Catch RollbackException:" + e.toString();
            } catch (SystemException e) {
                return "Catch SystemException:" + e.toString();
            }
            return "Enlist XAResourse result: " + result;
        }
    }

    public String callAnother(String URL, String vote1, String vote2, int expectedDirection){
        System.out.println("callAnother("+URL+", "+vote1+", "+vote2+", "+expectedDirection+")");
        boolean result = false;
        try {
            final ExtendedTransactionManager TM = TransactionManagerFactory
                    .getTransactionManager();
            final Serializable xaResInfo = XAResourceInfoFactory
                    .getXAResourceInfo();
            XAResourceImpl xaRes;
            if (vote1.equals("rollback")) {
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
            return "Catch XAResourceNotAvailableException before the"
                    + " second web service call:" + e.toString();
        } catch (IllegalStateException e) {
            return "Catch XAResourceNotAvailableException before the"
                    + " second web service call:" + e.toString();
        } catch (RollbackException e) {
            return "Catch XAResourceNotAvailableException before the"
                    + " second web service call:" + e.toString();
        } catch (SystemException e) {
            return "Catch XAResourceNotAvailableException before the"
                    + " second web service call:" + e.toString();
        }
        if (result == true) {
            URL wsdlLocation;
            try {
                wsdlLocation = new URL(URL
                        + "/endtoend/HelloImplTwowayService?wsdl");
                HelloImplTwowayService service = new HelloImplTwowayService(
                        wsdlLocation);
                web.endtoend.client.endtoend.HelloImplTwoway proxy = service.getHelloImplTwowayPort();
                final Map<String, Object> requestContext = ((BindingProvider) proxy).getRequestContext();
                requestContext.put(
                        "javax.xml.ws.service.endpoint.address",
                        URL + "/endtoend/HelloImplTwowayService");
                TxTestUtils.setTimeouts(requestContext);
                String response = proxy.sayHello(vote2, expectedDirection);
                System.out.println("Reply from the second call: "
                        + response);
                if (!response.equals("Enlist XAResourse result: true")){
                    return "Cannot get the expected result in the second call, "
                            + "expect 'Enlist XAResourse result: true', but get"
                            + response;
                } else {
                    return "Get expected result in the second call.";
                }
            } catch (MalformedURLException e) {
                return "Catch MalformedURLException during the"
                        + " second web service call:" + e.toString();
            }
        } else {
            return "Enlist XAResourse result: false."
                    + " There is no second web service call.";
        }
    }
}
