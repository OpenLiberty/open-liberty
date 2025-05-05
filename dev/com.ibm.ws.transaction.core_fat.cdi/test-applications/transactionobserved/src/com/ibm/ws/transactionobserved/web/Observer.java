/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.transactionobserved.web;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Dependent
public class Observer {
    void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) EventDto dto) {
        try {
            // Lookup should succeed (unless legacy behavior is enabled)
            InitialContext.doLookup("java:app/AppName");
            System.out.println("Context is available in AFTER_SUCCESS observer");
        } catch (NamingException e) {
            System.out.println("Context is not available in AFTER_SUCCESS observer");
        }
    }
}
