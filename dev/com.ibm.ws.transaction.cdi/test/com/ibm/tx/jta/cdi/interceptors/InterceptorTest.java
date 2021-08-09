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
import javax.transaction.Transactional;

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
import com.ibm.tx.jta.cdi.interceptors.beans.SimpleStereotypeMethodAnnotated;

// Tests ability to find interceptors on classes/methods in various situations
// from simple interceptors up to complex trees of stereotypes. Note that for all
// the class-based tests, we construct anonymous inner classes extending the
// annotated classes we're passing in, to simulate the way WELD proxies the
// bean classes. Note that our custom annotations are not marked @Inherited
public class InterceptorTest extends TransactionalInterceptor {

    private static final long serialVersionUID = 1L;

    @Test
    public void testNoAnnotation() {
        Transactional t = null;
        try {
            t = getTransactionalAnnotation(createContext(new BaseBean()), null);
            fail("Failed to not find Transactional annotation");
        } catch (Exception e) {
            assertNull(t);
        }
    }

    @Test
    public void testSimpleMethodAnnotation() {
        try {
            assertEquals(Exception.class,
                         getTransactionalAnnotation(createContext(new SimpleInterceptorMethodAnnotated()), Transactional.TxType.MANDATORY.toString()).rollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleMethodAnnotationExtension() {
        try {
            assertEquals(Exception.class,
                         getTransactionalAnnotation(createContext(new SimpleInterceptorMethodAnnotatedExtension()), Transactional.TxType.MANDATORY.toString()).rollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleClassAnnotation() {
        try {
            assertEquals(RuntimeException.class,
                         getTransactionalAnnotation(createContext(new SimpleInterceptorClassAnnotated() {}), Transactional.TxType.MANDATORY.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleClassAnnotationExtension() {
        try {
            assertEquals(RuntimeException.class,
                         getTransactionalAnnotation(createContext(new SimpleInterceptorClassAnnotatedExtension() {}), Transactional.TxType.MANDATORY.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleStereotypeAnnotation() {
        try {
            assertEquals(NoSuchMethodException.class,
                         getTransactionalAnnotation(createContext(new SimpleStereotypeClassAnnotated() {}), Transactional.TxType.REQUIRED.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleStereotypeMethodAnnotation() {
        try {
            assertEquals(NoSuchMethodException.class,
                         getTransactionalAnnotation(createContext(new SimpleStereotypeMethodAnnotated() {}), Transactional.TxType.REQUIRED.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSimpleStereotypeAnnotationInheritance() {
        try {
            assertEquals(NoSuchMethodException.class,
                         getTransactionalAnnotation(createContext(new SimpleStereotypeInheritance() {}), Transactional.TxType.REQUIRED.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testComplexStereotypeAnnotationInheritance() {
        try {
            assertEquals(NoSuchMethodException.class,
                         getTransactionalAnnotation(createContext(new ComplexStereotypeInheritance() {}), Transactional.TxType.REQUIRED.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testComplexStereotypeAnnotationInheritanceOtherOrder() {
        try {
            assertEquals(NoSuchMethodException.class,
                         getTransactionalAnnotation(createContext(new ComplexStereotypeInheritanceOtherOrder() {}), Transactional.TxType.REQUIRED.toString()).dontRollbackOn()[0]);
        } catch (Exception e) {
            fail(e.getMessage());
        }
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