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

import java.io.Serializable;
import java.rmi.Remote;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.rmi.PortableRemoteObject;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import componenttest.topology.utils.FATServletClient;
import test.user.feature.UserFeatureService;

/**
 * Multiple test classes use the same logic, so define that logic here.
 * Provide default test implementations and a @PostConstruct method to perform lookups.
 * Implementing {@link FATServlet}s will inherit the declared tests so they will 
 * if configured in a {@link FATServletClient} test case class.
 */
public interface IIOPClientTestLogic extends IIOPClientTests {
    ORB getOrb();

    default Business lookupBusinessBean() throws Exception {return ClientUtil.lookupBusinessBean(getOrb());}
    default TestRemote lookupTestBean() throws Exception {return ClientUtil.lookupTestBean(getOrb());}
    default TestIDLIntf getIDLObjectRef(ORB orb) throws Exception {return ClientUtil.getIDLObjectRef(getOrb());}

    @Override @Test default void intToInt() throws Exception {
        int i = 111;
        Assert.assertEquals(i, lookupBusinessBean().takesInt(i));
    }

    @Override @Test default void intToInteger() throws Exception {
        Integer i = 222;
        Assert.assertEquals(i, lookupBusinessBean().takesInteger(222));
    }

    @Override @Test default void integerToInteger() throws Exception {
        Integer i = 333;
        Assert.assertEquals(i, lookupBusinessBean().takesInteger(i));
    }

    @Override @Test default void stringToString() throws Exception {
        String s = "This is the string";
        Assert.assertEquals(s, lookupBusinessBean().takesString(s));
    }

    @Override @Test default void intToObject() throws Exception {
        int i = 444;
        Object o = i;
        Assert.assertEquals(o, lookupBusinessBean().takesObject(i));
    }

    @Override @Test default void stringToObject() throws Exception {
        String s = "String as object";
        Object o = s;
        Assert.assertEquals(o, lookupBusinessBean().takesObject(s));
    }

    @Override @Test default void dateToObject() throws Exception {
        Date d = new Date(0);
        Object o = d;
        Assert.assertEquals(o, lookupBusinessBean().takesObject(d));
    }

    @Override @Test default void stubToObject() throws Exception {
        Assert.assertEquals(lookupTestBean(), lookupBusinessBean().takesObject(lookupTestBean()));
    }

    @Override @Test default void testClassToObject() throws Exception {
        TestClass t = new TestClass("Test class as object");
        Object o = t;
        Assert.assertEquals(o, lookupBusinessBean().takesObject(t));
    }

    @Override @Test default void userFeatureToObject() throws Exception {
        UserFeatureService uf = new UserFeatureService("User feature object as object");
        Object o = uf;
        Assert.assertEquals(o, lookupBusinessBean().takesObject(uf));
    }

    @Override @Test default void intArrToObject() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Object o = arr;
        Assert.assertArrayEquals((int[]) o, (int[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void stringArrToObject() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void dateArrToObject() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void stubArrToObject() throws Exception {
        TestRemote[] arr = { lookupTestBean(), lookupTestBean(), lookupTestBean() };
        Object o = arr;
        Object[] b = (TestRemote[]) lookupBusinessBean().takesObject(arr);
        System.out.println(Arrays.toString(b));
        Assert.assertArrayEquals((Object[]) o, (Object[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void testClassArrToObject() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void userFeatureArrToObject() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) lookupBusinessBean().takesObject(arr));
    }

    @Override @Test default void intToSerializable() throws Exception {
        int i = 321;
        Serializable se = i;
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable(i));
    }

    @Override @Test default void stringToSerializable() throws Exception {
        String s = "String to serializable";
        Serializable se = s;
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable(s));
    }

    @Override @Test default void dateToSerializable() throws Exception {
        Date d = new Date(0);
        Serializable se = d;
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable(d));
    }

    @Override @Test default void stubToSerializable() throws Exception {
        Serializable se = (Serializable) lookupTestBean();
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable((Serializable) lookupTestBean()));
    }

