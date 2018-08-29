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
package com.ibm.ws.anno.targets.cache;

/**
 * Wrapper for a boolean value.
 *
 * Not thread safe.  For a thread safe boolean value wrapper,
 * use instead {@link java.util.concurrent.AtomicBoolean}.
 */
public class TargetCache_Boolean {
    public static final boolean DEFAULT_VALUE = false;

    public TargetCache_Boolean() {
        this(DEFAULT_VALUE);
    }

    public TargetCache_Boolean(boolean value) {
        this.value = value;
    }

    //

    private boolean value;

    public boolean getValue() {
        return value;
    }

    public boolean consumeValue() {
        boolean oldValue = value;
        value = DEFAULT_VALUE;
        return oldValue;
    }

    public boolean setValue(boolean newValue) {
        boolean oldValue = value;
        value = newValue;
        return oldValue;
    }
}
