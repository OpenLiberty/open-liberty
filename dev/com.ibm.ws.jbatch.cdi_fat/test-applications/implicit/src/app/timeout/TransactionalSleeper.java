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
package app.timeout;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;

@Dependent
public class TransactionalSleeper implements Serializable {

    private static final long serialVersionUID = 3971396184621605067L;
    private final static Logger logger = Logger.getLogger("test");

    /**
     *
     * Call a method with the CDI container starting a global tran around it via Interceptor, and
     * sleeping within method impl.
     *
     * @param sleepSeconds Number of seconds to sleep for.
     */
    @Transactional
    public void sleepyTran(int sleepSeconds) {

        logger.info("Entered sleepyTran(), will sleep: " + sleepSeconds + " number of seconds");
        logTranStatus();
        try {
            Thread.sleep(sleepSeconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Just for logging purposes
     */
    public void logTranStatus() {
        TransactionSynchronizationRegistry tranRegistry = null;
        String jndiLoc = "java:comp/TransactionSynchronizationRegistry";

        InitialContext ctxt;
        try {
            ctxt = new InitialContext();
            tranRegistry = (TransactionSynchronizationRegistry) ctxt.lookup(jndiLoc);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }

        logger.info("Checking tran, status = " + tranRegistry.getTransactionStatus());
    }

}
