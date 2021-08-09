/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AsyncTestServlet")
public class AsyncTestServlet extends FATServlet {
    private final static Logger _log = Logger.getLogger(AsyncTestServlet.class.getName());
    final static int TIMEOUT = 15;
    final static int MULTISTAGE_TIMEOUT = 120;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/asyncApp/";

    @Resource
    ExecutorService executor;

    @Override
    public void before() {
        assertNotNull(executor);
        App.executorService.compareAndSet(null, executor);
        BankAccountService.setBalance(1300.75);
    }

    @Override
    public void after() {
        assertNotNull(executor);
        App.executorService.compareAndSet(executor, null);
    }

    /**
     * Tests multi-stage CompletionStage (async) from a Rest Client.
     * Tests baseUri API.
     * 
     */
    @Test
    public void testMultiStageAsyncRestClientMethod(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        AccountsPayableClient acctsPayable = RestClientBuilder.newBuilder()
                        .register(AccountInfoListReaderWriter.class)
                        .register(DoubleReader.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(AccountsPayableClient.class);
        BankAccountClient bank = RestClientBuilder.newBuilder()
                        .register(DoubleReader.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(BankAccountClient.class);

        List<AccountInfo> paidInFull = Collections.synchronizedList(new ArrayList<>());
        DoubleAdder stillOwe = new DoubleAdder();
        AtomicBoolean timedOut = new AtomicBoolean(false);
        AtomicInteger numOfAccounts = new AtomicInteger(0);
        CompletionStage<List<AccountInfo>> cs = acctsPayable.getAllAccounts();
        CompletionStage<Void> cs2 = cs.thenAccept(accounts -> {
            if (!numOfAccounts.compareAndSet(0, accounts.size())) {
                _log.info("Unexpected initial value for numOfAccounts: " + numOfAccounts.get());
                numOfAccounts.set(-1);
            }
            _log.info("payAccountsThatWeCanPay - applying payment to " + accounts.size() + " accounts");
            
            CountDownLatch latch = new CountDownLatch(accounts.size());
            accounts.forEach(acct -> {
                try {
                    _log.info("checking to see if we can pay off account " + acct);
                    acctsPayable.checkBalance(acct.getAccountNumber()).thenCombine(bank.currentBalance(), (owed, balance) -> {
                        _log.info("payAccountsThatWeCanPay we owe " + owed + " to account " + acct.accountNumber);
                        if (balance > owed) {
                            try {
                                Double d = acctsPayable.pay(acct.getAccountNumber(), new Payment(owed)).toCompletableFuture().get();
                                if (d > 0) {
                                    stillOwe.add(d);
                                } else {
                                    paidInFull.add(acct);
                                }
                            } catch (InterruptedException | ExecutionException | UnknownAccountException e) {
                                _log.log(Level.SEVERE, "Unexpected error paying account " + acct.getAccountNumber(), e);

                            } catch (InsufficientFundsException e) {
                                stillOwe.add(owed);
                            }
                        } else {
                            stillOwe.add(owed);
                        }
                        latch.countDown();
                        return acct;
                    });
                } catch (UnknownAccountException e) {
                    // Unexpected since we've already checked that we owe money on this account - log and continue.
                    _log.log(Level.SEVERE, "Unexpected error checking balance on account " + acct.getAccountNumber(), e);
                }
            });
            try {
                timedOut.set(!latch.await(MULTISTAGE_TIMEOUT, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        try {
            List<AccountInfo> accts = cs.toCompletableFuture().get(MULTISTAGE_TIMEOUT + 10, TimeUnit.SECONDS);
            accts.forEach(acctInfo -> {_log.info("listAccounts " + acctInfo);});
            cs2.toCompletableFuture().get(MULTISTAGE_TIMEOUT + 10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            ex.printStackTrace();
            fail("Timed out... this most likely indicates a slow test machine...");
        }
        _log.info("Paid off " + paidInFull.size() + " accounts.  Still owe: " + stillOwe.sum());
        assertEquals("Timed out waiting for response", Boolean.FALSE, timedOut.get());
        assertEquals(5, numOfAccounts.get());
        assertEquals(4, paidInFull.size());
        assertEquals(2287.35, stillOwe.sum(), 0.0);
    }

    /**
     * Tests RestClientBuilderListener is executed for new Rest Clients.
     */
    @Test
    public void testRestClientBuilderListener(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        AccountsPayableClient acctsPayable = RestClientBuilder.newBuilder()
                        .register(AccountInfoListReaderWriter.class)
                        .register(DoubleReader.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(AccountsPayableClient.class);
        BankAccountClient bank = RestClientBuilder.newBuilder()
                        .register(DoubleReader.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(BankAccountClient.class);

        List<String> uris = Collections.synchronizedList(new ArrayList<>());
        Handler auditLog = new Handler(){

            @Override
            public void publish(LogRecord record) {
                uris.add(record.getMessage());
            }

            @Override
            public void flush() {
                //no-op
            }

            @Override
            public void close() throws SecurityException {
                // no-op
            }};
        RestClientAuditLogger._log.addHandler(auditLog);

        acctsPayable.getAllAccounts().toCompletableFuture().get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, uris.size());
        assertTrue(uris.get(0).contains("/accountsPayable/accounts"));

        bank.currentBalance().toCompletableFuture().get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(2, uris.size());
        assertTrue(uris.get(1).contains("/bank"));

        acctsPayable.getAllAccounts().toCompletableFuture().get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(3, uris.size());
        assertTrue(uris.get(2).contains("/accountsPayable/accounts"));
        assertTrue("UniqueURIFilter not invoked", !uris.get(0).equals(uris.get(2)));
    }

    @Test
    public void testCompletionStageCompletesExceptionally(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BankAccountClient bank = RestClientBuilder.newBuilder()
                        .register(DoubleReader.class)
                        .register(InsufficientFundsExceptionMapper.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(BankAccountClient.class);
        CountDownLatch latch = new CountDownLatch(1);
        CompletionStage<Double> cs = bank.withdraw(500000.00);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        Double exceptionValue = cs.exceptionally(t -> {
            _log.log(Level.INFO, "Expected exception", t);
            exception.set(t);
            latch.countDown();
            return -1.0;
        }).toCompletableFuture().get(TIMEOUT, TimeUnit.SECONDS);

        try {
            Double responseValue = cs.thenApply(d -> {
                _log.info("Unexpectedly, this withdrawal worked... " + d);
                latch.countDown();
                return d;
            }).toCompletableFuture().get(TIMEOUT, TimeUnit.SECONDS);
            fail("Failed to throw expected exception");
        } catch (ExecutionException ex) {
            Throwable t = ex.getCause();
            assertEquals(InsufficientFundsException.class.getName(), t.getClass().getName());
        }

        latch.await(TIMEOUT*2, TimeUnit.SECONDS);
        Throwable t = exception.get();
        if (t instanceof CompletionException) {
            t = t.getCause();
        }
        assertNotNull(t);
        assertEquals(InsufficientFundsException.class.getName(), t.getClass().getName());
        assertEquals(-1.0, exceptionValue, 0.0);
    }

    @Test
    public void testCompletionStageWithResponseType() throws Exception {
        final double expected = 28380.79;
        BankAccountService.setBalance(expected);
        BankAccountClient bank = RestClientBuilder.newBuilder()
                        .register(DoubleReader.class)
                        .register(InsufficientFundsExceptionMapper.class)
                        .executorService(executor)
                        .baseUri(URI.create(URI_CONTEXT_ROOT))
                        .build(BankAccountClient.class);
        Double d = bank.currentBalanceResponse().toCompletableFuture().get().readEntity(Double.class);
        assertEquals(expected, d, 0.001);
    }
}