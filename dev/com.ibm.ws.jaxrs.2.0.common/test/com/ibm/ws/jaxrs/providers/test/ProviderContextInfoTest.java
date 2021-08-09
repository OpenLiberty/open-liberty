/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.providers.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class ProviderContextInfoTest {

    static Field processingRequiredField;
    static Field fieldNamesField;
    static Field methodNamesField;
    static {
        Class<?> providerContextInfoClass = null;
        try {
            providerContextInfoClass = Class.forName("org.apache.cxf.jaxrs.model.AbstractResourceInfo$ProviderContextInfo");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Field[] fields = providerContextInfoClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("processingRequired")) {
                processingRequiredField = field;
                processingRequiredField.setAccessible(true);
            } else if (field.getName().equals("fieldNames")) {
                fieldNamesField = field;
                fieldNamesField.setAccessible(true);
            } else if (field.getName().equals("methodNames")) {
                methodNamesField = field;
                methodNamesField.setAccessible(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validateProviderContextInfo() throws Exception {
        Field contextPropsField = org.apache.cxf.jaxrs.model.AbstractResourceInfo.class.getDeclaredField("CONTEXT_PROPS");
        contextPropsField.setAccessible(true);
        Map<String, Object> contextProps = (Map<String, Object>) contextPropsField.get(null);
        for (String className : contextProps.keySet()) {
            System.out.println("Checking " + className);
            Class<?> clazz = Class.forName(className);
            Node rootNode = getContextInfo(clazz, null);
            checkNode(contextProps, rootNode);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkNode(Map<String, Object> contextProps, Node node) throws Exception {
        Object contextProp = contextProps.get(node.className);
        if (contextProp == null) {
            Assert.assertTrue(node.parent != null && !node.parent.needsProcessing);
        } else {
            Assert.assertEquals(node.needsProcessing, processingRequiredField.get(contextProp));
            Assert.assertTrue(compareCollections(node.fieldNames, (Collection<String>) fieldNamesField.get(contextProp)));
            Assert.assertTrue(compareCollections(node.methodNames, (Collection<String>) methodNamesField.get(contextProp)));
        }
        List<Node> children = node.children;
        if (children != null) {
            for (Node child : children) {
                checkNode(contextProps, child);
            }
        }
    }

    private boolean compareCollections(Collection<String> expected, Collection<String> actual) {
        if (expected == actual) {
            return true;
        }

        if (expected == null || actual == null) {
            System.out.println(expected + " " + actual);
            return false;
        }
        if (expected.size() != actual.size()) {
            System.out.println(expected + " " + actual);
            return false;
        }

        for (String exp : expected) {
            if (!actual.contains(exp)) {
                System.out.println(expected + " " + actual);
                return false;
            }
        }
        return true;
    }

    private Node getContextInfo(Class<?> clazz, Node parent) {
        Set<String> fContexts = null;
        Set<String> mContexts = null;
        Field[] contextFields = clazz.getDeclaredFields();
        for (Field field : contextFields) {
            Annotation[] annos = field.getAnnotations();
            for (Annotation anno : annos) {
                if (anno.annotationType() == Context.class) {
                    if (fContexts == null) {
                        fContexts = new HashSet<String>();
                    }
                    fContexts.add(field.getName());
                }
            }
        }
        Method[] contextMethods = clazz.getMethods();
        for (Method method : contextMethods) {
            if (!method.getName().startsWith("set") || method.getParameterTypes().length != 1) {
                continue;
            }
            Annotation[] annos = method.getAnnotations();
            for (Annotation anno : annos) {
                if (anno.annotationType() == Context.class) {
                    Class<?> parm = method.getParameterTypes()[0];
                    if (parm.isInterface() || parm == Application.class) {
                        if (mContexts == null) {
                            mContexts = new HashSet<String>();
                        }
                        mContexts.add(method.getName());
                    }
                }
            }
        }
        Node node = new Node(clazz.getName(), fContexts, mContexts, parent);

        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            node.addChild(getContextInfo(superClazz, node));
        }
        Class<?>[] intfs = clazz.getInterfaces();
        for (Class<?> intf : intfs) {
            node.addChild(getContextInfo(intf, node));
        }
        return node;
    }

    private static class Node {
        final String className;
        final Set<String> fieldNames;
        final Set<String> methodNames;
        final Node parent;
        List<Node> children;
        boolean needsProcessing = false;

        /**
         * @param className
         * @param fieldNames
         * @param methodNames
         */
        Node(String className, Set<String> fieldNames, Set<String> methodNames, Node parent) {
            super();
            this.className = className;
            this.fieldNames = fieldNames;
            this.methodNames = methodNames;
            this.parent = parent;
            if (fieldNames != null || methodNames != null) {
                needsProcessing = true;
                Node currentParent = parent;
                while (currentParent != null) {
                    if (currentParent.needsProcessing) {
                        break;
                    }
                    currentParent.needsProcessing = true;
                    currentParent = currentParent.parent;
                }
            }
        }

        void addChild(Node child) {
            if (children == null) {
                children = new ArrayList<>();
            }
            children.add(child);
        }
    }

}
