/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.ann.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless(name = "SLEnvInjectEJBFldRmt")
@Remote(SLInjectRemote.class)
public class SLEnvInjectEJBFldRmtBean {
    private static final String PASSED = "Passed";
    @Resource
    private SessionContext ctx;
    @EJB
    private SimpleSFRemote injectedRef;

    public String callInjectedEJB(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "success";
            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedRef --> Expected: " + expected + ". Received: " + injectedRef.getString() +
                         ". If they match the bean was successfully injected.", expected, injectedRef.getString());
            ++testpoint;

            // Lookup the stateful bean using the default ENC JNDI entry that should
            // have been added by default via the field level injection
            SimpleSFRemote obj = (SimpleSFRemote) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.SLEnvInjectEJBFldRmtBean/injectedRef");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "ctx.lookup --> Lookup of the stateful bean using the default ENC JNDI entry that should " +
                          "have been added by default via the field level injection was successsful.", obj);
            ++testpoint;

            // Call a method on the obj bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean obj --> Expected: " + expected + ". Received: " + obj.getString() +
                         ". If they match the bean was successfully injected.", expected, obj.getString());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    @Remove
    public void finish() {
        // Intentionally blank
    }
}
