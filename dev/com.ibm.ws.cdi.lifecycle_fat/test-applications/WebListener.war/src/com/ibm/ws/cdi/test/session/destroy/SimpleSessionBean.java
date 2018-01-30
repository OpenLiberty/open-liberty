/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.cdi.test.session.destroy;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;

@SessionScoped
public class SimpleSessionBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private static int id = 0;

    private final int localID;

    public SimpleSessionBean() {
        localID = id;
        id++;
    }

    public int getID() {
        return localID;
    }

    @PreDestroy
    public void pD() {
        System.out.println("pre destroy");
    }

    @PostConstruct
    public void pC() {
        System.out.println("post construct");
    }

}
