/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.ejbcontainer.session.async.warn.ejb;

import javax.ejb.Stateless;

@Stateless
public class InitRecoveryLogBean {
    public long getInvocationTime() {
        return System.currentTimeMillis();
    }
}
