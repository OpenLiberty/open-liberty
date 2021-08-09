/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.ejb.EJBException;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;

/**
 * This class implements a very simple stateful session bean for use
 * in testing support for bean managed transactions in the EJS runtime. <p>
 *
 * It expects to be deployed as a stateful session bean using bean
 * managed transactions. <p>
 */
@SuppressWarnings("unused")
public class BMTXSessionBean implements SessionBean {
    private static final long serialVersionUID = 3476903885582393808L;

    // The following fields is to test the passivation of stateful session bean.
    private boolean passivateFlag = false;
    private static final String PASSIVATION_STRING = "String for passivation test";
    private static final int PASSIVATION_INTEGER = 12345;
    private static final long PASSIVATION_LONG = 1234567890;
    private static final double PASSIVATION_DOUBLE = 3.1415926123456789;
    private static final float PASSIVATION_FLOAT = 3.14f;
    private static final byte PASSIVATION_BYTE = 3;
    private static final boolean PASSIVATION_BOOLEAN = true;
    private static final short PASSIVATION_SHORT = 1234;

    private static byte[] svHandleBytes = null;

    private final String passString = PASSIVATION_STRING;
    private final int passInt = PASSIVATION_INTEGER;
    private final long passLong = PASSIVATION_LONG;
    private final double passDouble = PASSIVATION_DOUBLE;
    private final float passFloat = PASSIVATION_FLOAT;
    private final byte passByte = PASSIVATION_BYTE;
    private final boolean passBoolean = PASSIVATION_BOOLEAN;
    private final short passShort = PASSIVATION_SHORT;
    private final Integer passIntObj = new Integer(PASSIVATION_INTEGER);
    private final Long passLongObj = new Long(PASSIVATION_LONG);
    private final Double passDoubleObj = new Double(PASSIVATION_DOUBLE);
    private final Float passFloatObj = new Float(PASSIVATION_FLOAT);
    private final Byte passByteObj = new Byte(String.valueOf(PASSIVATION_BYTE));
    private final Boolean passBooleanObj = new Boolean(PASSIVATION_BOOLEAN);
    private final Short passShortObj = new Short(String.valueOf(PASSIVATION_SHORT));
    private CMEntityHome cmHome; // test EJBHome
    private CMEntity cmEntity; // test EJBObject
    private Context jndiEnc; // test naming context reference
    private CMKey cmkey; // test serializable type
    private String cmkeyString1, cmkeyString2;
    private UserTransaction utx;

    private SessionContext sessionContext;

    /**
     * Create a new instance of this session bean.
     */
    public BMTXSessionBean() {
        sessionContext = null;
    }

    // -------------------------------------------------
    //
    // Methods defined by the BMTXSession interface.
    //
    // -------------------------------------------------

