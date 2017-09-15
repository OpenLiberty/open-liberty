/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.injection.mock.MockInjectionBinding;
import com.ibm.ws.injectionengine.osgi.internal.OSGiInjectionScopeData;
import com.ibm.wsspi.injectionengine.InjectionBinding;

public class OSGiInjectionScopeDataTest {
    private OSGiInjectionScopeData createComp() {
        return new OSGiInjectionScopeData(null, NamingConstants.JavaColonNamespace.COMP, null, null);
    }

    @Test
    public void testCompAllowed() {
        OSGiInjectionScopeData scopeData = createComp();
        Assert.assertFalse(scopeData.isCompAllowed());
        Assert.assertNotNull(scopeData.compLock());
        Assert.assertTrue(scopeData.isCompAllowed());
    }

    @Test
    public void testBindings() throws Exception {
        List<NamingConstants.JavaColonNamespace> bindNamespaces = Arrays.asList(NamingConstants.JavaColonNamespace.COMP,
                                                                                NamingConstants.JavaColonNamespace.COMP_ENV,
                                                                                NamingConstants.JavaColonNamespace.MODULE,
                                                                                NamingConstants.JavaColonNamespace.APP,
                                                                                NamingConstants.JavaColonNamespace.GLOBAL);
        for (NamingConstants.JavaColonNamespace bindNamespace : bindNamespaces) {
            NamingConstants.JavaColonNamespace createNamespace =
                            bindNamespace == NamingConstants.JavaColonNamespace.COMP_ENV ?
                                            NamingConstants.JavaColonNamespace.COMP :
                                            bindNamespace;
            ReentrantReadWriteLock nonCompLock =
                            createNamespace == NamingConstants.JavaColonNamespace.COMP ?
                                            null :
                                            new ReentrantReadWriteLock();
            OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, createNamespace, null, nonCompLock);
            scopeData.compLock();

            for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
                Assert.assertNull(bindNamespace + "+" + namespace, scopeData.getInjectionBinding(namespace, "name"));
                Assert.assertNull(bindNamespace + "+" + namespace, scopeData.getInjectionBinding(namespace, "doesnotexist"));
                Assert.assertFalse(bindNamespace + "+" + namespace, scopeData.hasObjectWithPrefix(namespace, ""));
                Assert.assertTrue(bindNamespace + "+" + namespace, scopeData.listInstances(bindNamespace, "").isEmpty());
            }

            final String name = "name";
            final String value = "value";
            InjectionBinding<?> binding = new MockInjectionBinding(value);
            binding.setObjects(value, null);
            Map<Class<?>, Map<String, InjectionBinding<?>>> newBindings = null;
            OSGiInjectionScopeData contributorScopeData = null;

            if (bindNamespace == NamingConstants.JavaColonNamespace.COMP) {
                scopeData.compLock().writeLock().lock();
                scopeData.addCompBinding(name, binding);
                scopeData.compLock().writeLock().unlock();
            } else if (bindNamespace == NamingConstants.JavaColonNamespace.COMP_ENV) {
                Map<String, InjectionBinding<?>> bindings = Collections.<String, InjectionBinding<?>> singletonMap(name, binding);

                scopeData.compLock().writeLock().lock();
                scopeData.addCompEnvBindings(bindings);
                scopeData.compLock().writeLock().unlock();
            } else {
                String qualifiedName = bindNamespace.prefix() + name;
                binding.setJndiName(qualifiedName);

                Map<String, InjectionBinding<?>> savedBindings = new HashMap<String, InjectionBinding<?>>();
                savedBindings.put(qualifiedName, binding);
                newBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
                newBindings.put(Object.class, savedBindings);

                contributorScopeData = bindNamespace == NamingConstants.JavaColonNamespace.MODULE ? null : createComp();

                nonCompLock.writeLock().lock();
                scopeData.addNonCompBindings(newBindings, contributorScopeData);
                scopeData.addNonCompBindings(newBindings, contributorScopeData);
                nonCompLock.writeLock().unlock();
            }

            OSGiInjectionScopeData contributorScopeData2 = null;

            // For java:comp/:module, ensure that the data is present.
            // For non-java:comp/:module, we do several rounds:
            // 1.  Ensure data is present.
            // 2.  Ensure data is present with a second contributor.
            // 3.  Ensure data is present after removing the first contributor.
            int numRounds = contributorScopeData == null ? 1 : 3;
            for (int round = 0; round < numRounds; round++) {
                Assert.assertTrue(bindNamespace.toString(), scopeData.hasObjectWithPrefix(bindNamespace, ""));
                Assert.assertSame(bindNamespace.toString(), binding, scopeData.getInjectionBinding(bindNamespace, name));
                Assert.assertEquals(bindNamespace.toString(), 1, scopeData.listInstances(bindNamespace, "").size());

                for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
                    if (namespace != bindNamespace) {
                        Assert.assertNull(bindNamespace + "+" + namespace, scopeData.getInjectionBinding(namespace, "name"));
                        Assert.assertNull(bindNamespace + "+" + namespace, scopeData.getInjectionBinding(namespace, "doesnotexist"));
                        Assert.assertFalse(bindNamespace + "+" + namespace, scopeData.hasObjectWithPrefix(namespace, ""));
                        Assert.assertTrue(bindNamespace + "+" + namespace, scopeData.listInstances(namespace, "").isEmpty());
                    }
                }

