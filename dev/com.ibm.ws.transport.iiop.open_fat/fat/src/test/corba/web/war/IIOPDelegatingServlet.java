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
    @Test public void intToInt() throws Exception {bean.intToInt();}
    @Test public void intToInteger() throws Exception {bean.intToInteger();}
    @Test public void integerToInteger() throws Exception {bean.integerToInteger();}
    @Test public void stringToString() throws Exception {bean.stringToString();}
    @Test public void intToObject() throws Exception {bean.intToObject();}
    @Test public void stringToObject() throws Exception {bean.stringToObject();}
    @Test public void dateToObject() throws Exception {bean.dateToObject();}
    @Test public void stubToObject() throws Exception {bean.stubToObject();}
    @Test public void testClassToObject() throws Exception {bean.testClassToObject();}
    @Test public void userFeatureToObject() throws Exception {bean.userFeatureToObject();}
    @Test public void intArrToObject() throws Exception {bean.intArrToObject();}
    @Test public void stringArrToObject() throws Exception {bean.stringArrToObject();}
    @Test public void dateArrToObject() throws Exception {bean.dateArrToObject();}
    @Test public void stubArrToObject() throws Exception {bean.stubArrToObject();}
    @Test public void testClassArrToObject() throws Exception {bean.testClassArrToObject();}
    @Test public void userFeatureArrToObject() throws Exception {bean.userFeatureArrToObject();}
    @Test public void intToSerializable() throws Exception {bean.intToSerializable();}
    @Test public void stringToSerializable() throws Exception {bean.stringToSerializable();}
    @Test public void dateToSerializable() throws Exception {bean.dateToSerializable();}
    @Test public void stubToSerializable() throws Exception {bean.stubToSerializable();}
    @Test public void testClassToSerializable() throws Exception {bean.testClassToSerializable();}
    @Test public void userFeatureToSerializable() throws Exception {bean.userFeatureToSerializable();}
    @Test public void intArrToSerializable() throws Exception {bean.intArrToSerializable();}
    @Test public void stringArrToSerializable() throws Exception {bean.stringArrToSerializable();}
    @Test public void dateArrToSerializable() throws Exception {bean.dateArrToSerializable();}
    @Test public void stubArrToSerializable() throws Exception {bean.stubArrToSerializable();}
    @Test public void testClassArrToSerializable() throws Exception {bean.testClassArrToSerializable();}
    @Test public void userFeatureArrToSerializable() throws Exception {bean.userFeatureArrToSerializable();}
    @Test public void stubToEjbIface() throws Exception {bean.stubToEjbIface();}
    @Test public void stubToRemote() throws Exception {bean.stubToRemote();}
    @Test public void testClassToTestClass() throws Exception {bean.testClassToTestClass();}
    @Test public void intArrToIntArr() throws Exception {bean.intArrToIntArr();}
    @Test public void stringArrToStringArr() throws Exception {bean.stringArrToStringArr();}
    @Test public void stringArrToObjectArr() throws Exception {bean.stringArrToObjectArr();}
    @Test public void dateArrToObjectArr() throws Exception {bean.dateArrToObjectArr();}
    @Test public void stubArrToObjectArr() throws Exception {bean.stubArrToObjectArr();}
    @Test public void testClassArrToObjectArr() throws Exception {bean.testClassArrToObjectArr();}
    @Test public void userFeatureArrToObjectArr() throws Exception {bean.userFeatureArrToObjectArr();}
    @Test public void stringArrToSerializableArr() throws Exception {bean.stringArrToSerializableArr();}
    @Test public void dateArrToSerializableArr() throws Exception {bean.dateArrToSerializableArr();}
    @Test public void stubArrToSerializableArr() throws Exception {bean.stubArrToSerializableArr();}
    @Test public void testClassArrToSerializableArr() throws Exception {bean.testClassArrToSerializableArr();}
    @Test public void userFeatureArrToSerializableArr() throws Exception {bean.userFeatureArrToSerializableArr();}
    @Test public void stubArrToEjbIfaceArr() throws Exception {bean.stubArrToEjbIfaceArr();}
    @Test public void stubArrToRemoteArr() throws Exception {bean.stubArrToRemoteArr();}
    @Test public void testClassArrToTestClassArr() throws Exception {bean.testClassArrToTestClassArr();}
    @Test public void enumToObject() throws Exception {bean.enumToObject();}
    @Test public void enumToSerializable() throws Exception {bean.enumToSerializable();}
    @Test public void timeUnitToObject() throws Exception {bean.enumToObject();}
    @Test public void timeUnitToSerializable() throws Exception {bean.enumToSerializable();}
    @Test public void cmsfv2ChildDataToObject() throws Exception {bean.cmsfv2ChildDataToObject();}
    @Test public void cmsfv2ChildDataToSerializable() throws Exception {bean.cmsfv2ChildDataToSerializable();}
    @Test public void testIDLEntityToObject() throws Exception {bean.testIDLEntityToObject();}
    @Test public void testIDLEntityToSerializable() throws Exception {bean.testIDLEntityToSerializable();}
    @Test public void testIDLEntityToIDLEntity() throws Exception {bean.testIDLEntityToIDLEntity();}
    @Test public void testIDLEntityArrToIDLEntityArr() throws Exception {bean.testIDLEntityArrToIDLEntityArr();}
    @Test public void testTwoLongsToTwoLongs() throws Exception {bean.testTwoLongsToTwoLongs();}
}
