/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fat.dserror.web;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/*")
@PersistenceUnit(name = "jpa/pu", unitName = "DSErrorPU")
@SuppressWarnings("serial")
public class DSErrorServlet extends FATServlet {
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void testLookupEMF() throws Exception {
        try {
            EntityManagerFactory emf = (EntityManagerFactory) new InitialContext().lookup("java:comp/env/jpa/pu");
            System.out.println("java:comp/env/jpa/pu = " + emf);
            EntityManager em = emf.createEntityManager();
            System.out.println("em = " + em);
            Assert.fail();
        } catch (NamingException e) {
            String s = e.toString();
            // CWWJP0015E: An error occurred in the
            // com.ibm.websphere.persistence.PersistenceProviderImpl persistence
            // provider when it attempted to create the container entity manager
            // factory for the DSErrorPU persistence unit. The following error
            // occurred:
            //
            // CWWJP0013E: The server cannot locate the jdbc/doesnotexist data
            // source for the DSErrorPU persistence unit because it has
            // encountered the following exception:
            //
            // javax.naming.NameNotFoundException: Intermediate context does not
            // exist: jdbc/doesnotexist.
            if (s.contains("CWNEN1001E:") &&
                ((s.contains("CWWJP0013E:") && s.contains("NameNotFoundException:")) ||
                 (s.contains("The object referenced by the java:comp/env/jpa/pu JNDI name could not be instantiated.")))) {
                System.out.println("Caught expected error: " + e);
            } else {
                throw e;
            }
        }
    }
}
