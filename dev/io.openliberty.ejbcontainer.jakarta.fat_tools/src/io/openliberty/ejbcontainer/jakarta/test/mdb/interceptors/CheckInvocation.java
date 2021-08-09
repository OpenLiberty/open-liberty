/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.mdb.interceptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CheckInvocation {
    private static final Map<String, CallStackSnapshot> stackTrElemsMap = new HashMap<String, CallStackSnapshot>();
    private static final Map<String, List<String>> invocationOrderMap = new HashMap<String, List<String>>();
    private static LinkedBlockingQueue<String> preDestroyList;

    private CheckInvocation() {
    }

    private static class SingletonInst {
        private static CheckInvocation instance = new CheckInvocation();
    }

    public static CheckInvocation getInstance() {
        return SingletonInst.instance;
    }

    public void recordStackTrace(String idKey, String instStr) {
        Thread currThread = Thread.currentThread();
        stackTrElemsMap.put(idKey, new CallStackSnapshot(instStr, currThread.toString(), currThread.getContextClassLoader().toString(), currThread.getStackTrace()));
    }

    public boolean verifyInvocationOrder(String idKey, List<String> calls) {
        boolean isInvOrderVerfied = false;

        int i = calls.size() - 1;
        for (StackTraceElement elem : stackTrElemsMap.get(idKey).stackTrElems) {
            if (i < 0) {
                isInvOrderVerfied = true;
                break;
            }
            if (elem.toString().lastIndexOf(calls.get(i)) >= 0) {
                i--;
            }
        }

        return isInvOrderVerfied;
    }

    /*
     * public void dumpClassStackSnapshot(String idKey) { CallStackSnapshot callsnap = stackTrElemsMap.get(idKey); if (callsnap != null) { svLogger.info(callsnap.toString()); } }
     */

    public String getCurrClassStackSnapshot(String instStr) {
        Thread currThread = Thread.currentThread();
        CallStackSnapshot snapshot = new CallStackSnapshot(instStr, currThread.toString(), currThread.getContextClassLoader().toString(), currThread.getStackTrace());
        return snapshot.toString();
    }

    public void recordCallInfo(String idKey, String value, Object obj) {
        if (idKey.equals("PreDestroy")) {
            LinkedBlockingQueue<String> list = preDestroyList;
            if (list != null) {
                preDestroyList.add(value);
            }
        } else {
            List<String> callList = invocationOrderMap.get(idKey);
            if (callList == null) {
                callList = new LinkedList<String>();
            }
            callList.add(value);
            invocationOrderMap.put(idKey, callList);
        }
    }

    public List<String> getCallInfoList(String idKey) {
        return invocationOrderMap.get(idKey);
    }

    public List<String> clearCallInfoList(String idKey) {
        if (idKey.startsWith("PreDestroy:")) {
            int size = Integer.parseInt(idKey.substring(idKey.indexOf(':') + 1));
            List<String> result = new ArrayList<String>();
            while (result.size() < size) {
                try {
                    result.add(preDestroyList.poll(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return result;
                }
            }
            // Drain any remaining items.
            for (String s; (s = preDestroyList.poll()) != null;) {
                result.add(s);
            }
            preDestroyList = null;
            return result;
        }

        return invocationOrderMap.remove(idKey);
    }

    public void setupPreDestroy() {
        preDestroyList = new LinkedBlockingQueue<String>();
    }

    public void clearAllCallInfoList() {
        invocationOrderMap.clear();
        preDestroyList = null;
    }

    private class CallStackSnapshot {
        String instanceStr;
        String threadStr;
        String threadCtxCLStr;
        StackTraceElement[] stackTrElems;

        public CallStackSnapshot(String instanceStr, String threadStr, String threadCtxCLStr, StackTraceElement[] stackTrElems) {
            this.instanceStr = instanceStr;
            this.threadStr = threadStr;
            this.threadCtxCLStr = threadCtxCLStr;
            this.stackTrElems = stackTrElems;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("instance str=").append(instanceStr).append('\n');
            sb.append("thread str=").append(threadStr).append('\n');
            sb.append("thread ctx cl=").append(threadCtxCLStr).append('\n');
            for (StackTraceElement elem : stackTrElems) {
                sb.append("thread stacktrace=").append(elem.toString()).append('\n');
            }
            return sb.toString();
        }
    }
}
