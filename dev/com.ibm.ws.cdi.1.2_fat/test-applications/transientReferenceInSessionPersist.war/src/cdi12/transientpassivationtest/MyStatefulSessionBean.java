/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package cdi12.transientpassivationtest;

import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

@LocalBean
@Stateful(passivationCapable = false)
public class MyStatefulSessionBean {

    String msg = "MyStatefulSessionBean was destroyed";

    public void doNothing() {
        int i = 1;
        i++;
    }

    public void setMessage(String s) {
        msg = s;
    }

    @PreDestroy
    public void preD() {
        GlobalState.addString(msg);
    }

    public MyStatefulSessionBean() {}
}
