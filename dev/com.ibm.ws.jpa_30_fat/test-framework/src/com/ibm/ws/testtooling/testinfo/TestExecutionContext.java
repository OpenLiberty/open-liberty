/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.testinfo;

import java.util.HashMap;

/**
 * Context object that directs generic test execution components.
 *
 */
public class TestExecutionContext implements java.io.Serializable {
    private static final long serialVersionUID = 9199074786089584538L;

    private String name = null;
    private String testLogicClassName = null;
    private String testLogicMethod = null;
    private TestSessionSignature testSessionSig = null;

    private HashMap<String, JPAPersistenceContext> jpaPCInfoMap = new HashMap<String, JPAPersistenceContext>();
    private HashMap<String, MessagingClientContext> msgClientMap = new HashMap<String, MessagingClientContext>();
    private HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();

    public TestExecutionContext(String name, String testLogicClassName, String testLogicMethod) {
        this.name = name;
        this.testLogicClassName = testLogicClassName;
        this.testLogicMethod = testLogicMethod;

        this.testSessionSig = new TestSessionSignature(name);
    }

    public TestExecutionContext(String name, String testLogicClassName, String testLogicMethod, TestSessionSignature tss) {
        this.name = name;
        this.testLogicClassName = testLogicClassName;
        this.testLogicMethod = testLogicMethod;

        this.testSessionSig = new TestSessionSignature(tss);
    }

    public TestExecutionContext(TestExecutionContext teCtx) {
        this.name = teCtx.getName();
        this.testLogicClassName = teCtx.getTestLogicClassName();
        this.testLogicMethod = teCtx.getTestLogicMethod();

        HashMap<String, JPAPersistenceContext> teCtxJPAPCInfoMap = teCtx.getJpaPCInfoMap();
        for (String key : teCtxJPAPCInfoMap.keySet()) {
            jpaPCInfoMap.put(key, new JPAPersistenceContext(teCtxJPAPCInfoMap.get(key)));
        }

        HashMap<String, MessagingClientContext> txCtxMsgClientMap = teCtx.getMsgClientMap();
        for (String key : txCtxMsgClientMap.keySet()) {
            try {
                msgClientMap.put(key, (txCtxMsgClientMap.get(key).clone()));
            } catch (CloneNotSupportedException e) {
                // Shouldn't happen, clone() is supported.
            }
        }

        properties.putAll(teCtx.getProperties());

        this.testSessionSig = new TestSessionSignature(name);
    }

    public final String getName() {
        return name;
    }

    public final String getTestLogicClassName() {
        return testLogicClassName;
    }

    public final String getTestLogicMethod() {
        return testLogicMethod;
    }

    public final TestSessionSignature getTestSessionSig() {
        return testSessionSig;
    }

    public final HashMap<String, JPAPersistenceContext> getJpaPCInfoMap() {
        return jpaPCInfoMap;
    }

    public final HashMap<String, MessagingClientContext> getMsgClientMap() {
        return msgClientMap;
    }

    public final HashMap<String, java.io.Serializable> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "TestExecutionContext [name=" + name +
               ", testLogicClassName=" + testLogicClassName +
               ", testLogicMethod=" + testLogicMethod +
               ", testSessionSig=" + testSessionSig +
               ", jpaPCInfoMap=" + jpaPCInfoMap +
               ", msgClientMap=" + msgClientMap +
               ", properties=" + properties + "]";
    }
}
