/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.corba.web.war;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;

import componenttest.app.FATServlet;
import shared.Business;
import shared.Cmsfv2ChildData;
import shared.TestClass;
import shared.TestEnum;
import shared.TestIDLIntf;
import shared.TestIDLIntf_impl;
import shared.TestRemote;
import test.user.feature.UserFeatureService;

@WebServlet("/MyIIOPClientServlet")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MyIIOPClientServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    private Business businessEjb;

    private TestRemote testEjb;

    private TestIDLIntf testIDLIntf;

    private static final String NS_URL = "corbaloc::localhost:" + System.getProperty("bvt.prop.IIOP") + "/NameService";

    public ORB getORB() throws Exception {
        return (ORB.init(new String[] { "-ORBInitRef", "NameService=" + NS_URL }, null));
    }

    @PostConstruct
    public void lookupEJBs() {
        final org.omg.CORBA.Object nameServiceRef;
        final POA rootPoa;
        final NamingContextExt rootContext;
        try {
            nameServiceRef = getORB().resolve_initial_references("NameService");
            rootPoa = (POA) getORB().resolve_initial_references("RootPOA");
            rootContext = NamingContextExtHelper.narrow(nameServiceRef);
        } catch (Exception e) {
            throw (RuntimeException) (new RuntimeException("!!!").initCause(e));
        }
        try {
		NameComponent[] ncArray1 = rootContext.to_name("ejb/global/test\\.corba/test\\.corba\\.bean/BusinessBean!shared\\.Business");
            Object obj1 = rootContext.resolve(ncArray1);
            businessEjb = (Business) PortableRemoteObject.narrow(obj1, Business.class);
            NameComponent[] ncArray2 = rootContext.to_name("ejb/global/test\\.corba/test\\.corba\\.bean/TestBean!shared\\.TestRemote");
            Object obj2 = rootContext.resolve(ncArray2);
            testEjb = (TestRemote) PortableRemoteObject.narrow(obj2, TestRemote.class);

            // Bind a sample CORBA object
            TestIDLIntf_impl testIDLIntf_impl = new TestIDLIntf_impl();
            testIDLIntf_impl.s("wibble");
            byte[] id = rootPoa.activate_object(testIDLIntf_impl);
            rootPoa.the_POAManager().activate();
            org.omg.CORBA.Object testIDLIntfRef = rootPoa.create_reference_with_id(id, testIDLIntf_impl._all_interfaces(rootPoa, id)[0]);
            testIDLIntf = (TestIDLIntf) PortableRemoteObject.narrow(testIDLIntfRef, TestIDLIntf.class);
        } catch (NotFound e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void intToInt() throws Exception {
        int i = 111;
        Assert.assertEquals(i, businessEjb.takesInt(i));
    }

    public void intToInteger() throws Exception {
        Integer i = new Integer(222);
        Assert.assertEquals(i, businessEjb.takesInteger(222));
    }

    public void integerToInteger() throws Exception {
        Integer i = new Integer(333);
        Assert.assertEquals(i, businessEjb.takesInteger(i));
    }

    public void stringToString() throws Exception {
        String s = "This is the string";
        Assert.assertEquals(s, businessEjb.takesString(s));
    }

    public void intToObject() throws Exception {
        int i = 444;
        Object o = i;
        Assert.assertEquals(o, businessEjb.takesObject(i));
    }

    public void stringToObject() throws Exception {
        String s = "String as object";
        Object o = s;
        Assert.assertEquals(o, businessEjb.takesObject(s));
    }

    public void dateToObject() throws Exception {
        Date d = new Date(0);
        Object o = d;
        Assert.assertEquals(o, businessEjb.takesObject(d));
    }

    public void stubToObject() throws Exception {
        Assert.assertEquals(testEjb, businessEjb.takesObject(testEjb));
    }

    public void testClassToObject() throws Exception {
        TestClass t = new TestClass("Test class as object");
        Object o = t;
        Assert.assertEquals(o, businessEjb.takesObject(t));
    }

    public void userFeatureToObject() throws Exception {
        UserFeatureService uf = new UserFeatureService("User feature object as object");
        Object o = uf;
        Assert.assertEquals(o, businessEjb.takesObject(uf));
    }

    public void intArrToObject() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Object o = arr;
        Assert.assertArrayEquals((int[]) o, (int[]) businessEjb.takesObject(arr));
    }

    public void stringArrToObject() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    public void dateArrToObject() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    public void stubArrToObject() throws Exception {
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Object o = arr;
        Object[] b = (TestRemote[]) businessEjb.takesObject(arr);
        System.out.println(Arrays.toString(b));
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    public void testClassArrToObject() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    public void userFeatureArrToObject() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Object o = arr;
        Assert.assertArrayEquals((Object[]) o, (Object[]) businessEjb.takesObject(arr));
    }

    public void intToSerializable() throws Exception {
        int i = 321;
        Serializable se = i;
        Assert.assertEquals(se, businessEjb.takesSerializable(i));
    }

    public void stringToSerializable() throws Exception {
        String s = "String to serializable";
        Serializable se = s;
        Assert.assertEquals(se, businessEjb.takesSerializable(s));
    }

    public void dateToSerializable() throws Exception {
        Date d = new Date(0);
        Serializable se = d;
        Assert.assertEquals(se, businessEjb.takesSerializable(d));
    }

    public void stubToSerializable() throws Exception {
        Serializable se = (Serializable) testEjb;
        Assert.assertEquals(se, businessEjb.takesSerializable((Serializable) testEjb));
    }

    public void testClassToSerializable() throws Exception {
        TestClass t = new TestClass("Test class as serializable");
        Serializable se = t;
        Assert.assertEquals(se, businessEjb.takesSerializable(t));
    }

    public void userFeatureToSerializable() throws Exception {
        UserFeatureService uf = new UserFeatureService("User feature object as serializable");
        Serializable se = uf;
        Assert.assertEquals(se, businessEjb.takesSerializable(uf));
    }

    public void intArrToSerializable() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Serializable se = arr;
        Assert.assertArrayEquals((int[]) se, (int[]) businessEjb.takesSerializable(arr));
    }

    public void stringArrToSerializable() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    public void dateArrToSerializable() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    public void stubArrToSerializable() throws Exception {
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    public void testClassArrToSerializable() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    public void userFeatureArrToSerializable() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Serializable se = arr;
        Assert.assertArrayEquals((Object[]) se, (Object[]) businessEjb.takesSerializable(arr));
    }

    public void stubToEjbIface() throws Exception {
        TestRemote iface = testEjb;
        Assert.assertEquals(iface, businessEjb.takesEjbIface(testEjb));
    }

    public void stubToRemote() throws Exception {
        Remote r = testEjb;
        Assert.assertEquals(r, businessEjb.takesRemote(testEjb));
    }

    public void testClassToTestClass() throws Exception {
        TestClass a = new TestClass("Test class as test class");
        Assert.assertEquals(a, businessEjb.takesTestClass(a));
    }

    public void intArrToIntArr() throws Exception {
        int[] arr = { 10, 20, 30, 40, 50 };
        Assert.assertArrayEquals(arr, businessEjb.takesIntArray(arr));
    }

    public void stringArrToStringArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesStringArray(arr));
    }

    public void stringArrToObjectArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    public void dateArrToObjectArr() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    public void stubArrToObjectArr() throws Exception {
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    public void testClassArrToObjectArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    public void userFeatureArrToObjectArr() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesObjectArray(arr));
    }

    public void stringArrToSerializableArr() throws Exception {
        String[] arr = { "abc", "def", "ghi", "jkl", "mno" };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    public void dateArrToSerializableArr() throws Exception {
        Date[] arr = { new Date(0), new Date(0), new Date(0) };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    public void stubArrToSerializableArr() throws Exception {
        Serializable[] arr = { (Serializable) testEjb, (Serializable) testEjb, (Serializable) testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    public void testClassArrToSerializableArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    public void userFeatureArrToSerializableArr() throws Exception {
        UserFeatureService a = new UserFeatureService("user feature 1");
        UserFeatureService b = new UserFeatureService("user feature 2");
        UserFeatureService c = new UserFeatureService("user feature 3");
        UserFeatureService[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesSerializableArray(arr));
    }

    public void stubArrToEjbIfaceArr() throws Exception {
        TestRemote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesEjbIfaceArray(arr));
    }

    public void stubArrToRemoteArr() throws Exception {
        Remote[] arr = { testEjb, testEjb, testEjb };
        Assert.assertArrayEquals(arr, businessEjb.takesRemoteArray(arr));
    }

    public void testClassArrToTestClassArr() throws Exception {
        TestClass a = new TestClass("test class 1");
        TestClass b = new TestClass("test class 2");
        TestClass c = new TestClass("test class 3");
        TestClass[] arr = { a, b, c };
        Assert.assertArrayEquals(arr, businessEjb.takesTestClassArray(arr));
    }

    public void enumToObject() throws Exception {
        TestEnum te = TestEnum.TE2;
        Assert.assertSame(te, businessEjb.takesObject(te));
    }

    public void enumToSerializable() throws Exception {
        TestEnum te = TestEnum.TE3;
        Assert.assertSame(te, businessEjb.takesSerializable(te));
    }

    public void timeUnitToObject() throws Exception {
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, businessEjb.takesObject(te));
    }

    public void timeUnitToSerializable() throws Exception {
        Enum<?> te = TimeUnit.NANOSECONDS;
        Assert.assertSame(te, businessEjb.takesSerializable(te));
    }

    public void cmsfv2ChildDataToObject() throws Exception {
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, businessEjb.takesObject(data));
    }

    public void cmsfv2ChildDataToSerializable() throws Exception {
        Cmsfv2ChildData data = new Cmsfv2ChildData();
        Assert.assertEquals(data, businessEjb.takesSerializable(data));
    }

    public void testIDLEntityToObject() throws Exception {
        System.out.println("### in testIDLIntfToObject");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) businessEjb.takesObject(testIDLIntf);
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityToSerializable() throws Exception {
        System.out.println("### in testIDLIntfToSerializable");
        org.omg.CORBA.Object o = (org.omg.CORBA.Object) businessEjb.takesSerializable(testIDLIntf);
        TestIDLIntf returned = (TestIDLIntf) PortableRemoteObject.narrow(o, TestIDLIntf.class);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityToIDLEntity() throws Exception {
        TestIDLIntf returned = businessEjb.takesIDLEntity(testIDLIntf);
        Assert.assertEquals(testIDLIntf.s(), returned.s());
    }

    public void testIDLEntityArrToIDLEntityArr() throws Exception {
        TestIDLIntf[] out = { null, testIDLIntf };
        TestIDLIntf[] in = businessEjb.takesIDLEntityArray(out);
        Assert.assertNull(in[0]);
        Assert.assertEquals(out[1].s(), in[1].s());
        Assert.assertEquals(out.length, in.length);
    }

    public void testTwoLongsToTwoLongs() throws Exception {
        Long in = 0xDEADBEEFCAFEBABEL;
        Long[] out = businessEjb.takesTwoLongs(in, in);
        Assert.assertNotNull("Method should return non-null array", out);
        Assert.assertEquals("Array should contain exactly two elements", 2, out.length);
        Assert.assertEquals("First element of array should be equal in value to input param", in, out[0]);
        Assert.assertEquals("Second element of array should be equal in value to input param", in, out[1]);
        Assert.assertSame("Both array elements should be the same instance", out[0], out[1]);
    }
}