                if (contributorScopeData != null) {
                    if (round == 0) {
                        contributorScopeData2 = createComp();
                        nonCompLock.writeLock().lock();
                        scopeData.validateNonCompBindings(newBindings);
                        scopeData.addNonCompBindings(newBindings, contributorScopeData2);
                        // Contribute again: dynamic servlet code path.
                        scopeData.addNonCompBindings(newBindings, contributorScopeData2);
                        nonCompLock.writeLock().unlock();
                    } else if (round == 1) {
                        contributorScopeData.destroy();
                    }
                }
            }

            // Finally, for non-java:comp, ensure the data is not present after
            // removing the second contributor.
            if (contributorScopeData2 != null) {
                contributorScopeData2.destroy();

                Assert.assertNull(bindNamespace.toString(), scopeData.getInjectionBinding(bindNamespace, name));
                Assert.assertFalse(bindNamespace.toString(), scopeData.hasObjectWithPrefix(bindNamespace, ""));
            }
        }
    }

    @Test
    public void testAddDeferredReferenceData() {
        OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, null, null, null);
        TestDeferredReferenceData refData = new TestDeferredReferenceData(null);
        scopeData.addDeferredReferenceData(refData);

        Assert.assertFalse("DeferredReferenceData should not be called when added", refData.called);
    }

    @Test
    public void testRemoveDeferredReferenceData() {
        OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, null, null, null);
        TestDeferredReferenceData refData = new TestDeferredReferenceData(null);
        // Ensure no exceptions.
        scopeData.removeDeferredReferenceData(refData);

        scopeData.addDeferredReferenceData(refData);
        scopeData.removeDeferredReferenceData(refData);
        Assert.assertFalse("Should not have processed anything", scopeData.processDeferredReferenceData());
        Assert.assertFalse("Should not have called refData", refData.called);
    }

    @Test
    public void testProcessDeferredReferenceData() {
        OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, null, null, null);
        TestDeferredReferenceData refData = new TestDeferredReferenceData(true);
        scopeData.addDeferredReferenceData(refData);

        Assert.assertTrue("Should propagate TestDeferredReferenceData.returnValue", scopeData.processDeferredReferenceData());
        Assert.assertTrue("DeferredReferenceData should have been called", refData.called);

        refData.called = false;
        Assert.assertFalse("Should not have processed anything", scopeData.processDeferredReferenceData());
        Assert.assertFalse("DeferredReferenceData should not be called again", refData.called);
    }

    @Test
    public void testProcessDeferredReferenceDataException() {
        OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, null, null, null);
        TestDeferredReferenceData refData = new TestDeferredReferenceData(null);
        scopeData.addDeferredReferenceData(refData);

        Assert.assertFalse("Should not have processed anything", scopeData.processDeferredReferenceData());
        Assert.assertTrue("DeferredReferenceData should have been called", refData.called);

        refData.called = false;
        Assert.assertFalse("Should not have processed anything", scopeData.processDeferredReferenceData());
        Assert.assertFalse("DeferredReferenceData should not be called again", refData.called);
    }

    @Test
    public void testProcessDeferredReferenceDataMultiple() {
        OSGiInjectionScopeData scopeData = new OSGiInjectionScopeData(null, null, null, null);
        TestDeferredReferenceData refData1 = new TestDeferredReferenceData(null);
        scopeData.addDeferredReferenceData(refData1);
        TestDeferredReferenceData refData2 = new TestDeferredReferenceData(true);
        scopeData.addDeferredReferenceData(refData2);

        Assert.assertTrue("Should have processed refData2", scopeData.processDeferredReferenceData());
        Assert.assertTrue("refData1 should have been called", refData1.called);
        Assert.assertTrue("refData2 should have been called", refData2.called);
    }

    @Test
    public void testEnableDeferredReferenceData() {
        OSGiInjectionScopeData scopeData1 = new OSGiInjectionScopeData(null, null, null, null);
        OSGiInjectionScopeData scopeData2 = new OSGiInjectionScopeData(null, null, scopeData1, null);
        TestDeferredReferenceData refData = new TestDeferredReferenceData(true);
        scopeData2.addDeferredReferenceData(refData);

        Assert.assertFalse("Should not process until enabled", scopeData1.processDeferredReferenceData());
        Assert.assertFalse("Should not have been called until scope enabled", refData.called);

        scopeData2.enableDeferredReferenceData();
        Assert.assertTrue("Should have processed refData", scopeData1.processDeferredReferenceData());
        Assert.assertTrue("Should have called refData", refData.called);
        refData.called = false;

        Assert.assertFalse("Should not have processed anything", scopeData1.processDeferredReferenceData());
        Assert.assertFalse("Should not have called refData again", refData.called);

        scopeData2.addDeferredReferenceData(refData);
        Assert.assertTrue("Should have processed refData", scopeData1.processDeferredReferenceData());
        Assert.assertTrue("Should have been called refData", refData.called);
    }

    @Test
    public void testDestroy() {
        OSGiInjectionScopeData scopeData1 = new OSGiInjectionScopeData(null, null, null, null);
        OSGiInjectionScopeData scopeData2 = new OSGiInjectionScopeData(null, null, scopeData1, null) {
            @Override
            public boolean processDeferredReferenceData() {
                throw new UnsupportedOperationException();
            }
        };
        scopeData2.addDeferredReferenceData(new TestDeferredReferenceData(null));
        scopeData2.enableDeferredReferenceData();
        scopeData2.destroy();
        Assert.assertFalse("Should not have processed scopeData2 after destroy", scopeData1.processDeferredReferenceData());
    }
}
