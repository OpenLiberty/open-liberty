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

import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/accountsPayable")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AccountsPayableClient {

    @GET
    @Path("/accounts")
    CompletionStage<List<AccountInfo>> getAllAccounts();
    
    @GET
    @Path("/accountInfo")
    CompletionStage<AccountInfo> accountDetails(@QueryParam("acct") String acctNumber) throws UnknownAccountException;
    
    @GET
    @Path("/accountBalance")
    CompletionStage<Double> checkBalance(@QueryParam("acct")String acctNumber) throws UnknownAccountException;
    
    @POST
    @Path("/pay")
    CompletionStage<Double> /*remaining balance*/ pay(@QueryParam("acct")String acctNumber, Payment payment) 
        throws UnknownAccountException, InsufficientFundsException;
    
}
