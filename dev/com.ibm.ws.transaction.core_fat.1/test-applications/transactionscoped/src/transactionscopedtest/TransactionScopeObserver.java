/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package transactionscopedtest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.TransactionScoped;

@ApplicationScoped
public class TransactionScopeObserver {

    private static boolean seenInitialized = false;
    private static boolean seenDestroyed = false;

    private static boolean seenRequestInitialized = false;
    private static boolean seenRequestDestroyed = false;

    public static boolean hasRequestInitialized() {
        return seenRequestInitialized;
    }

    public static boolean hasRequestDestroyed() {
        return seenRequestDestroyed;
    }

    public static boolean hasSeenInitialized() {
        return seenInitialized;
    }

    public static boolean hasSeenDestroyed() {
        return seenDestroyed;
    }

    public void requestInit(@Observes @Initialized(RequestScoped.class)Object event){
        System.out.println("Observed RequestScoped Initialized");
        seenRequestInitialized = true;
    }

    public void requestDestroyed(@Observes @Destroyed(RequestScoped.class)Object event){
        System.out.println("Observed RequestScoped Initialized");
        seenRequestDestroyed = true;
    }

    public void transactionInit(@Observes @Initialized(TransactionScoped.class)Object event){
        System.out.println("Observed TransactionScoped Initialized");
        seenInitialized = true;
    }

    public void transactionDestroyed(@Observes @Destroyed(TransactionScoped.class)Object event){
        System.out.println("Observed TransactionScoped Initialized");
        seenDestroyed = true;
    }
}
