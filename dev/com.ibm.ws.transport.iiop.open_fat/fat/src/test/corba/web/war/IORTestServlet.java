/*******************************************************************************
 * Copyright (c) 2020-2023 IBM Corporation and others.
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

import java.io.Serializable;
import java.rmi.Remote;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.rmi.CORBA.Stub;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.Business;
import shared.Cmsfv2ChildData;
import shared.TestClass;
import shared.TestEnum;
import shared.TestRemote;

@WebServlet("/IORTestServlet")
@SuppressWarnings("serial")
public class IORTestServlet extends FATServlet {
    @Resource
    private ORB orb;

    @Test
    public void testServletExists() {
        System.out.println("### It's alive! ###");
    }

    @Test
    public void testOrbInjected() {
        System.out.println( "### Examining orb reference: " + orb + " ###");
        Objects.requireNonNull(orb);
    }

    @Test
    public void testEjbLookup() throws Exception {
        Objects.requireNonNull(Beans.lookupTestBean(orb));
    }

    @Test
    public void testBusinessEjbLookup() throws Exception {
        Objects.requireNonNull(Beans.lookupBusinessBean(orb));
    }

    @Test
    public void testEjbIorHasExactlyOneProfile() throws Exception {
        TestRemote ejb = Beans.lookupTestBean(orb);
        final int numProfiles = Beans.getNumProfiles((Stub) ejb, orb);
        System.out.printf("### IOR retrieved, with %d profile(s).%n", numProfiles);
        Assert.assertEquals("There should be only one profile in the IOR", 1, numProfiles);
    }

    //=====
    @Test
    public void intToInt() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int i = 111;
        Assert.assertEquals(i, businessEjb.takesInt(i));
    }

    @Test
    public void intToInteger() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Integer i = Integer.valueOf(222);
        Assert.assertEquals(i, businessEjb.takesInteger(222));
    }

    @Test
    public void integerToInteger() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Integer i = Integer.valueOf(333);
        Assert.assertEquals(i, businessEjb.takesInteger(i));
    }

    @Test
    public void stringToString() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String s = "This is the string";
        Assert.assertEquals(s, businessEjb.takesString(s));
    }

    @Test
    public void intToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int i = 444;
        Object o = i;
        Assert.assertEquals(o, businessEjb.takesObject(i));
    }

    @Test
    public void stringToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String s = "String as object";
        Object o = s;
        Assert.assertEquals(o, businessEjb.takesObject(s));
    }

    @Test
    public void dateToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date d = new Date(0);
        Object o = d;
        Assert.assertEquals(o, businessEjb.takesObject(d));
    }

    @Test
    public void stubToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        Assert.assertEquals(testEjb, businessEjb.takesObject(testEjb));
    }

    @Test
    public void testClassToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass t = new TestClass("Test class as object");
        Object o = t;
        Assert.assertEquals(o, businessEjb.takesObject(t));
    }

    /*
    public void userFeatureToObject() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService uf = new UserFeatureService("User feature object as object");
        Object o = uf;
        Assert.assertEquals(o, businessEjb.takesObject(uf));
    }
    */

    @Test
    public void intArrToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int[] arr = { 10, 20, 30, 40, 50 };
        Object o = arr;
        Assert.assertArrayEquals((int[]) o, (int[]) businessEjb.takesObject(arr));
    }

    @Test
    public void stringArrToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    @Test
    public void dateArrToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    @Test
    public void stubArrToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Object o = arr;
        Object[] b = (TestRemote[]) businessEjb.takesObject(arr);
        System.out.println(Arrays.toString(b));
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    @Test
    public void testClassArrToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    /*
    public void userFeatureArrToObject() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }
    */

    @Test
    public void intToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int i = 321;
        Serializable se = i;
        Assert.assertEquals(se, businessEjb.takesSerializable(i));
    }

    @Test
    public void stringToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String s = "String to serializable";
        Serializable se = s;
        Assert.assertEquals(se, businessEjb.takesSerializable(s));
    }

    @Test
    public void dateToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date d = new Date(0);
        Serializable se = d;
        Assert.assertEquals(se, businessEjb.takesSerializable(d));
    }

    @Test
    public void stubToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        Serializable se = (Serializable) testEjb;
        Assert.assertEquals(se, businessEjb.takesSerializable((Serializable) testEjb));
    }

    @Test
    public void testClassToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass t = new TestClass("Test class as serializable");
        Serializable se = t;
        Assert.assertEquals(se, businessEjb.takesSerializable(t));
    }

    /*
    public void userFeatureToSerializable() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService uf = new UserFeatureService("User feature object as serializable");
        Serializable se = uf;
        Assert.assertEquals(se, businessEjb.takesSerializable(uf));
    }
    */

    @Test
    public void intArrToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int[] arr = { 10, 20, 30, 40, 50 };
        Serializable se = arr;
        Assert.assertArrayEquals((int[]) se, (int[]) businessEjb.takesSerializable(arr));
    }

    @Test
    public void stringArrToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    @Test
    public void dateArrToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    @Test
    public void stubArrToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    @Test
    public void testClassArrToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    /*
    public void userFeatureArrToSerializable() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }
    */

    @Test
    public void stubToEjbIface() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        TestRemote iface = testEjb;
        Assert.assertEquals(iface, businessEjb.takesEjbIface(testEjb));
    }

    @Test
    public void stubToRemote() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        Remote r = testEjb;
        Assert.assertEquals(r, businessEjb.takesRemote(testEjb));
    }

    public void testClassToTestClass() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("Test class as test class");
        Assert.assertEquals(a, businessEjb.takesTestClass(a));
    }

    @Test
    public void intArrToIntArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        int[] arr = { 10, 20, 30, 40, 50 };
        Assert.assertArrayEquals(arr, businessEjb.takesIntArray(arr));
    }

    @Test
    public void stringArrToStringArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesStringArray(arr));
    }

    @Test
    public void stringArrToObjectArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    @Test
    public void dateArrToObjectArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    @Test
    public void stubArrToObjectArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    @Test
    public void testClassArrToObjectArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    /*
    public void userFeatureArrToObjectArr() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }
    */

    @Test
    public void stringArrToSerializableArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    @Test
    public void dateArrToSerializableArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    @Test
    public void stubArrToSerializableArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        Serializable[] arr = { (Serializable) testEjb, (Serializable) testEjb, (Serializable) testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    @Test
    public void testClassArrToSerializableArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    /*
    public void userFeatureArrToSerializableArr() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }
    */

    @Test
    public void stubArrToEjbIfaceArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesEjbIfaceArray(arr));
    }

    @Test
    public void stubArrToRemoteArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestRemote testEjb = Beans.lookupTestBean(orb);
        Remote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesRemoteArray(arr));
    }

    @Test
    public void testClassArrToTestClassArr() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesTestClassArray(arr));
    }

    @Test
    public void enumToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestEnum te = TestEnum.TE2;
        Assert.assertSame(te, businessEjb.takesObject(te));
    }

    @Test
    public void enumToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        TestEnum te = TestEnum.TE3;
        Assert.assertSame(te, businessEjb.takesSerializable(te));
    }

    @Test
    public void timeUnitToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, businessEjb.takesObject(te));
    }

    @Test
    public void timeUnitToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, businessEjb.takesSerializable(te));
    }

    @Test
    public void cmsfv2ChildDataToObject() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, businessEjb.takesObject(data));
    }

    @Test
    public void cmsfv2ChildDataToSerializable() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, businessEjb.takesSerializable(data));
    }

    /* IDL testing
    public void testIDLEntityToObject() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        System.out.println("### in testIDLIntfToObject");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) businessEjb.takesObject(testIDLIntf);
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityToSerializable() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        System.out.println("### in testIDLIntfToSerializable");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) businessEjb.takesSerializable(testIDLIntf);
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityToIDLEntity() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        TestIDLIntf returned = businessEjb.takesIDLEntity(testIDLIntf);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityArrToIDLEntityArr() throws Exception {
        Business businessEjb = IiopLogic.lookupBusiness(orb);
        TestIDLIntf[] out = { null, testIDLIntf };
        TestIDLIntf[] in = businessEjb.takesIDLEntityArray(out);
        Assert.assertNull(in[0]);
        Assert.assertEquals(out[1].s(), in[1].s());
        Assert.assertEquals(out.length, in.length);
    }
    */

    @Test
    public void testTwoLongsToTwoLongs() throws Exception {
        Business businessEjb = Beans.lookupBusinessBean(orb);
        Long in = 0xDEADBEEFCAFEBABEL;
        Long[] out = businessEjb.takesTwoLongs(in, in);
        Assert.assertNotNull("Method should return non-null array", out);
        Assert.assertEquals("Array should contain exactly two elements", 2, out.length);
        Assert.assertEquals("First element of array should be equal in value to input param", in, out[0]);
        Assert.assertEquals("Second element of array should be equal in value to input param", in, out[1]);
        Assert.assertSame("Both array elements should be the same instance", out[0], out[1]);
    }
}

