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

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;

@Local(SuperEnvInjectionLocal.class)
@Remote(SuperEnvInjectionRemote.class)
public class SuperSFEnvInjectBean extends SuperSuperEnvInject implements java.io.Serializable {
    private static final long serialVersionUID = 8179031532447319309L;
    private static final String PASSED = "Passed";
    private static final float FDELTA = 0.0F;

    private static final String A_STRING = new String("Yes I will!");
    private static final Boolean A_BOOLEAN = new Boolean(true);
    private static final Character B_CHARACTER = new Character('Y');
    private static final Integer C_INTEGER = new Integer(8675309);
    private static final Integer D_INTEGER = new Integer(90210);
    private static final String F_STRING = new String("Yes");
    private static final Integer F_INTEGER = new Integer(1);
    private static final Float F_FLOAT = new Float(55.55F);
    private static final Short F_SHORT = new Short((short) 5);

    private SessionContext ivContext;

    @SuppressWarnings("hiding")
    private int myNumber = 0;

    @Override
    protected void setMyNumber(int myNumber) {
        this.myNumber = myNumber;
    }

    @Override
    public int getMyNumber() {
        return myNumber;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that myNumber within this class, not the superclass myNumber is injected correctly
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "myNumber is 8675309 : " + getMyNumber(),
                     C_INTEGER.intValue(), getMyNumber());
        ++testpoint;

        // Assert that the superclass's myNumber was not injected
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "my supers myNumber is 90210 : " + super.getMyNumber(),
                     D_INTEGER.intValue(), super.getMyNumber());
        ++testpoint;

        // Assert that the superclass was injected into a PRIVATE FIELD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field is true : " + getField(),
                     A_BOOLEAN, getField());
        ++testpoint;

        // Assert that the superclass was injected into a PROTECTED FIELD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field2 is yes : " + field2,
                     F_STRING, field2);
        ++testpoint;

        // Assert that the superclass was injected into a PUBLIC FIELD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field3 is Y : " + field3,
                     B_CHARACTER, field3);
        ++testpoint;

        // Assert that the superclass was injected into a PROTECTED METHOD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field4 is 1 : " + field4,
                     F_INTEGER.intValue(), field4);
        ++testpoint;

        // Assert that the superclass was injected into a PRIVATE METHOD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field5 is 55.55 : " + getField5(),
                     F_FLOAT.floatValue(), getField5(), FDELTA);
        ++testpoint;

        // Assert that the superclass was injected into a PUBLIC METHOD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My supers field6 is 5 : " + field6,
                     F_SHORT.shortValue(), field6);
        ++testpoint;

        // Assert that the superclass of a superclass was properly injected with a String
        // into a PRIVATE METHOD using the default name
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "My superSuperPrivateString is Yes I will! : " + getSuperSuperPrivateString(),
                     A_STRING, getSuperSuperPrivateString());
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejb.SuperSuperEnvInject/field2";
            Object aString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + aString,
                         F_STRING, aString);
            ++testpoint;

        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejb.SuperSuperEnvInject/field2";
        Object aString = ivContext.lookup(envName);
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "lookup:" + envName + ":" + aString,
                     F_STRING, aString);
        ++testpoint;

        return PASSED;
    }

    /* Provided for interface completeness, used by SLSB tests */
    public void discardInstance() {
        return;
    }

    public SuperSFEnvInjectBean() {
        // intentionally blank
    }
}
