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

import java.io.Serializable;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;

import cdi12.resources.GlobalState;

/**
 *
 */
@SessionScoped
public class SessionScopedBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

    public static void onStart(@Observes @Initialized(SessionScoped.class) Object e) {

        GlobalState.recordSessionStart();
    }

    public static void onStop(@Observes @Destroyed(SessionScoped.class) Object e) {
        GlobalState.recordSessionStop();

    }

}
