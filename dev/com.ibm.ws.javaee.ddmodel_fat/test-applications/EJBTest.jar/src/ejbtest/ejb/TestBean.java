/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejbtest.ejb;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Bean implementation class for Enterprise Bean: TestBean
 */
public class TestBean implements SessionBean {

    static final long serialVersionUID = 3206093459760846163L;

    //

    private static int total = 0;
    private static int alive = 0;
    private static int active = 0;

    public int getTotal() {
        int useTotal = total;
        System.out.println("getTotal " + useTotal);
        return useTotal;
    }

    public int getAlive() {
        int useAlive = alive;
        System.out.println("getAlive " + useAlive);
        return useAlive;
    }

    public int getActive() {
        int useActive = active;
        System.out.println("getActive " + useActive);
        return useActive;
    }

    //

    private SessionContext mySessionCtx;

    public SessionContext getSessionContext() {
        return mySessionCtx;
    }

    public void setSessionContext(SessionContext ctx) {
        mySessionCtx = ctx;
    }

    //

    private String ID = null;

    public void ejbCreate(String ID) throws CreateException {
        this.ID = ID;

        int useTotal, useAlive, useActive;

        synchronized( TestBean.class ) {
            total++;
            alive++;
            active++;

            useTotal = total;
            useAlive = alive;
            useActive = active;
        }

        System.out.println("ejbCreate " + useTotal + "/" + useAlive + "/" + useActive);
    }

    public void ejbActivate() {
        int useTotal, useAlive, useActive;

        synchronized( TestBean.class ) {
            active++;

            useTotal = total;
            useAlive = alive;
            useActive = active;
        }

        System.out.println("ejbActivate " + useTotal + "/" + useAlive + "/" + useActive);
    }

    public void ejbPassivate() {
        int useTotal, useAlive, useActive;

        synchronized( TestBean.class ) {
            active--;

            useTotal = total;
            useAlive = alive;
            useActive = active;
        }

        System.out.println("ejbPassivate " + useTotal + "/" + useAlive + "/" + useActive);
    }

    public void ejbRemove() {
        int useTotal, useAlive, useActive;

        synchronized( TestBean.class ) {
            active--;
            alive--;

            useTotal = total;
            useAlive = alive;
            useActive = active;
        }

        System.out.println("ejbRemove " + useTotal + "/" + useAlive + "/" + useActive);
    }
}
