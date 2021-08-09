/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.nested.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.async.fat.nested.ejb.NestedAsyncMethodsLocal;
import com.ibm.ws.ejbcontainer.async.fat.nested.ejb.NestedAsyncThreadCompareLocal;
import com.ibm.ws.ejbcontainer.async.fat.nested.ejb.NestedAsyncTimeoutBean;
import com.ibm.ws.ejbcontainer.async.fat.nested.ejb.SleepTimeoutBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/NestedAsyncServlet")
public class NestedAsyncServlet extends FATServlet {
    private final static String CLASSNAME = NestedAsyncServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private final static String APPLICATION = "NestedAsyncTestApp";
    private final static String MODULE = "NestedAsyncEJB";

    private NestedAsyncMethodsLocal lookupAsyncMethodsBean() throws NamingException {
        return (NestedAsyncMethodsLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(NestedAsyncMethodsLocal.class.getName(), APPLICATION,
                                                                                     MODULE, "NestedAsyncMethodsBean");
    }

    private NestedAsyncThreadCompareLocal lookupAsyncBean() throws NamingException {
        return (NestedAsyncThreadCompareLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(NestedAsyncThreadCompareLocal.class.getName(), APPLICATION,
                                                                                           MODULE, "NestedAsyncThreadCompareBean");
    }

    private NestedAsyncTimeoutBean lookupTimeoutBean() throws NamingException {
        return (NestedAsyncTimeoutBean) FATHelper.lookupDefaultBindingEJBJavaGlobal(NestedAsyncTimeoutBean.class.getName(), APPLICATION,
                                                                                    MODULE, "NestedAsyncTimeoutBean");
    }

    /**
     * This test verifies that nested asynchronous methods are in fact executed
     * asynchronously. The outermost method is a Fire-and-Return async method,
     * the inner method is a Fire-and-Forget async method, and innermost is also
     * a Fire-and-Forget async method.
     *
     * @throws Exception
     */
    @Test
    public void testNestedAsyncMethods() throws Exception {
        NestedAsyncMethodsLocal bean = lookupAsyncMethodsBean();

        bean.setupTestLatch();

        Future<String> futureMsg = bean.outerFARMethod("Aloha");

        try {
            String msg = futureMsg.get(5, TimeUnit.SECONDS);
            assertEquals("--> Expected string: Aloha. Received string:" + msg,
                         "Aloha", msg);
        } catch (TimeoutException e) {
            e.printStackTrace();
            fail("--> The async method took too long to return a result, caught a TimeoutException: "
                 + e);
        }

        try {
            ArrayList<Integer> result = bean.getAsyncCallOrder1();

            int compareValue;
            for (int i = 0; i < 3; i++) {
                compareValue = result.get(i).intValue();
                svLogger.info("--> result[" + i + "] = " + compareValue);
                if (compareValue != i) {
                    fail("--> The order of the async methods is incorrect: result["
                         + i + ") = " + compareValue);
                }
            }
        } finally {
            bean.testReset();
            svLogger.info("--> bean.testReset was called and bean.getAsyncCallOrder1() returns: "
                          + bean.getAsyncCallOrder1()
                          + " with a size of "
                          + bean.getAsyncCallOrder1().size());
            assertEquals("--> Verify that the Array list was reset, bean.getAsyncCallOrder1() returns: "
                         + bean.getAsyncCallOrder1() + " with a size of "
                         + bean.getAsyncCallOrder1().size(), 0, bean.getAsyncCallOrder1().size());
        }
    }

    /**
     * This test verifies that nested asynchronous methods are executed
     * asynchronously and all the async methods have unique ThreadIDs. The test's
     * ThreadID is verified to be unique from all the async method ThreadIDs. The
     * outermost method is a Fire-and-Return async method, the inner method is a
     * Fire-and-Forget async method, and innermost is also a Fire-and-Forget
     * async method.
     *
     * @throws Exception
     */
    @Test
    public void testNestedAsyncThreadCompare() throws Exception {
        NestedAsyncThreadCompareLocal bean = lookupAsyncBean();

        Future<ArrayList<Long>> futureAsyncBeanResult = bean.outerFARMethod();

        long testThreadID = Thread.currentThread().getId();

        try {
            ArrayList<Long> asyncMethodThreadIdList = futureAsyncBeanResult.get(
                                                                                10, TimeUnit.SECONDS);
            assertNotNull("--> The returned Future object from the bean, asyncMethodThreadIdList, "
                          + "should not be null. Check logs for bean method output for debug help.",
                          asyncMethodThreadIdList);

            if (asyncMethodThreadIdList.size() != 3) {
                fail("--> The size of the asyncMethodThreadIdList should be 3, size = "
                     + asyncMethodThreadIdList.size());
            }

            svLogger.info("--> testThreadID = " + testThreadID);
            for (int i = 0; i < asyncMethodThreadIdList.size(); i++) {
                svLogger.info("--> asyncMethodThreadIdList[" + i + "] = "
                              + asyncMethodThreadIdList.get(i));
            }
            assertTrue("--> ThreadIDs for async methods and test are all unique.",
                       (asyncMethodThreadIdList.get(0).longValue() != testThreadID
                        && asyncMethodThreadIdList.get(1).longValue() != testThreadID
                        && asyncMethodThreadIdList.get(2).longValue() != testThreadID
                        && asyncMethodThreadIdList.get(0).longValue() != asyncMethodThreadIdList.get(1).longValue()
                        && asyncMethodThreadIdList.get(0).longValue() != asyncMethodThreadIdList.get(2).longValue()
                        && asyncMethodThreadIdList.get(1).longValue() != asyncMethodThreadIdList.get(2).longValue()));

        } catch (TimeoutException e) {
            e.printStackTrace();
            fail("--> The async method took too long to return a result, caught a TimeoutException: "
                 + e);
        } finally {
            ArrayList<Long> verifyThreadIDListReset = bean.testReset();
            svLogger.info("--> bean.testReset was called and returned: "
                          + verifyThreadIDListReset + " with a size of "
                          + verifyThreadIDListReset.size());
            assertEquals(
                         "--> Verify that the Array list was reset, bean.testReset was called and returned: "
                         + verifyThreadIDListReset
                         + " with a size of "
                         + verifyThreadIDListReset.size(), 0,
                         verifyThreadIDListReset.size());
        }
    }

    /**
     * This test verifies that nested asynchronous methods honor timeout values passed to Future.get().
     * If A -> B -> C
     * - C hangs
     * - A timeout > B timeout
     * B should timeout first and return to A, previously we did not time out until A.
     *
     *
     * See also PI96086
     *
     * @throws Exception
     */
    @Test
    public void testNestedAsyncTimeout() throws Exception {
        NestedAsyncTimeoutBean bean = lookupTimeoutBean();
        SleepTimeoutBean.svTestLatch = new CountDownLatch(1);
        Future<String> futureMsg = bean.testFutureTimeout();

        try {
            svLogger.info("Top level future.get()");
            futureMsg.get(90, TimeUnit.SECONDS);
            SleepTimeoutBean.svTestLatch.countDown();
        } catch (TimeoutException e) {
            Assert.fail("Top level future.get() timed out, we're not passing nested future.get timeout values correctly");
        } catch (Exception e) {
            // unexpected exception;
            Assert.fail("testNestedAsyncTimeout unexpected exception: " + e.getMessage());
        }
    }
}
