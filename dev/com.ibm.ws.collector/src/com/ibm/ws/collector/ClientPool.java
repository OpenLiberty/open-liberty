/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.ssl.SSLSupport;

/*
 * A fixed size pool of Clients.  Thread safe.
 */
public abstract class ClientPool {

    private static final TraceComponent tc = Tr.register(ClientPool.class);

    // tracks the number of clients we've closed - a value > 0 means pool is closing or closed
    private final AtomicInteger numClientsClosed = new AtomicInteger(0);

    private final int numClients;
    LinkedBlockingDeque<Client> clients;

    @FFDCIgnore(value = { InterruptedException.class })
    public ClientPool(String sslConfig, SSLSupport sslSupport, int numClients) throws SSLException {
        clients = new LinkedBlockingDeque<Client>(numClients);
        this.numClients = numClients;

        for (int i = 0; i < numClients; i++) {
            Client client;

            // creating client may throw SSLException
            client = createClient(sslConfig, sslSupport);

            boolean done = false;
            while (!done) {
                try {
                    clients.put(client);
                    done = true;
                } catch (InterruptedException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "InterruptedException during <init> - will retry");
                    // try again
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "pool created  - " + (numClients - clients.remainingCapacity()) + " of " + numClients + " clients available.");
        }

    }

    /*
     * Checks out a client for use by the caller. This method waits if necessary until a client is available or until pool is closed.
     * May return null if InterruptedException occurs during checkout, or if pool is closed during checkout.
     */
    @FFDCIgnore(value = { InterruptedException.class })
    public Client checkoutClient() {
        Client client = null;

        try {
            // need to poll here as the dequeue can be permanently emptied by close method at any time
            while (client == null && numClientsClosed.get() == 0) {
                client = clients.poll(200, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "InterruptedException during checkout");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            int clientHash = (client != null) ? client.hashCode() : 0;
            Tr.event(tc, "post-checkout - (" + clientHash + ") " + (numClients - clients.remainingCapacity()) + " of " + numClients + " clients available.");
        }
        return client;
    }

    /*
     * Checks in a client.
     */
    @FFDCIgnore(value = { InterruptedException.class })
    public void checkinClient(Client client) {
        boolean done = false;

        //If no more space. Return. Shouldn't be checking more than we have. Shouldn't happen besides unit test anyways.
        if (clients.remainingCapacity() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Checking in more clients than there should be.");
            }
            return;
        }

        while (!done) {
            try {
                //check if this client was already checked-in.
                if (!clients.contains(client)) {
                    clients.putFirst(client);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        int clientHash = (client != null) ? client.hashCode() : 0;
                        Tr.event(tc, "post-checkin  - (" + clientHash + ") " + (numClients - clients.remainingCapacity()) + " of " + numClients + " clients available.");
                    }
                }
                done = true;
            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "InterruptedException during checkin - will retry");
            }
        }

    }

    /*
     * Closes all clients. Waits for checked out clients to be checked in, then closes them too.
     */
    @FFDCIgnore(value = { InterruptedException.class })
    public void close() {
        // remove all the clients from the deque, closing them as we go
        while (numClientsClosed.get() < numClients) {
            try {
                Client client = clients.takeFirst();
                try {
                    client.close();
                } catch (Exception e) {
                    //Ignore this
                }
                numClientsClosed.incrementAndGet();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "pool closed   - " + numClientsClosed + " of " + numClients + " clients closed");
            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "InterruptedException during close - will retry");
            }
        }
    }

    public abstract Client createClient(String sslConfig, SSLSupport sslSupport) throws SSLException;

}
