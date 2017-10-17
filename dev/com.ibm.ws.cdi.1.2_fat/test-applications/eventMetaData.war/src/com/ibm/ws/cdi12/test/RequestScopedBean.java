/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 *
 */
@RequestScoped
public class RequestScopedBean {

    @Inject
    @MetaQualifier
    Event<MyEvent> event;

    public void fireEvent() {
        event.fire(new MyEvent());
    }

}
