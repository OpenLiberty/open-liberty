/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Bean implementation class for Enterprise Bean: SFRa
 */
public class SFRaBean implements SessionBean {
    private static final long serialVersionUID = -686496765499777826L;

    private static final String CLASS_NAME = SFRaBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String BeanName = "SFRa";

    private SessionContext mySessionCtx;

    public String pKey;
    public int intValue;
    public String stringValue;

    /**
     * getSessionContext
     */
    public SessionContext getSessionContext() {
        printMsg("(getSessionContext)");
        return mySessionCtx;
    }

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        printMsg("(setSessionContext)");
        mySessionCtx = ctx;

        // PK17144.1: EJB 2.1 Spec, section 7.6.1, table 2 states that this
        // life cycle method should have access to java:comp/env....so lets try.
        this.getJavaEnvValue("setSessionContext");
    }

    /**
     * unsetSessionContext
     */
    public void unsetSessionContext() {
        printMsg("(unsetSessionContext)");
        mySessionCtx = null;
    }

    /**
     * Get accessor for persistent attribute: pKey
     */
    public String getPKey() {
        return pKey;
    }

    /**
     * Set accessor for persistent attribute: pKey
     */
    public void setPKey(String pKey) {
        this.pKey = pKey;
    }

    /**
     * Get accessor for persistent attribute: intValue
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * Set accessor for persistent attribute: intValue
     */
    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Get accessor for persistent attribute: stringValue
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Set accessor for persistent attribute: stringValue
     */
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * PK17144.1: This APAR was opened because a user tried to get some context out
     * of java:comp/env during ejbPassivate in a Stateful Session Bean. This lookup failed.
     * Therefore, I'm adding a test here to verify that with the fix to this APAR
     * we can get at the java:comp/env context. This code is tested indirectly through
     * the tests in this suite. In other words, the tests in this suite perform certain
     * operations which, along with the fact that this bean has its "atActivate" field set
     * to "TRANSACTION", will cause this bean to be passivated. If the following look up
     * fails, an exception will be thrown. This exception may not be apparently obvious (i.e.
     * it may not cause the test to fail immediately), but after this exception is thrown the
     * bean should be destroyed and as such on subsequent accesses a "NoSuchObjectException"
     * should be thrown which will cause the fvts to fail horribly.
     * This method will be called by a number of the life cycle methods in this bean. Even
     * though this APAR was specific to ejbPassivate, we might as well go the extra inch and
     * verify that other life cycle methods can access java:comp/en as outlines in the
     * EJB 2.1 Spec, section 7.6.1, table 2.
     *
     * @param method A string containing the life cycle method that calls this method.
     */
    public void getJavaEnvValue(String method) {
        String str = null;
        this.printMsg("getJavaEnvValue called by: " + method);
        try {
            InitialContext ic = new InitialContext();

            // First, lets verify that we can get the default/root java:comp/env context (i.e.
            // the bean's environment naming context).
            try {
                Context defaultCxt = (Context) ic.lookup("java:comp/env");
                if (defaultCxt == null) {
                    throw new EJBException("Test of java:comp/env failed when trying to get the default context, called by life cycle method: " + method);
                }

                str = (String) defaultCxt.lookup("PK17144");
                if (str == null || !str.equals("PK17144_string")) {
                    throw new EJBException("Test of java:comp/env failed using the default context." +
                                           " The retrieved String value, \"" + str + "\", is incorrect.  Called by life cycle method: " + method);
                }

            } catch (Throwable t) {
                printMsg("Got an exception when doing a java:comp/env lookup!  We should be able to"
                         + " do a java:comp/env lookup in " + method + ".  See APAR PK17144.  Exception is: " + t);
                throw new RuntimeException("Got an excpetion when doing a java:comp/env lookup!  We should be able to"
                                           + " do a java:comp/env lookup in " + method + ".  See APAR PK17144.  Exception is: " + t);
            }

            // Second, verify that we can get an env variable by using fully qualified
            // path names.
            str = (String) ic.lookup("java:comp/env/PK17144");
            if (str == null || !str.equals("PK17144_string")) {
                throw new EJBException("Test of java:comp/env/PK17144 failed." +
                                       " The retrieved String value, \"" + str + "\", is incorrect.  Called by life cycle method: " + method);
            }
            // OK, if here we should be satisfy that we can access java:comp/env from a
            // life cycle method.
        } catch (Throwable t) {
            printMsg("Got an unexpected excpetion when doing the java:comp/env lookup test!  We should be able to"
                     + " do a java:comp/env lookup from life cycle method: " + method + ".  See APAR PK17144.  Exception is: " + t);
            t.printStackTrace();
            throw new RuntimeException("Got an excpetion when doing the java:comp/env lookup test!  We should be able to"
                                       + " do a java:comp/env lookup from life cycle method: " + method + ".  See APAR PK17144.", t);
        }
    }

    /**
    *
    */
    public void ejbCreate(String pKey, int intValue, String stringValue) throws CreateException {
        printMsg("(ejbCreate) pKey = " + pKey);
        printMsg("    intValue     = " + intValue);
        printMsg("    stringValue  = " + stringValue);

        setPKey(pKey);
        setIntValue(intValue);
        setStringValue(stringValue);

        if (getIntValue() == SFRa.Create) {
            printMsg("throw RuntimeException");
            //            int a =1;
            //            int b = 0;
            //            int c = a/b;
            throw new RuntimeException("SFRa.ejbCreate()");
        }

        // PK17144.1: EJB 2.1 Spec, section 7.6.1, table 2 states that this
        // life cycle method should have access to java:comp/env....so lets try.
        this.getJavaEnvValue("ejbCreate");
    }

    /**
     * Insert the method's description here.
     */
    public void ejbPostCreate(String pKey, int intValue, String stringValue) throws CreateException {
        printMsg("(ejbPostCreate):" + getIntValue());
        if (getIntValue() == SFRa.PostCreate) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbPostCreate()");
        }
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg("(ejbActivate):" + getIntValue());
        if (getIntValue() == SFRa.Activate) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbActivate()");
        }

        // PK17144.1: EJB 2.1 Spec, section 7.6.1, table 2 states that this
        // life cycle method should have access to java:comp/env....so lets try.
        this.getJavaEnvValue("ejbActivate");
    }

    /**
     * ejbPassivate
     */
    @Override
    public void ejbPassivate() {
        printMsg("(ejbPassivate):" + getIntValue());
        if (getIntValue() == SFRa.Passivate) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbPassivate()");
        }

        // PK17144.1: EJB 2.1 Spec, section 7.6.1, table 2 states that this
        // life cycle method should have access to java:comp/env....so lets try.
        this.getJavaEnvValue("ejbPassivate");
    }

    /**
     * ejbLoad
     */
    public void ejbLoad() {
        printMsg("(ejbLoad):" + getIntValue());
        if (getIntValue() == SFRa.Load) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbLoad()");
        }
    }

    /**
     * ejbStore
     */
    public void ejbStore() {
        printMsg("(ejbStore):" + getIntValue());
        if (getIntValue() == SFRa.Store) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbStore()");
        }
    }

    /**
     * ejbRemove
     */
    @Override
    public void ejbRemove() {
        printMsg("(ejbRemove):" + getIntValue());
        if (getIntValue() == SFRa.Remove) {
            printMsg("throw RuntimeException");
            throw new RuntimeException("SFRa.ejbRemove()");
        }

        // PK17144.1: EJB 2.1 Spec, section 7.6.1, table 2 states that this
        // life cycle method should have access to java:comp/env....so lets try.
        this.getJavaEnvValue("ejbRemove");
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:04:36 PM)
     */
    public void printMsg(String msg) {
        svLogger.info("  " + BeanName + " : " + msg);
    }

    /**
     * Echo the input string plus the key string
     */
    public String echoRequired(String str) {
        return str + ":" + getPKey();
    }
}