    /**
     * Initialize values for the passivation test.
     */
    public void beforePassivation(int i) {
        cmkey = new CMKey("Test bean for passivation " + i);
        cmkeyString1 = cmkey.toString();
        try {
            createInitialContext();
            //cmHome = (CMEntityHome) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/StatefulPassivationEJB/CMEntity"), CMEntityHome.class);
            Object tmpObj = ic.lookup("CMEntity");
            cmHome = (CMEntityHome) PortableRemoteObject.narrow(tmpObj, CMEntityHome.class); // d163474
        } catch (NamingException ex) {
            throw new EJBException("lookup of CMEntity home failed.", ex);
        }

        try {
            cmEntity = cmHome.create(cmkey);
        } catch (Exception ex) {
            throw new EJBException("create cmEntity failed.  primaryKey = " + cmkeyString1, ex);
        }

        cmkey = new CMKey("Test key for passivation" + this);
        cmkeyString2 = cmkey.toString();

        try {
            jndiEnc = (Context) ic.lookup("java:comp/env");
        } catch (NamingException ex) {
            throw new EJBException("Failed to look up java:comp/env.", ex);
        }

        utx = sessionContext.getUserTransaction();
        try {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bStream);
            ostream.writeObject(sessionContext.getEJBObject().getHandle());
            bStream.flush();
            bStream.close();
            svHandleBytes = bStream.toByteArray();
        } catch (Exception ex) {
            throw new EJBException("failed to write EJBObject to file");
        }
    }

    /**
     * Clean up all the entity beans created during this test.
     */
    public void afterPassivation(int num) {
        try {
            cmEntity.remove();
        } catch (Exception ex) {
            // nothing to remove.
        }
    }

    /**
     * Test if passivation and activation correctly save and restore conversation
     * state.
     */
    public void testPassivationActivation() {
        try {
            if (passInt != PASSIVATION_INTEGER
                || passLong != PASSIVATION_LONG
                || passDouble != PASSIVATION_DOUBLE
                || passFloat != PASSIVATION_FLOAT
                || passByte != PASSIVATION_BYTE
                || passBoolean != PASSIVATION_BOOLEAN
                || passShort != PASSIVATION_SHORT)
                throw new EJBException("Test of passivation and activation failed. Primary types are not correctly passivated.");

            if (!passString.equals(PASSIVATION_STRING)
                || !passIntObj.equals(new Integer(PASSIVATION_INTEGER))
                || !passLongObj.equals(new Long(PASSIVATION_LONG))
                || !passDoubleObj.equals(new Double(PASSIVATION_DOUBLE))
                || !passFloatObj.equals(new Float(PASSIVATION_FLOAT))
                || !passByteObj.equals(new Byte(String.valueOf(PASSIVATION_BYTE)))
                || !passBooleanObj.equals(new Boolean(PASSIVATION_BOOLEAN))
                || !passShortObj.equals(new Short(String.valueOf(PASSIVATION_SHORT))))
                throw new EJBException("Test of passivation and activation failed. Build-in object types are not correctly passivated.");

            if (!cmkey.toString().equals(cmkeyString2))
                throw new EJBException("Test of passivation and activation failed. Serializable Object is not correctly passivated.");
            if (!cmEntity.getKey().toString().equals(cmkeyString1))
                throw new EJBException("Test of passivation and activation failed. EJBObject is not correctly passivated.");

            Handle savedHandle = null;
            try {
                ByteArrayInputStream bStream = new ByteArrayInputStream(svHandleBytes);
                ObjectInputStream istream = new ObjectInputStream(bStream);
                savedHandle = (Handle) istream.readObject();
                bStream.close();
            } catch (Exception ex) {
                throw new EJBException("failed to read handles from file");
            }

            EJBObject ejbObject = sessionContext.getEJBObject();

            /*
             * EJSWrapper uses stub.equals() to test if two EJSWrappers are
             * identical. That is not good enough. We should uncomment this test
             * segment after the isIdentical method is improved.
             */
            // if (!sessionContext.getEJBObject().isIdentical(savedHandle.getEJBObject()))
            //     throw new RemoteException("Test of passivation and activation failed. Handle or session context is not correctly passivated.");

            if (!cmHome.equals(cmEntity.getEJBHome()))
                throw new EJBException("Test of passivation and activation failed. EJBHome or EJBObject is not correctly passivated.");

            if (!jndiEnc.getNameInNamespace().equals("java:comp/env")) {
                throw new EJBException("Test of passivation and activation failed. Naming context is not correctly passivated.");
            }

            utx.begin();
            cmEntity.setValue(9);
            utx.commit();
        } catch (Exception ex) {
            throw new EJBException("Test of passivation and activation failed. ", ex);
        }
    }

    /**
     * Return if this instance has been passivated or not.
     */
    public boolean isPassivated() {
        try {
            if (!passivateFlag)
                cmEntity.getKey();
        } catch (Exception ex) {
            throw new EJBException("Exception keeping CMEntity alive. ", ex);
        }

        return passivateFlag;
    }

    public void runNotSupportedTest() {
        String key = "not supported";

        CMEntity bean = null;

        try {
            InitialContext initCtx = new InitialContext();
            Object o = initCtx.lookup("CMEntity");

            CMEntityHome home = (CMEntityHome) PortableRemoteObject.narrow(o, CMEntityHome.class); // d163474
            //CMEntityHome home = (CMEntityHome) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/StatefulPassivationEJB/CMEntity"), CMEntityHome.class);
            bean = home.create(new CMKey(key));

            UserTransaction ut = null;
            ut = sessionContext.getUserTransaction();

            try {
                ut.begin();
                bean.setValue_NotSupported("NotSupported");
                String getStr = bean.getValue_NotSupported();
                ut.commit();
            } catch (Exception e) {
                ut.rollback();
                throw new EJBException("Exception ", e);
            }

            bean.remove();
        } catch (Exception ex) {
            throw new EJBException("Exception ", ex);
        }
    }

    private transient InitialContext ic = null;

    private synchronized void createInitialContext() {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (Exception ex) {
                throw new EJBException("Failure creating initial context", ex);
            }
        }
    }

    // ----------------------------------------------
    //
    // Methods defined by the SessionBean inteface.
    //
    // ----------------------------------------------

    public void ejbCreate() {}

    @Override
    public void ejbActivate() {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (Exception ex) {
                throw new EJBException("Failure creating initial context", ex);
            }
        }
    }

    @Override
    public void ejbPassivate() {
        passivateFlag = true;
    }

    @Override
    public void ejbRemove() {}

    /**
     * Set the session context for use by this bean.
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        sessionContext = ctx;
    }
}