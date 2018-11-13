/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.test.methodfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.cdi.config.MethodFinder;
import com.ibm.ws.microprofile.faulttolerance.cdi.test.methodfinder.GenericInnerSearchA.GenericInnerSearchA1;
import com.ibm.ws.microprofile.faulttolerance.cdi.test.methodfinder.GenericInnerSearchB.GenericInnerSearchB1;

public class MethodFinderTest {

    @Test
    public void testSimple() {
        Method source = getSource(SimpleSearch.class);
        Method target = getTarget(SimpleSearch.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testPrivate() {
        Method source = getSource(PrivateSearch.class);
        Method target = getTarget(PrivateSearch.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testSuperclass() {
        Method source = getSource(SuperclassSearchA.class);
        Method target = getTarget(SuperclassSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testGeneric() {
        Method source = getSource(GenericSearchA.class);
        Method target = getTarget(GenericSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testGenericArray() {
        Method source = getSource(GenericArraySearchA.class);
        Method target = getTarget(GenericArraySearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testGenericLong() {
        Method source = getSource(GenericLongSearchA.class);
        Method target = getTarget(GenericLongSearchC.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testComplexGeneric() {
        Method source = getSource(GenericComplexSearchA.class);
        Method target = getTarget(GenericComplexSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testInPackage() {
        Method source = getSource(InPackageSearchA.class);
        Method target = getTarget(InPackageSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testAbstract() {
        Method source = getSource(AbstractSearch.class);
        Method target = getTarget(AbstractSearch.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testInterface() {
        Method source = getSource(InterfaceSearchA.class);
        Method target = getTarget(InterfaceSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testSuperclassPrivateNegative() {
        Method source = getSource(SuperclassPrivateSearchA.class);
        // Method target = getTarget(SuperclassPrivateSearchB.class);
        // target on SuperclassPrivateSearchB not found because it is private and not in the original class
        assertNull(MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testOutOfPackageNegative() {
        Method source = getSource(OutOfPackageSearchA.class);
        // Method target = getTarget(OutOfPackageSearchB.class);
        // target not found because it's package-scoped and not in the original package
        assertNull(MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testSubclassSeachNegative() throws Exception {
        // Note source method is a public method on SubclassSearchB
        //      target method is a public method on SubclassSearchA
        Method source = SubclassSearchA.class.getMethod("source", Integer.TYPE, Long.class);
        // Even though we retrieve the method through SubclassSearchA, the search should begin on the declaring class (SubclassSearchB) and should not search subclasses.
        assertNull(MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testWildcard() {
        Method source = getSource(WildcardSearch.class);
        Method target = getTarget(WildcardSearch.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testWildcardNegative() {
        Method source = getSource(WildcardNegativeSearch.class);
        //Method target = getTarget(WildcardNegativeSearch.class);
        assertNull(MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testGenericWildcard() {
        Method source = getSource(GenericWildcardSearchA.class);
        Method target = getTarget(GenericWildcardSearchB.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    public void testVarargs() {
        Method source = getSource(VarargsSearch.class);
        Method target = getTarget(VarargsSearch.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    @Test
    @Ignore // Not needed or supported as non-static inner classes cannot be beans
    public void testInnerClass() {
        Method source = getSource(GenericInnerSearchA1.class);
        Method target = getTarget(GenericInnerSearchB1.class);
        assertEquals(target, MethodFinder.findMatchingMethod(source, "target"));
    }

    private Method getSource(Class<?> clazz) {
        return getMethod(clazz, "source");
    }

    private Method getTarget(Class<?> clazz) {
        return getMethod(clazz, "target");
    }

    private Method getMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }

            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new AssertionError("No method named " + name + " found on " + clazz);
    }

}
