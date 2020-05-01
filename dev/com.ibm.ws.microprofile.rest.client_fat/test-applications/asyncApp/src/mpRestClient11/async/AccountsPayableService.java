/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.async;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Path("/accountsPayable")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountsPayableService {
    private final static Logger _log = Logger.getLogger(AccountsPayableService.class.getName());
    
    static final Map<String,AccountInfo> accountInfos = new ConcurrentHashMap<>();
    static final Map<String,Double> accountBalances = new ConcurrentHashMap<>();
    static final BankAccountClient bankAccountClient;
    
    static {
        accountInfos.put("12300567", new AccountInfo("abc123", "12300567"));
        accountInfos.put("12300678", new AccountInfo("xyz789", "12300678"));
        accountInfos.put("12300946", new AccountInfo("qrs456", "12300946"));
        accountInfos.put("12300444", new AccountInfo("mno852", "12300444"));
        accountInfos.put("12300963", new AccountInfo("ijk147", "12300963"));
        
        accountBalances.put("12300567", 560.50);
        accountBalances.put("12300678", 300.75);
        accountBalances.put("12300946", 50.25);
        accountBalances.put("12300444", 250.00);
        accountBalances.put("12300963", 2287.35);
        
        bankAccountClient = RestClientBuilder.newBuilder()
                                             .baseUri(URI.create(AsyncTestServlet.URI_CONTEXT_ROOT))
                                             .executorService(App.executorService.get())
                                             .build(BankAccountClient.class);
    }
    
    private static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
    }
    
    private static final boolean isAIX() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("AIX".toLowerCase())) {
            return true;
        }
        return false;
    }
    
    @GET
    @Path("/accounts")
    public List<AccountInfo> getAllAccounts() {
        _log.info("getAllAccounts " + accountInfos.values());
        return Arrays.asList(accountInfos.values().toArray(new AccountInfo[]{}));
    }
    
    @GET
    @Path("/accountInfo")
    public AccountInfo accountDetails(@QueryParam("acct") String acctNumber) throws UnknownAccountException {
        AccountInfo info = accountInfos.get(acctNumber);
        _log.info("accountDetails " + acctNumber + " " + info);
        if (info == null) {
            throw new UnknownAccountException();
        }
        return info;
    }
    
    @GET
    @Path("/accountBalance")
    public Double checkBalance(@QueryParam("acct")String acctNumber) throws UnknownAccountException {
        Double balance = accountBalances.get(acctNumber);
        _log.info("checkBalance " + acctNumber + " " + balance);
        if (balance == null) {
            throw new UnknownAccountException();
        }
        return balance;
    }
    
    @POST
    @Path("/pay")
    public Double pay(@QueryParam("acct")String acctNumber, Payment payment) throws UnknownAccountException, InsufficientFundsException {
        
        int myTimeout = AsyncTestServlet.TIMEOUT;
        if (isAIX()) {
            myTimeout = AsyncTestServlet.TIMEOUT * 2;
        }
        if (isZOS()) {
            myTimeout = AsyncTestServlet.TIMEOUT * 3;
        }
        
        Double balance = accountBalances.get(acctNumber);
        if (balance == null) {
            throw new UnknownAccountException();
        }

        Double paymentAmt = payment.getAmount();
        try {
            Double remainingBalanceInAccount = bankAccountClient.withdraw(paymentAmt)
                                                                .toCompletableFuture()
                                                                .get(myTimeout, TimeUnit.SECONDS);
            _log.info("balance remaining in bank after withdrawal: " + remainingBalanceInAccount);
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            Throwable t = ex.getCause();
            if (t != null && t instanceof InsufficientFundsException) {
                throw (InsufficientFundsException) t;
            }
            _log.log(Level.WARNING, "Caught unexpected exception: " + ex + " with cause " + t);
        }

        Double remainingBalance = balance - paymentAmt;
        accountBalances.put(acctNumber, remainingBalance);
        _log.info("pay " + acctNumber + " " + remainingBalance);
        return remainingBalance;
    }
}
