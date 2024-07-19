/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.web;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaDataRecreate")
public class JakartaDataRecreateServlet extends FATServlet {

    @PersistenceContext(unitName = "RecreatePersistenceUnit")
    private EntityManager em;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

}
