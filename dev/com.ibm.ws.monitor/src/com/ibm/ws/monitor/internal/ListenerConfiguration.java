/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAfterCall;
import com.ibm.websphere.monitor.annotation.ProbeAtCallError;
import com.ibm.websphere.monitor.annotation.ProbeAtCatch;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtExceptionExit;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeAtThrow;
import com.ibm.websphere.monitor.annotation.ProbeBeforeCall;
import com.ibm.websphere.monitor.annotation.ProbeFieldGet;
import com.ibm.websphere.monitor.annotation.ProbeFieldSet;
import com.ibm.websphere.monitor.annotation.ProbeSite;

// probes=[{
//         class.filter="com.ibm.ws.threading.internal.*",
//         method.filter="*",
//         method.descriptor.filter="*",
//         bundle.filter="com.ibm.ws.threading"
//         bundle.version="0.0"
//     }, {
//         class.filter="com.ibm.websphere.threading.*",
//         method.filter="*",
//         method.descriptor.filter="*"
// }]

// A listener configuration is an aggregation:
// 
// * A ProbeFilter that can be used to filter which classes and methods
//   need to be instrumented with probes
// * A collection of probe locations that describe where probes are injected
//
// Where classes are defined, the ProbeFilter implementations are evaluated
// against the defined class.  If the class filter matches, each declared
// method of the class is evaluated.  Where a method match is found, a probe
// location specification will be created.

// ProbeLocation --> ProbeImpl --> Collection<ProbeListener>
//
// ProbeListener --> Collection<ProbeImpl>
// ProbeListener --> ListenerConfiguration --> ProbeFilter
//                                         +-> ProbeLocation --> Collection<ProbeListener>

public class ListenerConfiguration {

    private Set<String> groupNames;
    private ProbeFilter probeFilter;
    private ProbeAtEntry probeAtEntry;
    private ProbeAtReturn probeAtReturn;
    private ProbeAtExceptionExit probeAtExceptionExit;
    private ProbeAtCatch probeAtCatch;
    private ProbeAtThrow probeAtThrow;
    private ProbeBeforeCall probeBeforeCall;
    private ProbeAfterCall probeAfterCall;
    private ProbeAtCallError probeAtCallError;
    private ProbeFieldGet probeFieldGet;
    private ProbeFieldSet probeFieldSet;

    private ConcurrentHashMap<String, Object> transformerConfigData = new ConcurrentHashMap<String, Object>();

    ListenerConfiguration(Monitor groups, ProbeSite probeSite, Method method) {
        this.groupNames = new HashSet<String>();
        if (groups == null) {
            groupNames.add(method.getDeclaringClass().getName());
        } else {
            for (String group : groups.group()) {
                groupNames.add(group);
            }
        }
        probeFilter = new ProbeFilter(probeSite);
        probeAtEntry = method.getAnnotation(ProbeAtEntry.class);
        probeAtReturn = method.getAnnotation(ProbeAtReturn.class);
        probeAtExceptionExit = method.getAnnotation(ProbeAtExceptionExit.class);
        probeAtCatch = method.getAnnotation(ProbeAtCatch.class);
        probeAtThrow = method.getAnnotation(ProbeAtThrow.class);
        probeBeforeCall = method.getAnnotation(ProbeBeforeCall.class);
        probeAfterCall = method.getAnnotation(ProbeAfterCall.class);
        probeAtCallError = method.getAnnotation(ProbeAtCallError.class);
        probeFieldGet = method.getAnnotation(ProbeFieldGet.class);
        probeFieldSet = method.getAnnotation(ProbeFieldSet.class);
    }

    public Set<String> getGroupNames() {
        return groupNames;
    }

    public ProbeFilter getProbeFilter() {
        return probeFilter;
    }

    public ProbeAtEntry getProbeAtEntry() {
        return probeAtEntry;
    }

    public ProbeAtReturn getProbeAtReturn() {
        return probeAtReturn;
    }

    public ProbeAtExceptionExit getProbeAtExceptionExit() {
        return probeAtExceptionExit;
    }

    public ProbeAtCatch getProbeAtCatch() {
        return probeAtCatch;
    }

    public ProbeAtThrow getProbeAtThrow() {
        return probeAtThrow;
    }

    public ProbeBeforeCall getProbeBeforeCall() {
        return probeBeforeCall;
    }

    public ProbeAfterCall getProbeAfterCall() {
        return probeAfterCall;
    }

    public ProbeAtCallError getProbeAtCallError() {
        return probeAtCallError;
    }

    public ProbeFieldGet getProbeFieldGet() {
        return probeFieldGet;
    }

    public ProbeFieldSet getProbeFieldSet() {
        return probeFieldSet;
    }

    @SuppressWarnings("unchecked")
    public <T extends Object> T getTransformerData(String key) {
        return (T) transformerConfigData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends Object> T setTransformerData(String key, T data) {
        return (T) transformerConfigData.put(key, data);
    }
}
