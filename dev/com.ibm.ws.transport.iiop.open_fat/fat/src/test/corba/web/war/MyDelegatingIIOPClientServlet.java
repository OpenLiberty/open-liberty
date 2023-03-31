/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package test.corba.web.war;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet("/MyDelegatingIIOPClientServlet")
public class MyDelegatingIIOPClientServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private MyIIOPClientBean bean;

    public void intToInt() throws Exception {
        bean.intToInt();
    }

    public void intToInteger() throws Exception {
        bean.intToInteger();
    }

    public void integerToInteger() throws Exception {
        bean.integerToInteger();
    }

    public void stringToString() throws Exception {
        bean.stringToString();
    }

    public void intToObject() throws Exception {
        bean.intToObject();
    }

    public void stringToObject() throws Exception {
        bean.stringToObject();
    }

    public void dateToObject() throws Exception {
        bean.dateToObject();
    }

    public void stubToObject() throws Exception {
        bean.stubToObject();
    }

    public void testClassToObject() throws Exception {
        bean.testClassToObject();
    }

    public void userFeatureToObject() throws Exception {
        bean.userFeatureToObject();
    }

    public void intArrToObject() throws Exception {
        bean.intArrToObject();
    }

    public void stringArrToObject() throws Exception {
        bean.stringArrToObject();
    }

    public void dateArrToObject() throws Exception {
        bean.dateArrToObject();
    }

    public void stubArrToObject() throws Exception {
        bean.stubArrToObject();
    }

    public void testClassArrToObject() throws Exception {
        bean.testClassArrToObject();
    }

    public void userFeatureArrToObject() throws Exception {
        bean.userFeatureArrToObject();
    }

    public void intToSerializable() throws Exception {
        bean.intToSerializable();
    }

    public void stringToSerializable() throws Exception {
        bean.stringToSerializable();
    }

    public void dateToSerializable() throws Exception {
        bean.dateToSerializable();
    }

    public void stubToSerializable() throws Exception {
        bean.stubToSerializable();
    }

    public void testClassToSerializable() throws Exception {
        bean.testClassToSerializable();
    }

    public void userFeatureToSerializable() throws Exception {
        bean.userFeatureToSerializable();
    }

    public void intArrToSerializable() throws Exception {
        bean.intArrToSerializable();
    }

    public void stringArrToSerializable() throws Exception {
        bean.stringArrToSerializable();
    }

    public void dateArrToSerializable() throws Exception {
        bean.dateArrToSerializable();
    }

    public void stubArrToSerializable() throws Exception {
        bean.stubArrToSerializable();
    }

    public void testClassArrToSerializable() throws Exception {
        bean.testClassArrToSerializable();
    }

    public void userFeatureArrToSerializable() throws Exception {
        bean.userFeatureArrToSerializable();
    }

    public void stubToEjbIface() throws Exception {
        bean.stubToEjbIface();
    }

    public void stubToRemote() throws Exception {
        bean.stubToRemote();
    }

    public void testClassToTestClass() throws Exception {
        bean.testClassToTestClass();
    }

    public void intArrToIntArr() throws Exception {
        bean.intArrToIntArr();
    }

    public void stringArrToStringArr() throws Exception {
        bean.stringArrToStringArr();
    }

    public void stringArrToObjectArr() throws Exception {
        bean.stringArrToObjectArr();
    }

    public void dateArrToObjectArr() throws Exception {
        bean.dateArrToObjectArr();
    }

    public void stubArrToObjectArr() throws Exception {
        bean.stubArrToObjectArr();
    }

    public void testClassArrToObjectArr() throws Exception {
        bean.testClassArrToObjectArr();
    }

    public void userFeatureArrToObjectArr() throws Exception {
        bean.userFeatureArrToObjectArr();
    }

    public void stringArrToSerializableArr() throws Exception {
        bean.stringArrToSerializableArr();
    }

    public void dateArrToSerializableArr() throws Exception {
        bean.dateArrToSerializableArr();
    }

    public void stubArrToSerializableArr() throws Exception {
        bean.stubArrToSerializableArr();
    }

    public void testClassArrToSerializableArr() throws Exception {
        bean.testClassArrToSerializableArr();
    }

    public void userFeatureArrToSerializableArr() throws Exception {
        bean.userFeatureArrToSerializableArr();
    }

    public void stubArrToEjbIfaceArr() throws Exception {
        bean.stubArrToEjbIfaceArr();
    }

    public void stubArrToRemoteArr() throws Exception {
        bean.stubArrToRemoteArr();
    }

    public void testClassArrToTestClassArr() throws Exception {
        bean.testClassArrToTestClassArr();
    }

    public void enumToObject() throws Exception {
        bean.enumToObject();
    }

    public void enumToSerializable() throws Exception {
        bean.enumToSerializable();
    }

    public void timeUnitToObject() throws Exception {
        bean.enumToObject();
    }

    public void timeUnitToSerializable() throws Exception {
        bean.enumToSerializable();
    }

    public void cmsfv2ChildDataToObject() throws Exception {
        bean.cmsfv2ChildDataToObject();
    }

    public void cmsfv2ChildDataToSerializable() throws Exception {
        bean.cmsfv2ChildDataToSerializable();
    }

    public void testIDLEntityToObject() throws Exception {
        bean.testIDLEntityToObject();
    }

    public void testIDLEntityToSerializable() throws Exception {
        bean.testIDLEntityToSerializable();
    }

    public void testIDLEntityToIDLEntity() throws Exception {
        bean.testIDLEntityToIDLEntity();
    }

    public void testIDLEntityArrToIDLEntityArr() throws Exception {
        bean.testIDLEntityArrToIDLEntityArr();
    }

    public void testTwoLongsToTwoLongs() throws Exception {
        bean.testTwoLongsToTwoLongs();
    }
}
