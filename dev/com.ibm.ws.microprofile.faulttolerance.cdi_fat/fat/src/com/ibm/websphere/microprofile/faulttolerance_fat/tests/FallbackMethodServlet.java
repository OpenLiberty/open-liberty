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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.AbstractSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericArraySearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericComplexSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericLongSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.GenericWildcardSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InPackageSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.InterfaceSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.PrivateSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SimpleSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.SuperclassSearchA;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.VarargsSearch;
import com.ibm.websphere.microprofile.faulttolerance_fat.fallbackMethod.WildcardSearch;

import componenttest.app.FATServlet;

/**
 * Test to check we can call fallback methods from different locations
 */
@WebServlet("/fallbackMethods")
public class FallbackMethodServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private SimpleSearch simpleBean;

    @Test
    public void testSimple() {
        assertThat(simpleBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private PrivateSearch privateBean;

    @Test
    public void testPrivate() {
        assertThat(privateBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private SuperclassSearchA superclassBean;

    @Test
    public void testSuperclass() {
        assertThat(superclassBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private InPackageSearchA inPackageBean;

    @Test
    public void testInPackage() {
        assertThat(inPackageBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private GenericSearchA genericBean;

    @Test
    public void testGeneric() {
        assertThat(genericBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private GenericLongSearchA genericLongBean;

    @Test
    public void testGenericLong() {
        assertThat(genericLongBean.source("foo"), equalTo("target"));
    }

    @Inject
    private GenericComplexSearchA genericComplexBean;

    @Test
    public void testGenericComplex() {
        assertThat(genericComplexBean.source(null), equalTo("target"));
    }

    @Inject
    private GenericArraySearchA genericArrayBean;

    @Test
    public void testGenericArray() {
        assertThat(genericArrayBean.source(null), equalTo("target"));
    }

    @Inject
    private AbstractSearchA abstractBean;

    @Test
    public void testAbstract() {
        assertThat(abstractBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private InterfaceSearchA interfaceBean;

    @Test
    public void testInterface() {
        assertThat(interfaceBean.source(1, 2L), equalTo("target"));
    }

    @Inject
    private WildcardSearch wildcardBean;

    @Test
    public void testWildcard() {
        assertThat(wildcardBean.source(Collections.emptyList()), equalTo("target"));
    }

    @Inject
    private GenericWildcardSearchA genericWildcardBean;

    @Test
    public void testGenericWildcard() {
        assertThat(genericWildcardBean.source(Collections.emptyList()), equalTo("target"));
    }

    @Inject
    private VarargsSearch varargsBean;

    @Test
    public void testVarargs() {
        assertThat(varargsBean.source(1, 2L, 3L), equalTo("target"));
    }

}
