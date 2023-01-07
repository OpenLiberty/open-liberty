/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.SecurityContext;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextErrorCodes;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.adapter.FVTAdapterImpl;
import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.work.TestTransactionWorkContext;
import com.ibm.adapter.work.TestWorkContext;
import com.ibm.adapter.work.TestWorkContextProviderWorkImpl;
import com.ibm.adapter.work.TestWorkContextProviderWorkImplUsingLatch;

/**
 *
 */
@SuppressWarnings("serial")
public class GWCTestServlet extends HttpServlet {

    /**
     * Test FAT servlet setup
     * 
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */

    ResourceAdapter testRA = null; // The test Resource Adapter object
    private FVTMessageProvider provider = null; // FVT Message provider
    private TestWorkContextProviderWorkImpl work = null;
    private List<WorkContext> wcList = null;
    private final String servletName = this.getClass().getSimpleName();

    /**
     * Message written to servlet to indicate that is has been successfully
     * invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println(" ---> " + servletName + " is starting " + test + "<br>");
        System.out.println(" ---> " + servletName + " is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace();
            out.println(" <--- " + test + " FAILED");
            System.out.println(" <--- " + test + " FAILED");
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * This test verifies if TestWorkContext is supported <br/>
     * The expected outcome is False
     */
    public void testIsTestWorkContextSupported(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        final String method = "testIsTestWorkContextSupported";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }

        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        provider.setResourceAdapter((FVTAdapterImpl) testRA);

        System.out.println("Done setup");

