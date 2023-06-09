/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.corba.web.war;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import shared.IIOPClientTests;

@WebServlet("/IIOPDelegatingClientServlet")
public class IIOPDelegatingServlet extends FATServlet implements IIOPClientTests {
    private static final long serialVersionUID = 1L;
    @EJB private IIOPClientBean bean;
    @Override @Test public void intToInt() throws Exception {bean.intToInt();}
    @Override @Test public void intToInteger() throws Exception {bean.intToInteger();}
    @Override @Test public void integerToInteger() throws Exception {bean.integerToInteger();}
    @Override @Test public void stringToString() throws Exception {bean.stringToString();}
    @Override @Test public void intToObject() throws Exception {bean.intToObject();}
    @Override @Test public void stringToObject() throws Exception {bean.stringToObject();}
    @Override @Test public void dateToObject() throws Exception {bean.dateToObject();}
    @Override @Test public void stubToObject() throws Exception {bean.stubToObject();}
    @Override @Test public void testClassToObject() throws Exception {bean.testClassToObject();}
    @Override @Test public void userFeatureToObject() throws Exception {bean.userFeatureToObject();}
    @Override @Test public void intArrToObject() throws Exception {bean.intArrToObject();}
    @Override @Test public void stringArrToObject() throws Exception {bean.stringArrToObject();}
    @Override @Test public void dateArrToObject() throws Exception {bean.dateArrToObject();}
    @Override @Test public void stubArrToObject() throws Exception {bean.stubArrToObject();}
    @Override @Test public void testClassArrToObject() throws Exception {bean.testClassArrToObject();}
    @Override @Test public void userFeatureArrToObject() throws Exception {bean.userFeatureArrToObject();}
    @Override @Test public void intToSerializable() throws Exception {bean.intToSerializable();}
    @Override @Test public void stringToSerializable() throws Exception {bean.stringToSerializable();}
    @Override @Test public void dateToSerializable() throws Exception {bean.dateToSerializable();}
    @Override @Test public void stubToSerializable() throws Exception {bean.stubToSerializable();}
    @Override @Test public void testClassToSerializable() throws Exception {bean.testClassToSerializable();}
    @Override @Test public void userFeatureToSerializable() throws Exception {bean.userFeatureToSerializable();}
    @Override @Test public void intArrToSerializable() throws Exception {bean.intArrToSerializable();}
    @Override @Test public void stringArrToSerializable() throws Exception {bean.stringArrToSerializable();}
    @Override @Test public void dateArrToSerializable() throws Exception {bean.dateArrToSerializable();}
    @Override @Test public void stubArrToSerializable() throws Exception {bean.stubArrToSerializable();}
    @Override @Test public void testClassArrToSerializable() throws Exception {bean.testClassArrToSerializable();}
    @Override @Test public void userFeatureArrToSerializable() throws Exception {bean.userFeatureArrToSerializable();}
    @Override @Test public void stubToEjbIface() throws Exception {bean.stubToEjbIface();}
    @Override @Test public void stubToRemote() throws Exception {bean.stubToRemote();}
    @Override @Test public void testClassToTestClass() throws Exception {bean.testClassToTestClass();}
    @Override @Test public void intArrToIntArr() throws Exception {bean.intArrToIntArr();}
    @Override @Test public void stringArrToStringArr() throws Exception {bean.stringArrToStringArr();}
    @Override @Test public void stringArrToObjectArr() throws Exception {bean.stringArrToObjectArr();}
    @Override @Test public void dateArrToObjectArr() throws Exception {bean.dateArrToObjectArr();}
    @Override @Test public void stubArrToObjectArr() throws Exception {bean.stubArrToObjectArr();}
    @Override @Test public void testClassArrToObjectArr() throws Exception {bean.testClassArrToObjectArr();}
    @Override @Test public void userFeatureArrToObjectArr() throws Exception {bean.userFeatureArrToObjectArr();}
    @Override @Test public void stringArrToSerializableArr() throws Exception {bean.stringArrToSerializableArr();}
    @Override @Test public void dateArrToSerializableArr() throws Exception {bean.dateArrToSerializableArr();}
    @Override @Test public void stubArrToSerializableArr() throws Exception {bean.stubArrToSerializableArr();}
    @Override @Test public void testClassArrToSerializableArr() throws Exception {bean.testClassArrToSerializableArr();}
    @Override @Test public void userFeatureArrToSerializableArr() throws Exception {bean.userFeatureArrToSerializableArr();}
    @Override @Test public void stubArrToEjbIfaceArr() throws Exception {bean.stubArrToEjbIfaceArr();}
    @Override @Test public void stubArrToRemoteArr() throws Exception {bean.stubArrToRemoteArr();}
    @Override @Test public void testClassArrToTestClassArr() throws Exception {bean.testClassArrToTestClassArr();}
    @Override @Test public void enumToObject() throws Exception {bean.enumToObject();}
    @Override @Test public void enumToSerializable() throws Exception {bean.enumToSerializable();}
    @Override @Test public void timeUnitToObject() throws Exception {bean.enumToObject();}
    @Override @Test public void timeUnitToSerializable() throws Exception {bean.enumToSerializable();}
    @Override @Test public void cmsfv2ChildDataToObject() throws Exception {bean.cmsfv2ChildDataToObject();}
    @Override @Test public void cmsfv2ChildDataToSerializable() throws Exception {bean.cmsfv2ChildDataToSerializable();}
    @Override @Test public void testIDLEntityToObject() throws Exception {bean.testIDLEntityToObject();}
    @Override @Test public void testIDLEntityToSerializable() throws Exception {bean.testIDLEntityToSerializable();}
    @Override @Test public void testIDLEntityToIDLEntity() throws Exception {bean.testIDLEntityToIDLEntity();}
    @Override @Test public void testIDLEntityArrToIDLEntityArr() throws Exception {bean.testIDLEntityArrToIDLEntityArr();}
    @Override @Test public void testTwoLongsToTwoLongs() throws Exception {bean.testTwoLongsToTwoLongs();}
}
