/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2015
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.logging.utils;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class that contains a thread local that any log handler can use to
 * avoid loops when any tracing or logging in its code path causes an event
 * to be written back to log or trace source.
 */
@Trivial
public class ThreadLocalHandler {

    private static ThreadLocal<Boolean> boolValue = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static Boolean get() {
        return boolValue.get();
    }

    public static void set(Boolean value) {
        boolValue.set(value);
    }

    public static void remove() {
        boolValue.remove();
    }
}
