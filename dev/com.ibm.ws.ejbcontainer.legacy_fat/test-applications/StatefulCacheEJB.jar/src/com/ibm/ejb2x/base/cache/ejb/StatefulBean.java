/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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
package com.ibm.ejb2x.base.cache.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Bean implementation for a basic Stateful Session bean that may be
 * configured with different Activation policies (ONCE, TRANSACTION).
 **/
public class StatefulBean implements SessionBean {
    private static final long serialVersionUID = -6265505590411880402L;

    private static boolean svRefPassivateFlag = false; // PK04804

    public SessionContext ivSC = null; // PK04804
    protected String ivMessage;
    protected StatefulLocalObject ivRefObj; // PK04804

    public void ejbCreate() throws CreateException {
        ivMessage = "StatefulBean - default message";
    }

    public void ejbCreate(String message) throws CreateException {
        ivMessage = message;
    }

    public void setMessage(String message) {
        ivMessage = message;
    }

    public String getMessage() {
        return ivMessage;
    }

    public StatefulBean() {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {
        // If this object references another, then set a flag so the client
        // knows this bean is passivating and will attempt to access the
        // referenced bean... and sleep to give the client some time.      PK04804
        if (ivRefObj != null) {
            svRefPassivateFlag = true;

            try {
                System.out.println("StatefulBean.ejbPassivate: Sleeping for 10000...");
                Thread.sleep(10000);
                System.out.println("StatefulBean.ejbPassivate: Removing Reference");
                ivRefObj.remove();
            } catch (Throwable ex) {
                System.out.println("Unexpected exception in ejbPassivate: " + ex);
                ex.printStackTrace(System.out);
                throw new EJBException("Unexpected exception in ejbPassivate " + ex);
            } finally {
                svRefPassivateFlag = false;
                ivRefObj = null;
            }
        }
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivSC = sc;
    }

    /**
     * Returns the hashcode of the 'primary key' for this Stateful
     * Session bean. <p>
     *
     * In order to force EJB Cache bucket contention, this hashCode
     * method returns a hashcode that is the 'significant' part of
     * the hashcode used by the EJB Cache.
     **/
    // PK04804
    public int getHashCode() {
        // The 'primary key' for a Stateful Session bean cannot be obtained
        // directly, so a sneaky way is done here... and relies on the
        // fact that the toString() method of a Local Wrapper will include
        // the BeanId, which includes the primary key.
        EJBLocalObject local = ivSC.getEJBLocalObject();
        String beanIdString = local.toString();

        // Strip out the upper long of the primary key from the
        // BeanId string.
        int comma = beanIdString.lastIndexOf(',');
        String idStr = beanIdString.substring(comma + 2, comma + 20);

        // Convert the hex bytes of the upper long back to a long
        // and cast to an int, just like is done in the UUID class
        // that serves as the primary key.
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt(idStr.substring(14, 16), 16);
        bytes[1] = (byte) Integer.parseInt(idStr.substring(16, 18), 16);
        bytes[2] = (byte) Integer.parseInt(idStr.substring(9, 11), 16);
        bytes[3] = (byte) Integer.parseInt(idStr.substring(11, 13), 16);
        bytes[4] = (byte) Integer.parseInt(idStr.substring(0, 2), 16);
        bytes[5] = (byte) Integer.parseInt(idStr.substring(2, 4), 16);
        bytes[6] = (byte) Integer.parseInt(idStr.substring(4, 6), 16);
        bytes[7] = (byte) Integer.parseInt(idStr.substring(6, 8), 16);
        long id = (((long) bytes[0]) & 0xFF) << 56
                  | (((long) bytes[1]) & 0xFF) << 48
                  | (((long) bytes[2]) & 0xFF) << 40
                  | (((long) bytes[3]) & 0xFF) << 32
                  | (((long) bytes[4]) & 0xFF) << 24
                  | (((long) bytes[5]) & 0xFF) << 16
                  | (((long) bytes[6]) & 0xFF) << 8
                  | (((long) bytes[7]) & 0xFF) << 0;
        return (int) id;
    }

    // PK04804
    public void setReference(StatefulLocalObject refObj) {
        ivRefObj = refObj;
    }

    // PK04804
    public boolean getReferencePassivateFlag() {
        return svRefPassivateFlag;
    }
}