/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckInvocation {
    private static final Map<String, CallStackSnapshot> stackTrElemsMap = new HashMap<String, CallStackSnapshot>();
    private static final Map<String, List<String>> invocationOrderMap = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> injectionMap = new HashMap<String, List<String>>();

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
        List<String> callList = invocationOrderMap.get(idKey);
        if (callList == null) {
            callList = new LinkedList<String>();
        }
        callList.add(value);
        invocationOrderMap.put(idKey, callList);

        List<String> instanceList = invocationOrderMap.get(idKey + "Instance");
        if (instanceList == null) {
            instanceList = new LinkedList<String>();
        }
        instanceList.add(obj.toString());
        invocationOrderMap.put(idKey + "Instance", instanceList);
    }

    public List<String> getCallInfoList(String idKey) {
        return invocationOrderMap.get(idKey);
    }

    public List<String> clearCallInfoList(String idKey) {
        return invocationOrderMap.remove(idKey);
    }

    public void clearAllCallInfoList() {
        invocationOrderMap.clear();
    }

    public void recordInjectionInfo(String idKey, String value, Object obj) {
        List<String> injList = injectionMap.get(idKey);
        if (injList == null) {
            injList = new LinkedList<String>();
        }
        injList.add(value);
        injectionMap.put(idKey, injList);

        List<String> instanceList = injectionMap.get(idKey + "Instance");
        if (instanceList == null) {
            instanceList = new LinkedList<String>();
        }
        instanceList.add(obj.toString());
        injectionMap.put(idKey + "Instance", instanceList);
    }

    public List<String> getInjectionList(String idKey) {
        return injectionMap.get(idKey);
    }

    public List<String> clearInjectionList(String idKey) {
        return injectionMap.remove(idKey);
    }

    public void clearAllInjectionList() {
        injectionMap.clear();
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