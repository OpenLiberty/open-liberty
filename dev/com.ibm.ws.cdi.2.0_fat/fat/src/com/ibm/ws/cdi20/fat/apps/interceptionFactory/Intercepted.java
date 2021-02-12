/**
 *
 */
package com.ibm.ws.cdi20.fat.apps.interceptionFactory;

/**
 *
 */
public class Intercepted {

    static boolean intercepted = false;

    static void set() {
        intercepted = true;
    }

    static boolean get() {
        return intercepted;
    }
}
