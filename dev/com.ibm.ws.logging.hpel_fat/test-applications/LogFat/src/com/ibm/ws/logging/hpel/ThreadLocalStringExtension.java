/*
 * This program may be used, executed, copied, modified and distributed
 * without royalty for the purpose of developing, using, marketing, or distributing.
 */

package com.ibm.ws.logging.hpel;

import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.LogRecordContext;

public class ThreadLocalStringExtension implements LogRecordContext.Extension {
    private static Logger logger = Logger.getLogger("com.ibm.ws.test.ThreadLocalStringExtension");

    public ThreadLocalStringExtension() {
    }

    private final ThreadLocal<String> threadLocalString = new ThreadLocal<String>();

    public void setValue(String string) {
        threadLocalString.set(string);
        logger.info("setValue - new value: [" + string + "] for thread " + Thread.currentThread().getName());
//        System.out.println("setValue - new value: [" + string + "] for thread " + Thread.currentThread().getName());
    }

    @Override
    public String getValue() {
        return threadLocalString.get();
        // don't log here or you will get a recursion
    }
}
