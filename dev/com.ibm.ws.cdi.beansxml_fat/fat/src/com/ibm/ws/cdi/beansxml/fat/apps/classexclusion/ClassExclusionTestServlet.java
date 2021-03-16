/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.classexclusion;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedByComboBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedByPropertyBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedPackageBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IExcludedPackageTreeBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IIncludedBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IProtectedByClassBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IProtectedByHalfComboBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IVetoedBean;

import componenttest.app.FATServlet;

@WebServlet("/test")
public class ClassExclusionTestServlet extends FATServlet {

    @Inject
    IIncludedBean included;
    @Inject
    IExcludedBean excluded;
    @Inject
    IExcludedPackageBean excludedPackageBean;
    @Inject
    IExcludedPackageTreeBean excludedPackageTreeBean;
    @Inject
    IProtectedByClassBean protectedByClassBean;
    @Inject
    IExcludedByPropertyBean excludedByPropertyBean;
    @Inject
    IExcludedByComboBean excludedByComboBean;
    @Inject
    IProtectedByHalfComboBean proectedByHalfComboBean;
    @Inject
    IVetoedBean vetoedBean;

    private static final long serialVersionUID = 8549700799591343964L;

    @Test
    public void testIncludedBean() throws Exception {
        assertEquals("IncludedBean was correctly injected", included.getOutput());
    }

    @Test
    public void testExcludedBean() throws Exception {
        assertEquals("ExcludedBean was correctly rejected", excluded.getOutput());
    }

    @Test
    public void testExcludedPackageBean() throws Exception {
        assertEquals("ExcludedPackageBean was correctly rejected", excludedPackageBean.getOutput());
    }

    @Test
    public void testExcludedPackageTreeBean() throws Exception {
        assertEquals("ExcludedPackageTreeBean was correctly rejected", excludedPackageTreeBean.getOutput());
    }

    @Test
    public void testProtectedByClassBean() throws Exception {
        assertEquals("ProtectedByClassBean was correctly injected", protectedByClassBean.getOutput());
    }

    @Test
    public void testExcludedByPropertyBean() throws Exception {
        assertEquals("ExcludedByPropertyBean was correctly rejected", excludedByPropertyBean.getOutput());
    }

    @Test
    public void testExcludedByComboBean() throws Exception {
        assertEquals("ExcludedByComboBean was correctly rejected", excludedByComboBean.getOutput());
    }

    @Test
    public void testProtectedByHalfComboBean() throws Exception {
        assertEquals("ProtectedByHalfComboBean was correctly injected", proectedByHalfComboBean.getOutput());
    }

    @Test
    public void testVetoedBean() throws Exception {
        assertEquals("VetoedBean was correctly rejected", vetoedBean.getOutput());
    }

}