    @Override @Test default void testClassToSerializable() throws Exception {
        TestClass t = new TestClass("Test class as serializable");
        Serializable se = t;
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable(t));
    }

    @Override @Test default void userFeatureToSerializable() throws Exception {
        UserFeatureService uf = new UserFeatureService("User feature object as serializable");
        Serializable se = uf;
        Assert.assertEquals(se, lookupBusinessBean().takesSerializable(uf));
    }

    @Override @Test default void intArrToSerializable() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Serializable se = arr;
        Assert.assertArrayEquals((int[]) se, (int[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void stringArrToSerializable() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void dateArrToSerializable() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void stubArrToSerializable() throws Exception {
        TestRemote[] arr = { lookupTestBean(), lookupTestBean(), lookupTestBean() };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void testClassArrToSerializable() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void userFeatureArrToSerializable() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) lookupBusinessBean().takesSerializable(arr));
    }

    @Override @Test default void stubToEjbIface() throws Exception {
        TestRemote iface = lookupTestBean();
        Assert.assertEquals(iface, lookupBusinessBean().takesEjbIface(lookupTestBean()));
    }

    @Override @Test default void stubToRemote() throws Exception {
        Remote r = lookupTestBean();
        Assert.assertEquals(r, lookupBusinessBean().takesRemote(lookupTestBean()));
    }

    @Override @Test default void testClassToTestClass() throws Exception {
        TestClass a = new TestClass("Test class as test class");
        Assert.assertEquals(a, lookupBusinessBean().takesTestClass(a));
    }

    @Override @Test default void intArrToIntArr() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesIntArray(arr));
    }

    @Override @Test default void stringArrToStringArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesStringArray(arr));
    }

    @Override @Test default void stringArrToObjectArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesObjectArray(arr));
    }

    @Override @Test default void dateArrToObjectArr() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesObjectArray(arr));
    }

    @Override @Test default void stubArrToObjectArr() throws Exception {
        TestRemote[] arr = { lookupTestBean(), lookupTestBean(), lookupTestBean() };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesObjectArray(arr));
    }

    @Override @Test default void testClassArrToObjectArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesObjectArray(arr));
    }

    @Override @Test default void userFeatureArrToObjectArr() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesObjectArray(arr));
    }

    @Override @Test default void stringArrToSerializableArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesSerializableArray(arr));
    }

    @Override @Test default void dateArrToSerializableArr() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesSerializableArray(arr));
    }

    @Override @Test default void stubArrToSerializableArr() throws Exception {
        Serializable[] arr = { (Serializable) lookupTestBean(), (Serializable) lookupTestBean(), (Serializable) lookupTestBean() };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesSerializableArray(arr));
    }

    @Override @Test default void testClassArrToSerializableArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesSerializableArray(arr));
    }

    @Override @Test default void userFeatureArrToSerializableArr() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesSerializableArray(arr));
    }

    @Override @Test default void stubArrToEjbIfaceArr() throws Exception {
        TestRemote[] arr = { lookupTestBean(), lookupTestBean(), lookupTestBean() };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesEjbIfaceArray(arr));
    }

    @Override @Test default void stubArrToRemoteArr() throws Exception {
        Remote[] arr = { lookupTestBean(), lookupTestBean(), lookupTestBean() };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesRemoteArray(arr));
    }

    @Override @Test default void testClassArrToTestClassArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, lookupBusinessBean().takesTestClassArray(arr));
    }

    @Override @Test default void enumToObject() throws Exception {
        TestEnum te = TestEnum.TE2;
        Assert.assertSame(te, lookupBusinessBean().takesObject(te));
    }

    @Override @Test default void enumToSerializable() throws Exception {
        TestEnum te = TestEnum.TE3;
        Assert.assertSame(te, lookupBusinessBean().takesSerializable(te));
    }

    @Override @Test default void timeUnitToObject() throws Exception {
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, lookupBusinessBean().takesObject(te));
    }

    @Override @Test default void timeUnitToSerializable() throws Exception {
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, lookupBusinessBean().takesSerializable(te));
    }

    @Override @Test default void cmsfv2ChildDataToObject() throws Exception {
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, lookupBusinessBean().takesObject(data));
    }

    @Override @Test default void cmsfv2ChildDataToSerializable() throws Exception {
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, lookupBusinessBean().takesSerializable(data));
    }

    @Override @Test default void testIDLEntityToObject() throws Exception {
        System.out.println("### in testIDLIntfToObject");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) lookupBusinessBean().takesObject(getIDLObjectRef(getOrb()));
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(getIDLObjectRef(getOrb()).s(), returned.s());
    }

    @Override @Test default void testIDLEntityToSerializable() throws Exception {
        System.out.println("### in testIDLIntfToSerializable");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) lookupBusinessBean().takesSerializable(getIDLObjectRef(getOrb()));
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(getIDLObjectRef(getOrb()).s(), returned.s());
    }

    @Override @Test default void testIDLEntityToIDLEntity() throws Exception {
        TestIDLIntf returned = lookupBusinessBean().takesIDLEntity(getIDLObjectRef(getOrb()));
        Assert.assertEquals(getIDLObjectRef(getOrb()).s(), returned.s());
    }

    @Override @Test default void testIDLEntityArrToIDLEntityArr() throws Exception {
        TestIDLIntf[] out = { null, getIDLObjectRef(getOrb()) };
        TestIDLIntf[] in = lookupBusinessBean().takesIDLEntityArray(out);
        Assert.assertNull(in[0]);
        Assert.assertEquals(out[1].s(), in[1].s());
        Assert.assertEquals(out.length, in.length);
    }

    @Override @Test default void testTwoLongsToTwoLongs() throws Exception {
        Long in = 0xDEADBEEFCAFEBABEL;
        Long[] out = lookupBusinessBean().takesTwoLongs(in, in);
        Assert.assertNotNull("Method should return non-null array", out);
        Assert.assertEquals("Array should contain exactly two elements", 2, out.length);
        Assert.assertEquals("First element of array should be equal in value to input param", in, out[0]);
        Assert.assertEquals("Second element of array should be equal in value to input param", in, out[1]);
        Assert.assertSame("Both array elements should be the same instance", out[0], out[1]);
    }
}
