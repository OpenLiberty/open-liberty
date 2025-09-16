/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.platformtck.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import org.junit.Ignore;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PlatformTCK32")
public class PlatformTCKServlet extends FATServlet {
    @Inject
    private EntityManager defaultEM; // Default TransactionScoped
    
    // Direct persistence context as a fallback
    @PersistenceContext(unitName = "PlatformTCKPersistenceUnit")
    private EntityManager directEM;

    @Inject
    @ShortScoped
    private EntityManager shortScopedEM; // Dependent scope (shorter than TransactionScoped)

    @Inject
    @LongScoped
    private EntityManager longScopedEM; // ApplicationScoped (longer than TransactionScoped)

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }
@Test
public void testEntityManagerInjection() throws Exception {
    // Start a transaction since the default EntityManager is TransactionScoped
    tx.begin();
    try {
        assertNotNull(defaultEM);
        System.out.println("EntityManager injected via @Inject");
        assertTrue(defaultEM.isOpen());
        System.out.println("EntityManager @Inject test passed!");
    } finally {
        tx.commit();
    }
}

@Test
public void testEntityManagerWithShorterScope() throws Exception {
    assertNotNull(shortScopedEM);
    assertTrue(shortScopedEM.isOpen());
    System.out.println("Short-scoped EntityManager injected successfully");
    
    // Start a transaction
    tx.begin();
    
    // Use the EntityManager
    shortScopedEM.clear();
    System.out.println("Short-scoped EntityManager used within transaction");
    
    // Get a reference to the current EntityManager
    EntityManager em1 = shortScopedEM;
    
    // In Dependent scope, each injection point gets a new instance
    // So we need to verify that the EntityManager is still usable within the transaction
    assertTrue(em1.isOpen());
    System.out.println("Short-scoped EntityManager is open within transaction");
    
    // End the transaction
    tx.commit();
    System.out.println("Transaction committed");
    
    // The EntityManager should still be open after the transaction
    // because it's not bound to the transaction scope
    assertTrue(shortScopedEM.isOpen());
    System.out.println("Short-scoped EntityManager is still open after transaction");  
    System.out.println("Short-scoped EntityManager test passed!");
}

@Test
public void testDirectEntityManagerInjection() {
    assertNotNull(directEM);
    System.out.println("EntityManager injected via @PersistenceContext");
    assertTrue(directEM.isOpen());
    System.out.println("Direct EntityManager test passed!");
}

@Test
public void testEntityManagerWithLongerScope() throws Exception {
    assertNotNull(longScopedEM);
    assertTrue(longScopedEM.isOpen());
    System.out.println("Long-scoped EntityManager injected successfully");
    
    // Start a transaction
    tx.begin();
    
    // Use the EntityManager
    longScopedEM.clear();
    System.out.println("Long-scoped EntityManager used within transaction");
    
    // End the transaction
    tx.commit();
    System.out.println("Transaction committed");
    
    // The EntityManager should still be open after the transaction
    // because it's ApplicationScoped (longer than TransactionScoped)
    assertTrue(longScopedEM.isOpen());
    System.out.println("Long-scoped EntityManager is still open after transaction");
    
    // Start another transaction to verify the EntityManager is still usable
    tx.begin();
    longScopedEM.clear();
    tx.commit();
    System.out.println("Long-scoped EntityManager used in a second transaction");
    
    System.out.println("Long-scoped EntityManager test passed!");
}

}
