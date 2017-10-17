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
package cdi12.scopedclasses;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;

import cdi12.resources.GlobalState;

/**
 *
 */
@RequestScoped
public class RequestScopedBean {

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {
        GlobalState.recordRequestStart();

    }

    public static void onStop(@Observes @Destroyed(RequestScoped.class) Object e) {

        GlobalState.recordRequestStop();

    }

}
