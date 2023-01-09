/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.testtooling.vehicle.web;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.JEEExecutionContextHelper;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

import componenttest.app.FATServlet;

public abstract class JPATestServlet extends FATServlet {
    private static final long serialVersionUID = -4038309130483462162L;

    protected static int portNumber = 0;

    public final static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    public final static ObjectName fatServerInfoMBeanObjectName;

    static {
        ObjectName on = null;
        try {
            on = new ObjectName("WebSphereFAT:name=ServerInfo");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            fatServerInfoMBeanObjectName = on;
        }
    }

    protected Set<String> getInstalledFeatures() {
        HashSet<String> retVal = new HashSet<String>();

        try {
            Set<String> instFeatureSet = (Set<String>) mbeanServer.getAttribute(fatServerInfoMBeanObjectName, "InstalledFeatures");
            if (instFeatureSet != null) {
                retVal.addAll(instFeatureSet);
            }
        } catch (Throwable t) {
        }
        return retVal;
    }

    protected boolean isUsingJPA20Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.0");
    }

    protected boolean isUsingJPA21Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.1");
    }

    protected boolean isUsingJPA22Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.2");
    }

    protected boolean isUsingJPA21ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.1"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.1");
    }

    protected boolean isUsingJPA22ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.2"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.2");
    }

    protected final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();
    protected String testClassName;

    @Resource
    protected UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        portNumber = request.getLocalPort();

        super.doGet(request, response);
    }

    protected void executeTest(String testName, String testMethod, String testResource) throws Exception {
        executeTest(testName, testMethod, testResource, null);
    }

    protected void executeTest(String testName, String testMethod, String testResource, Map<String, java.io.Serializable> props) throws Exception {
        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);

        executeTest(testName, testMethod, testResourcesList, props);
    }

    protected void executeTest(String testName, String testMethod, Map<String, String> testResourcesList, Map<String, java.io.Serializable> props) throws Exception {
        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        for (Map.Entry<String, String> entry : testResourcesList.entrySet()) {
            jpaPCInfoMap.put(entry.getKey(), jpaPctxMap.get(entry.getValue()));
        }

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        if (props != null && !props.isEmpty()) {
            properties.putAll(props);
        }

        executeTestVehicle(testExecCtx);
    }

    protected void executeTestVehicle(TestExecutionContext ctx) {
        TestExecutionResources testExecResources = null;

        try {
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
                Assert.fail("TestServlet Cleanup Caught Exception: " + t);
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
