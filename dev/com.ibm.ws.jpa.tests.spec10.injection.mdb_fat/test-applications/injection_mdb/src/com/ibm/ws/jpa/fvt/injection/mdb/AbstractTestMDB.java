/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injection.mdb;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.JEEExecutionContextHelper;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public abstract class AbstractTestMDB implements MessageListener {
    @Resource
    private MessageDrivenContext context;

    @Resource
    private UserTransaction tx = null;

    protected String beanName = "";

    public void onMessage(Message msg) {
        TestExecutionResult tre = new TestExecutionResult();

        try {
            ObjectMessage om = (ObjectMessage) msg;
            TestExecutionContext ctx = (TestExecutionContext) om.getObject();
            executeTestLogic(ctx);
        } catch (JMSException e) {
            e.printStackTrace();
            tre.setException(e);
        } catch (Throwable t) {
            tre.setException(t);
        } finally {
            TestCoordinator.notifyCompletion(tre);
        }
    }

    @PostConstruct
    protected void postConstruct() {
        try {
            beanName = (String) context.lookup("beanName");
            System.out.println("Bean self identifying with identity \"" + beanName + "\".");
        } catch (Throwable t) {
            // Swallow.  Defining a bean name is not required by the test framework.
        }
    }

    public String getEnvDefinedBeanName() {
        return beanName;
    }

    public void executeTestLogic(TestExecutionContext ctx) {
        TestExecutionResources testExecResources = null;

        try {
            String beanName = getEnvDefinedBeanName();
            if (beanName != null && !beanName.trim().isEmpty()) {
                ctx.getProperties().put("managed.resource.name", beanName);
            }

            JEEExecutionContextHelper.printBeginTestInfo(ctx);

            // Create resources needed by the test
            testExecResources = JEEExecutionContextHelper.processTestExecutionResources(ctx, this, tx);

            // Execute the test
            JEEExecutionContextHelper.executeTestLogic(ctx, testExecResources, this);
        } catch (Throwable t) {
            logException(t, ctx);
        } finally {
            // Cleanup Resources
            try {
                JEEExecutionContextHelper.destroyExecutionResources(testExecResources);
            } catch (Throwable t) {
                // Assert.fail("BMTEJBTestVehicle Cleanup Caught Exception: " + t);
            }

            JEEExecutionContextHelper.printEndTestInfo(ctx);
        }
    }

    protected void logException(Throwable t, TestExecutionContext ctx) throws java.lang.AssertionError {
        if (t instanceof java.lang.AssertionError) {
            throw (java.lang.AssertionError) t;
        }

        String exText = exceptionToString(t);

        StringBuilder sb = new StringBuilder();
        sb.append("\n!!!!!\n");
        sb.append("Test failed with Exception: \n");
        sb.append("  Test Name: ").append(ctx.getName()).append("\n");
        sb.append("  Test Logic Class: ").append(ctx.getTestLogicClassName()).append("\n");
        sb.append("  Test Logic Method: ").append(ctx.getTestLogicMethod()).append("\n");
        sb.append("Exception:\n");
        sb.append(exText).append("\n");
        sb.append("!!!!!\n");
        System.out.println(sb);

        Assert.fail("TestServlet Caught Exception: " + t + "\n" + exText);
    }

    protected String exceptionToString(Throwable t) {
        CharArrayWriter caw = new CharArrayWriter();
        t.printStackTrace(new PrintWriter(caw));
        return caw.toString();
    }
}
