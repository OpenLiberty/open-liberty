/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

package com.ibm.ws.cdi12test.remoteEjb.ejb;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.Stateful;
import javax.enterprise.event.Observes;

import com.ibm.ws.cdi12test.remoteEjb.api.EJBEvent;
import com.ibm.ws.cdi12test.remoteEjb.api.RemoteInterface;

@Stateful
public class TestObserver implements RemoteInterface {

    static AtomicBoolean observed = new AtomicBoolean(false);

    @Override
    public void observeRemote(@Observes EJBEvent e) {
        observed.set(true);
    }

    @Override
    public boolean observed() {
        return observed.get();
    }

}
