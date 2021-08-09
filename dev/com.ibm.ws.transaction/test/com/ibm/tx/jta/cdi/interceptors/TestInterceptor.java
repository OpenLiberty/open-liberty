/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.cdi.interceptors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.interceptor.InvocationContext;

import org.junit.Test;

import com.ibm.tx.jta.cdi.interceptors.beans.BaseBean;
import com.ibm.tx.jta.cdi.interceptors.beans.ComplexStereotypeInheritance;
import com.ibm.tx.jta.cdi.interceptors.beans.ComplexStereotypeInheritanceOtherOrder;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleInterceptorClassAnnotated;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleInterceptorClassAnnotatedExtension;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleInterceptorMethodAnnotated;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleInterceptorMethodAnnotatedExtension;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleStereotypeClassAnnotated;
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleStereotypeInheritance;

// Tests ability to find interceptors on classes/methods in various situations
// from simple interceptors up to complex trees of stereotypes. Note that for all
// the class-based tests, we construct anonymous inner classes extending the
// annotated classes we're passing in, to simulate the way WELD proxies the
// bean classes. Note that our custom annotations are not marked @Inherited
public class TestInterceptor extends com.ibm.tx.jta.cdi.interceptors.TransactionalInterceptor {

    private static final long serialVersionUID = 1L;

    @Test
    public void testNoAnnotation() throws Exception {
        try {
            assertNull(getTransactionalAnnotation(createContext(new BaseBean()), "TEST"));
            fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void testSimpleMethodAnnotation() throws Exception {
        assertEquals(Exception.class, getTransactionalAnnotation(createContext(new SimpleInterceptorMethodAnnotated()), "TEST").rollbackOn()[0]);
    }

    @Test
    public void testSimpleMethodAnnotationExtension() throws Exception {
        assertEquals(Exception.class, getTransactionalAnnotation(createContext(new SimpleInterceptorMethodAnnotatedExtension() {}), "TEST").rollbackOn()[0]);
    }

    @Test
    public void testSimpleClassAnnotation() throws Exception {
        assertEquals(RuntimeException.class, getTransactionalAnnotation(createContext(new SimpleInterceptorClassAnnotated() {}), "TEST").dontRollbackOn()[0]);
    }

    @Test
    public void testSimpleClassAnnotationExtension() throws Exception {
        assertEquals(RuntimeException.class, getTransactionalAnnotation(createContext(new SimpleInterceptorClassAnnotatedExtension() {}), "TEST").dontRollbackOn()[0]);
    }

    @Test
    public void testSimpleStereotypeAnnotation() throws Exception {
        assertEquals(NoSuchMethodException.class, getTransactionalAnnotation(createContext(new SimpleStereotypeClassAnnotated() {}), "TEST").dontRollbackOn()[0]);
    }

    @Test
    public void testSimpleStereotypeAnnotationInheritance() throws Exception {
        assertEquals(NoSuchMethodException.class, getTransactionalAnnotation(createContext(new SimpleStereotypeInheritance() {}), "TEST").dontRollbackOn()[0]);
    }

    @Test
    public void testComplexStereotypeAnnotationInheritance() throws Exception {
        assertEquals(NoSuchMethodException.class, getTransactionalAnnotation(createContext(new ComplexStereotypeInheritance() {}), "TEST").dontRollbackOn()[0]);
    }

    @Test
    public void testComplexStereotypeAnnotationInheritanceOtherOrder() throws Exception {
        assertEquals(NoSuchMethodException.class, getTransactionalAnnotation(createContext(new ComplexStereotypeInheritanceOtherOrder() {}), "TEST").dontRollbackOn()[0]);
    }

    private InvocationContext createContext(final Object target) {
        return new InvocationContext() {

            @Override
            public void setParameters(Object[] arg0) {}

            @Override
            public Object proceed() throws Exception {
                return null;
            }

            @Override
            public Object getTimer() {
                return null;
            }

            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Object[] getParameters() {
                return null;
            }

            @Override
            public Method getMethod() {
                try {
                    return target.getClass().getMethod("baseMethod");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Couldn't find method - should never happen!");
                } catch (SecurityException e) {
                    throw new RuntimeException("Couldn't find method - should never happen!");
                }
            }

            @Override
            public Map<String, Object> getContextData() {
                return null;
            }

            @Override
            public Constructor<?> getConstructor() {
                return null;
            }
        };
    }

}
