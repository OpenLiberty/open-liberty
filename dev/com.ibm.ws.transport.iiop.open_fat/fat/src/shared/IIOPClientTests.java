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
package shared;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Define some tests to be run via any {@link FATServlet} that implement this interface.
 * Servlets will not compile if they do not provide implementations of these methods, 
 * but it is the implementer's responsibility to add annotations to make sure 
 * these methods are run as tests. 
 */
public interface IIOPClientTests {
    void intToInt() throws Exception;
    void intToInteger() throws Exception;
    void integerToInteger() throws Exception;
    void stringToString() throws Exception;
    void intToObject() throws Exception;
    void stringToObject() throws Exception;
    void dateToObject() throws Exception;
    void stubToObject() throws Exception;
    void testClassToObject() throws Exception;
    void userFeatureToObject() throws Exception;
    void intArrToObject() throws Exception;
    void stringArrToObject() throws Exception;
    void dateArrToObject() throws Exception;
    void stubArrToObject() throws Exception;
    void testClassArrToObject() throws Exception;
    void userFeatureArrToObject() throws Exception;
    void intToSerializable() throws Exception;
    void stringToSerializable() throws Exception;
    void dateToSerializable() throws Exception;
    void stubToSerializable() throws Exception;
    void testClassToSerializable() throws Exception;
    void userFeatureToSerializable() throws Exception;
    void intArrToSerializable() throws Exception;
    void stringArrToSerializable() throws Exception;
    void dateArrToSerializable() throws Exception;
    void stubArrToSerializable() throws Exception;
    void testClassArrToSerializable() throws Exception;
    void userFeatureArrToSerializable() throws Exception;
    void stubToEjbIface() throws Exception;
    void stubToRemote() throws Exception;
    void testClassToTestClass() throws Exception;
    void intArrToIntArr() throws Exception;
    void stringArrToStringArr() throws Exception;
    void stringArrToObjectArr() throws Exception;
    void dateArrToObjectArr() throws Exception;
    void stubArrToObjectArr() throws Exception;
    void testClassArrToObjectArr() throws Exception;
    void userFeatureArrToObjectArr() throws Exception;
    void stringArrToSerializableArr() throws Exception;
    void dateArrToSerializableArr() throws Exception;
    void stubArrToSerializableArr() throws Exception;
    void testClassArrToSerializableArr() throws Exception;
    void userFeatureArrToSerializableArr() throws Exception;
    void stubArrToEjbIfaceArr() throws Exception;
    void stubArrToRemoteArr() throws Exception;
    void testClassArrToTestClassArr() throws Exception;
    void enumToObject() throws Exception;
    void enumToSerializable() throws Exception;
    void timeUnitToObject() throws Exception;
    void timeUnitToSerializable() throws Exception;
    void cmsfv2ChildDataToObject() throws Exception;
    void cmsfv2ChildDataToSerializable() throws Exception;
    void testIDLEntityToObject() throws Exception;
    void testIDLEntityToSerializable() throws Exception;
    void testIDLEntityToIDLEntity() throws Exception;
    void testIDLEntityArrToIDLEntityArr() throws Exception;
    void testTwoLongsToTwoLongs() throws Exception;
}