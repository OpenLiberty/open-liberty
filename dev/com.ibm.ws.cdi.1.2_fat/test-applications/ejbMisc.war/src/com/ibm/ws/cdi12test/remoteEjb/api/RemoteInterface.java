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

package com.ibm.ws.cdi12test.remoteEjb.api;

import javax.ejb.Remote;

@Remote
public interface RemoteInterface {

    public void observeRemote(EJBEvent e);

    public boolean observed();

}
