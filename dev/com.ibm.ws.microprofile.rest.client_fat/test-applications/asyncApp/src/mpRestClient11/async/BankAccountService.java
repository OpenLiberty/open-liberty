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

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/bank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankAccountService {
    private final static Logger _log = Logger.getLogger(BankAccountService.class.getName());

    static Double balance;

    static void setBalance(double d) {
        balance = d;
    }

    @GET
    public Double currentBalance() {
        synchronized (balance) {
            _log.info("currentBalance " + balance);
            return balance;
        }
    }

    @DELETE
    @Path("/{amt}")
    public Double withdraw(@PathParam("amt") Double amount) throws InsufficientFundsException {
        synchronized (balance) {
            if (balance < amount) {
                throw new InsufficientFundsException();
            }
            balance = balance - amount;
            _log.info("withdraw " + amount + " " + balance);
            return balance;
        }
    }
}
