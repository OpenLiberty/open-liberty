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
package com.ibm.ws.cdi12.test.passivation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;

@ApplicationScoped
public class BeanHolder {

    public void doNothing() {
        int i = 1;
        i++;
    }

    @Inject
    public void transientVisit(@TransientReference TransiantDependentScopedBean bean) {
        bean.doNothing();
    }
}