        Class<? extends WorkContext> workContextClass = TestWorkContext.class;
        BootstrapContext bootstrapContext = provider.getBootstrapContext();
        if (!(bootstrapContext.isContextSupported(workContextClass)))
            System.out.println("Test Passed : " + method);
        else
            throw new Exception("Test Failed : " + method);

    }

    /**
     * This test verifies if HintsContext is supported <br/>
     * The expected outcome is that True
     */
    public void testIsHintsContextSupported(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        final String method = "testIsHintsContextSupported";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }

        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        provider.setResourceAdapter((FVTAdapterImpl) testRA);

        System.out.println("Done setup");

        Class<? extends WorkContext> workContextClass = HintsContext.class;
        BootstrapContext bootstrapContext = provider.getBootstrapContext();
        if (bootstrapContext.isContextSupported(workContextClass))
            System.out.println("Test Passed : " + method);
        else
            throw new Exception("Test failed : " + method);
    }

    /**
     * This test verifies if TransactionContext is supported <br/>
     * The expected outcome is that True
     */
    public void testIsTransactionContextSupported(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        final String method = "testIsTransactionContextSupported";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }

        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");
        Class<? extends WorkContext> workContextClass = TransactionContext.class;
        BootstrapContext bootstrapContext = provider.getBootstrapContext();
        if (bootstrapContext.isContextSupported(workContextClass))
            System.out.println("Test Passed : " + method);
        else
            throw new Exception("Test Failed : " + method);

    }

    /**
     * This test verifies if SecurityWorkContext is supported <br/>
     * The expected outcome is that True
     */

    public void testIsSecurityContextSupported(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        final String method = "testIsSecurityContextSupported";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        Class<? extends WorkContext> workContextClass = SecurityContext.class;
        BootstrapContext bootstrapContext = provider.getBootstrapContext();
        if (bootstrapContext.isContextSupported(workContextClass))
            System.out.println("Test Passed : " + method);
        else {
            throw new Exception("Test Failed :" + method);
        }
    }

    /**
     * This test verifies that not null ExecutionContext and work cannot be
     * passed to WorkManager <br/>
     * The expected outcome is WorkRejectedException
     */
    public void testExecutionContextNotNulldoWork(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        final String method = "testExecutionContextNotNulldoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();

            TestWorkContext testWC = new TestWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);

            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // Initiate an executionContext
            ExecutionContext ec = new ExecutionContext();
            // submit the work to work manager with doWork()
            try {
                wm.doWork(work, WorkManager.INDEFINITE, ec, null);
            } catch (WorkRejectedException wrex) {
                System.out.println("Test Passed : " + method + " - "
                                   + "WorkRejected Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkRejectedException is not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }

    }

    /**
     * This test verifies that not null ExecutionContext cannot be passed to
     * WorkManager <br/>
     * The expected outcome is WorkRejectedException
     */
    public void testExecutionContextNotNullstartWork(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testExecutionContextNotNullstartWork";
        System.out.println("Entering " + method);

        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // Initiate an executionContext
            ExecutionContext ec = new ExecutionContext();
            try {
                // submit the work to work manager with startWork()
                wm.startWork(work, WorkManager.INDEFINITE, ec, null);
            } catch (WorkRejectedException wrex) {
                System.out.println("Test Passed : " + method + " - "
                                   + "WorkRejected Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkRejectedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that not null ExecutionContext cannot be passed to
     * WorkManager <br/>
     * The expected outcome is WorkRejectedException
     */
    public void testExecutionContextNotNullscheduleWork(
                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testExecutionContextNotNullscheduleWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");
        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // Initiate an executionContext
            ExecutionContext ec = new ExecutionContext();
            try {
                // submit the work to work manager with scheduleWork()
                wm.scheduleWork(work, WorkManager.INDEFINITE, ec, null);
            } catch (WorkRejectedException wrex) {
                System.out.println("Test Passed : " + method + " - "
                                   + "WorkRejected Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkRejectedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission (doWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedWorkContextdoWork(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedWorkContextdoWork";
        System.out.println("Entering " + method);

        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
            } catch (WorkCompletedException wcex) {
                if (WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE == wcex.getErrorCode())
                    System.out.println("Test Passed : " + method + " - "
                                       + "WorkCompleted Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkcompletedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission (startWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedWorkContextstartWork(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedWorkContextstartWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);

            CountDownLatch latch = new CountDownLatch(1);

            // create a work
            work = new TestWorkContextProviderWorkImplUsingLatch(method, wcList, latch);
            // submit the work to work manager with startWork()
            wm.startWork(work, WorkManager.INDEFINITE, null, provider.getWorkDispatcher());

            System.out.println("About to invoke countDown on the latch.");
            latch.countDown();
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission
     * (scheduleWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedWorkContextscheduleWork(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedWorkContextscheduleWork";
        System.out.println("Entering " + method);

        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.scheduleWork(work, WorkManager.INDEFINITE, null, null);

        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission (doWork)
     * even if another context is supported <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedAndSupportedWorkContextdoWork(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedAndSupportedWorkContextdoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
            } catch (WorkCompletedException wcex) {
                if (WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE == wcex.getErrorCode())
                    System.out.println("Test Passed : " + method + " - "
                                       + "WorkCompleted Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkCompletedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission (startWork)
     * even if another context is supported <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedAndSupportedWorkContextstartWork(
                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedAndSupportedWorkContextstartWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);
            wcList.add(txWC);

            CountDownLatch latch = new CountDownLatch(1);

            // create a work
            work = new TestWorkContextProviderWorkImplUsingLatch(method, wcList, latch);
            // submit the work to work manager with startWork()
            wm.startWork(work, WorkManager.INDEFINITE, null, provider.getWorkDispatcher());

            System.out.println("About to invoke countDown on the latch.");
            latch.countDown();
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies unsupported context fails Work submission
     * (scheduleWork) even if another context is supported <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testUnsupportedAndSupportedWorkContextscheduleWork(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String method = "testUnsupportedAndSupportedWorkContextscheduleWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestWorkContext testWC = new TestWorkContext();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testWC);
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.scheduleWork(work, WorkManager.INDEFINITE, null, null);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that duplicate WorkContexts fail Work sumbission
     * (doWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testDupWorkContextdoWork(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {
        final String method = "testDupWorkContextdoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
            } catch (WorkCompletedException wcex) {
                if (WorkContextErrorCodes.DUPLICATE_CONTEXTS == wcex.getErrorCode())
                    System.out.println("Test Passed : " + method + " - "
                                       + "WorkCompleted Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkCompletedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that duplicate WorkContexts fail Work sumbission
     * (startWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testDupWorkContextstartWork(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        final String method = "testDupWorkContextstartWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            wcList.add(txWC);

            CountDownLatch latch = new CountDownLatch(1);

            // create a work
            work = new TestWorkContextProviderWorkImplUsingLatch(method, wcList, latch);
            // submit the work to work manager with startWork()
            wm.startWork(work, WorkManager.INDEFINITE, null, provider.getWorkDispatcher());

            System.out.println("About to invoke countDown on the latch.");
            latch.countDown();
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that duplicate WorkContext are fail Work sumbission
     * (sscheduleWork) <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testDupWorkContextscheduleWork(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        final String method = "testDupWorkContextscheduleWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.scheduleWork(work, WorkManager.INDEFINITE, null, null);

        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that duplicate WorkContexts fail Work sumbission
     * (doWork) One duplicate is a subclass of another. <br/>
     * The expected outcome is WorkCompletedException
     */
    public void testDupWorkContextdoWorkSub(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        final String method = "testDupWorkContextdoWorkSub";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            TestTransactionWorkContext testTxWC = new TestTransactionWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            wcList.add(testTxWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
            } catch (WorkCompletedException wcex) {
                if (WorkContextErrorCodes.DUPLICATE_CONTEXTS == wcex.getErrorCode())
                    System.out.println("Test Passed : " + method + " - "
                                       + "WorkCompleted Exception is thrown as expected");
                return;
            }
            String exception_message = "WorkCompletedException not thrown as expected";
            throw new Exception("Test Failed : " + method + " - "
                                + exception_message);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (doWork) for supported context <br/>
     */
    public void testTxWorkContextdoWork(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextdoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
                System.out.println("Test Passed : " + method + " - "
                                   + "UnExpectedException is not thrown");
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
                String exception_message = ("UnExpectedException is thrown : " + ex);
                throw new Exception("Test Failed : " + method + " - "
                                    + exception_message);
            }
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (startWork) for supported context <br/>
     */
    public void testHintsWorkContextdoWork(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        final String method = "testHintsWorkContextdoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            HintsContext hintsWC = new HintsContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(hintsWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
                System.out.println("Test Passed : " + method + " - "
                                   + "UnExpectedException is not thrown");
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
                String exception_message = ("UnExpectedException is thrown : " + ex);
                throw new Exception("Test Failed : " + method + " - "
                                    + exception_message);
            }

        } catch (Throwable ex) {
            throw ex;
        }

    }

    /**
     * This test verifies that Work submission (startWork) for supported context <br/>
     */
    public void testTxWorkContextstartWork(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextstartWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.startWork(work, WorkManager.INDEFINITE, null, null);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (scheduleWork) for supported
     * context <br/>
     */
    public void testTxWorkContextscheduleWork(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextscheduleWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TransactionContext txWC = new TransactionContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(txWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.scheduleWork(work, WorkManager.INDEFINITE, null, null);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (doWork) for context that is a
     * subclasss of supported context <br/>
     */
    public void testTxWorkContextSubdoWork(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextSubdoWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestTransactionWorkContext testTxWC = new TestTransactionWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testTxWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            try {
                // submit the work to work manager with scheduleWork()
                wm.doWork(work, WorkManager.INDEFINITE, null, null);
                System.out.println("Test Passed : " + method + " - "
                                   + "UnExpectedException is not thrown");
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
                String exception_message = ("UnExpectedException is thrown : " + ex);
                throw new Exception("Test Failed : " + method + " - "
                                    + exception_message);
            }
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (startWork) for context that is a
     * subclasss of supported context <br/>
     */
    public void testTxWorkContextSubstartWork(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextSubstartWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestTransactionWorkContext testTxWC = new TestTransactionWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testTxWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.startWork(work, WorkManager.INDEFINITE, null, null);
        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * This test verifies that Work submission (scheduleWork) for context that
     * is a subclasss of supported context <br/>
     */
    public void testTxWorkContextSubscheduleWork(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        final String method = "testTxWorkContextSubscheduleWork";
        System.out.println("Entering " + method);
        try {
            provider = (FVTMessageProvider) new InitialContext().lookup("fvtProvider");
        } catch (NamingException ne) {
            Throwable t = ne.getRootCause() != null ? ne.getRootCause() : ne;
            t.printStackTrace(System.out);
            throw ne;
        }
        try {
            testRA = (ResourceAdapter) FVTAdapterImpl.raName.get("adapter_jca16_gwc_GenericWorkContextTestRAR");

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        provider.setResourceAdapter((FVTAdapterImpl) testRA);
        System.out.println("Done setup");

        try {
            // get the work manager
            WorkManager wm = provider.getWorkManager();
            TestTransactionWorkContext testTxWC = new TestTransactionWorkContext();
            wcList = new ArrayList<WorkContext>();
            wcList.add(testTxWC);
            // create a work
            work = new TestWorkContextProviderWorkImpl(method, wcList);
            // submit the work to work manager with scheduleWork()
            wm.scheduleWork(work, WorkManager.INDEFINITE, null, null);
        } catch (Throwable ex) {
            throw ex;
        }
    }

}
