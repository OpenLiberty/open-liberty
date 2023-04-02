/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.web;

import static org.junit.Assert.assertFalse;

import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * Stateless bean using jakarta package annotations, except for
 * an incorrect use of javax.annotation.PostConstruct.
 */
@Stateless
public class JakartaStatelessWarPostConstructBean {
    boolean postConstruct = false;

    @javax.annotation.PostConstruct
    private void postConstruct() {
        postConstruct = true;
        throw new EJBException("javax.annotation.PostConstruct should not be called");
    }

    public void verifyPostConstruct() {
        assertFalse("javax PostConstruct called", postConstruct);
    }
}
