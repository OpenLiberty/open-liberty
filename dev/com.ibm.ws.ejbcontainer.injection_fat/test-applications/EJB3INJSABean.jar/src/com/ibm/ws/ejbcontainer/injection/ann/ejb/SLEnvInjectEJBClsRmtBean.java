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

// Inject a simple stateful bean into the ENC for this bean
@EJB(name = "ejb/SFEJBInjectedRmt_remote_biz", beanName = "SFEJBInjectedRmt", beanInterface = SimpleSFRemote.class)
@Stateless(name = "SLEnvInjectEJBClsRmt")
@Remote(SLInjectRemote.class)
public class SLEnvInjectEJBClsRmtBean {
    private static final String PASSED = "Passed";
    @Resource
    private SessionContext ctx;

    public String callInjectedEJB(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            // Lookup the stateful bean using an injected session context, using the ENC
            // JNDI entry added by class level injection
            Object obj = ctx.lookup("ejb/SFEJBInjectedRmt_remote_biz");
            SimpleSFRemote injectedRef = (SimpleSFRemote) obj;

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Lookup of the stateful bean using an injected session context using the " +
                          "ENC JNDI entry added by the class level injection was successsful.", injectedRef);
            ++testpoint;

            String expected = "success";
            // Call a method on the bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Expected: " + expected + ". Received: " + injectedRef.getString() +
                         ". If they match the bean was successfully injected.", expected, injectedRef.getString());
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
