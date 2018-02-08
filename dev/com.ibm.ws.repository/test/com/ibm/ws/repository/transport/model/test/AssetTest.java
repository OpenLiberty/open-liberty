/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.transport.client.JSONAssetConverter;
import com.ibm.ws.repository.transport.model.AbstractJSON;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Asset.Featured;
import com.ibm.ws.repository.transport.model.Asset.Privacy;
import com.ibm.ws.repository.transport.model.AssetInformation;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentInfo;
import com.ibm.ws.repository.transport.model.Feedback;
import com.ibm.ws.repository.transport.model.FilterVersion;
import com.ibm.ws.repository.transport.model.ImageDetails;
import com.ibm.ws.repository.transport.model.Link;
import com.ibm.ws.repository.transport.model.Provider;
import com.ibm.ws.repository.transport.model.RequireFeatureWithTolerates;
import com.ibm.ws.repository.transport.model.Reviewed;
import com.ibm.ws.repository.transport.model.SalesContact;
import com.ibm.ws.repository.transport.model.StateUpdateAction;
import com.ibm.ws.repository.transport.model.User;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * Tests for the {@link Asset} class
 */
public class AssetTest {

    @Test
    public void testAssetInformationEquals() throws Throwable {
        checkEqualsOnClass(new AssetInformation(), new AssetInformation());
    }

    @Test
    public void testAttachmentEquals() throws Throwable {
        checkEqualsOnClass(new Attachment(), new Attachment());
    }

    @Test
    public void testFeedbackEquals() throws Throwable {
        checkEqualsOnClass(new Feedback(), new Feedback());
    }

    @Test
    public void testReviewedEquals() throws Throwable {
        checkEqualsOnClass(new Reviewed(), new Reviewed());
    }

    @Test
    public void testSalesContactEquals() throws Throwable {
        checkEqualsOnClass(new SalesContact(), new SalesContact());
    }

    @Test
    public void testStateActionEquals() throws Throwable {
        checkEqualsOnClass(new StateUpdateAction(), new StateUpdateAction());
    }

    @Test
    public void testUserEquals() throws Throwable {
        checkEqualsOnClass(new User(), new User());
    }

    @Test
    public void testWlpInformationEquals() throws Throwable {
        checkEqualsOnClass(new WlpInformation(), new WlpInformation());
    }

    @Test
    public void testLinkEquals() throws Throwable {
        checkEqualsOnClass(new Link(), new Link());
    }

    @Test
    public void testAssetEquals() throws Throwable {
        checkEqualsOnClass(new Asset(), new Asset());
    }

    @Test
    public void testAppliesToFilterInfoEquals() throws Throwable {
        checkEqualsOnClass(new AppliesToFilterInfo(), new AppliesToFilterInfo());
    }

    @Test
    public void testFilterVersionEquals() throws Throwable {
        checkEqualsOnClass(new FilterVersion(), new FilterVersion());
    }

    @Test
    public void testAttachmentInfoEquals() throws Throwable {
        checkEqualsOnClass(new AttachmentInfo(), new AttachmentInfo());
    }

    @Test
    public void testRequireFeatureWithTolerates() throws Throwable {
        checkEqualsOnClass(new RequireFeatureWithTolerates(), new RequireFeatureWithTolerates());
    }

    /**
     * Test to make sure that when you set a short name the serialized and desialization preserve the lower case version.
     *
     * @throws IOException
     */
    @Test
    public void testLowerCaseShortName() throws Exception {
        Asset testAsset = new Asset();
        WlpInformation testInfo = new WlpInformation();
        testAsset.setWlpInformation(testInfo);
        String shortName = "MixedCaseShortName";
        testInfo.setShortName(shortName);
        assertEquals("The lower case short name should be set when the upper case one is", shortName.toLowerCase(), testAsset.getWlpInformation().getLowerCaseShortName());
        String assetString = JSONAssetConverter.writeValueAsString(testAsset);
        assertTrue("The converted JSON should contain the lower case property", assetString.contains("lowerCaseShortName"));
        assertTrue("The converted JSON should contain the lower case value", assetString.contains(shortName.toLowerCase()));
        Asset reReadAsset = JSONAssetConverter.readValue(new ByteArrayInputStream(assetString.getBytes()));
        assertEquals("Should be able to serialize and deserialize the asset and preserve the lower case short name", shortName.toLowerCase(),
                     reReadAsset.getWlpInformation().getLowerCaseShortName());
    }

    @Test
    public void testCreateJSONFile() throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, SecurityException, InvocationTargetException {
        Asset ass = new Asset();
        ass.set_id("12345");
        ass.setAttachments(new ArrayList<Attachment>());
        ass.setCreatedBy(new User());
        ass.setCreatedOn(Calendar.getInstance());
        ass.setDescription("description");
        ass.setFeatured(Featured.YES);
        ass.setFeedback(new Feedback());
        ass.setInformation(new AssetInformation());
        ass.setInMyStore("yes");
        ass.setLastUpdatedOn(Calendar.getInstance());
        ass.setLicenseId("License ID");
        ass.setLicenseType(LicenseType.ILAN);
        ass.setMarketplaceId("market place id");
        ass.setMarketplaceName("marketplace name");
        ass.setName("wibble");
        ass.setPrivacy(Privacy.PRIVATE);
        ass.setProvider(new Provider());
        ass.setReviewed(new Reviewed());
        ass.setShortDescription("short desc");
        ass.setState(State.DRAFT);
        ass.setType(ResourceType.FEATURE);
        ass.setVersion("1.0.1");
        ass.setWlpInformation(new WlpInformation());
        Asset assJson = ass.createMinimalAssetForJSON();

        JsonChecker chk = new JsonChecker(ass, assJson);
        chk.check("_id", false);
        chk.check("Attachments", false);
        chk.check("CreatedBy", false);
        chk.check("CreatedOn", false);
        chk.check("Description", true);
        chk.check("Featured", false);
        chk.check("Feedback", false);
        chk.check("Information", false);
        chk.check("InMyStore", false);
        chk.check("LastUpdatedOn", false);
        chk.check("LicenseId", true);
        chk.check("LicenseType", true);
        chk.check("MarketplaceId", false);
        chk.check("MarketplaceName", false);
        chk.check("Name", true);
        chk.check("Privacy", false);
        chk.check("Provider", true);
        chk.check("Reviewed", false);
        chk.check("ShortDescription", true);
        chk.check("State", false);
        chk.check("Type", true);
        chk.check("Version", true);
        chk.check("WlpInformation", true);
    }

    private class JsonChecker {
        private final Asset _source;
        private final Asset _target;

        private JsonChecker(Asset source, Asset target) {
            _source = source;
            _target = target;
        }

        private void check(String name,
                           boolean copied) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Method m = Asset.class.getDeclaredMethod("get" + name, (Class[]) null);
            Object found = m.invoke(_target);
            Object expected = copied ? m.invoke(_source) : null;
            assertEquals("The " + name + " field should " + (copied ? " " : " not ") + " be copied",
                         expected, found);
        }

    }

    /**
     * Reflectively finds the fields in the class and sets these fields to values then performs
     * equals checks on the object.
     *
     * @param left First object used in equals
     * @param right Second object used in equals
     * @throws Throwable
     */
    private void checkEqualsOnClass(Object left, Object right) throws Throwable {

        // Keep track of any fields we haven't tested, this is in
        // case new fields are added of types that this code doesn't
        // yet cater for.
        ArrayList<String> notTested = new ArrayList<String>();

        Class<?> cls = left.getClass();
        Field[] fields = cls.getDeclaredFields();
        System.out.println("Found " + fields.length + " fields");

        // Go through each fields and check it. Returns true if the field
        // was checked and false otherwise.
        // Ignore synthetic fields (e.g. those added by JaCoCo code coverage)
        for (Field fld : fields) {
            if (!fld.isSynthetic() && !checkField(fld, left, right)) {
                notTested.add(fld.getName() + ":" + fld.getType());
            }
        }

        // Make sure we have no unchecked fields
        System.out.println("Not tested:" + notTested);
        assertEquals("Some fields were not tested", notTested.size(), 0);
    }

    /**
     * Checks the specified field. It first determines the java type the field is then
     * calls the check method associated with that field
     *
     * @param fld The field to check
     * @param left First object used in equals
     * @param right Second object used in equals
     * @return
     * @throws Throwable
     */
    private boolean checkField(Field fld, Object left, Object right) throws Throwable {
        if (Modifier.isFinal(fld.getModifiers())) {
            return true;
        }
        boolean tested = false;
        String fldName = fld.getName();

        // Get the type
        Class<?> cls = fld.getType();
        System.out.println("Name is " + fldName + " Type is " + cls);

        // Get the setter for this field
        Method[] methods = left.getClass().getDeclaredMethods();
        Method setter = null;
        for (Method m : methods) {
            if (("set" + fldName).equalsIgnoreCase(m.getName())) {
                setter = m;
                break;
            } else {
                if (("setIs" + fldName).equalsIgnoreCase(m.getName())) {
                    setter = m;
                    break;
                }
            }
        }

        // If field starts with an "_" then check for method name with that char removed.
        if (setter == null) {
            if (fldName.startsWith("_")) {
                String checkFld = fldName.substring(1);
                for (Method m : methods) {
                    if (("set" + checkFld).equalsIgnoreCase(m.getName())) {
                        setter = m;
                        break;
                    } else {
                        if (("setIs" + checkFld).equalsIgnoreCase(m.getName())) {
                            setter = m;
                            break;
                        }
                    }
                }
            }
        }

        if (setter == null) {
            System.out.println("NO SETTER FOUND FOR " + fldName);
        }

        // Now find the type the field is and call the associated check method
        try {
            // Make sure we can call the setter to be able to run the equals test
            setter.setAccessible(true);
            if (cls.equals(String.class)) {
                checkString(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.isEnum()) {
                checkEnum(cls, setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.isPrimitive()) {
                checkPrimitive(cls, setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if ((cls.equals(Collection.class)) &&
                       (fld.getGenericType() instanceof ParameterizedType)) {
                // erm....hmm...cry
                System.out.println("	We got a collection ");
                ParameterizedType type = (ParameterizedType) fld.getGenericType();
                java.lang.reflect.Type collectionOf = type.getActualTypeArguments()[0];
                if (collectionOf.equals(String.class)) {
                    checkCollectionOfString(setter, left, right);
                    System.out.println("	" + fldName + " OK");
                    tested = true;
                } else {
                    checkCollectionOfObjects(setter, left, right);
                    System.out.println("	" + fldName + " OK");
                    tested = true;
                }
            } else if ((cls.equals(List.class)) &&
                       (fld.getGenericType() instanceof ParameterizedType)) {
                // erm....hmm...cry
                System.out.println("	We got a list ");
                checkListOfObjects(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.equals(Date.class)) {
                checkDate(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.equals(Calendar.class)) {
                checkCalendar(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (AbstractJSON.class.equals(cls.getGenericSuperclass())) {
                // We could recursively dig into the JSON objects since they are
                // "ours" but not sure we need to go that deep as the plan is
                // to test the equals methods for all of our POJO's in seperate
                // tests
                System.out.println("	We have a JSON object");
                checkJSON(cls, setter, left, right);
                tested = true;
            } else if (cls.equals(Locale.class)) {
                checkLocale(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.equals(FilterVersion.class)) {
                checkFilterVersion(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else if (cls.equals(ImageDetails.class)) {
                checkImageDetails(setter, left, right);
                System.out.println("	" + fldName + " OK");
                tested = true;
            } else {
                // This will mean we return false, and this type will be added to
                // the list of "unknown" types, throwing an exception, which means
                // the above if / else block would need to be extended to cater
                // for the new type
                System.out.println("	WTF is a " + fld.getType());
            }
        } catch (Throwable t) {
            System.out.println("EQUALS FAILED FOR " + fld.getName()
                               + " on class " + left.getClass().getName());
            throw new Exception("Equals failed for field " + fld.getName() +
                                " on class " + left.getClass().getName(), t);
        }
        return tested;
    }

    /*
     * --------------------------------------------------------------------------
     *
     * METHODS TO CHECK EQUALS ON DIFFERENT TYPES
     *
     * --------------------------------------------------------------------------
     */

    private void checkJSON(Class<?> cls, Method setter, Object left,
                           Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, SecurityException, NoSuchMethodException {
        Constructor<?> c = cls.getConstructor();
        Object JSON = c.newInstance();
        setter.invoke(left, JSON);
        setter.invoke(right, (Object) null);
        assertNotEquals(left, right);
        setter.invoke(right, JSON);
        assertEquals(left, right);
    }

    private void checkPrimitive(Class<?> cls, Method setter, Object left,
                                Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String name = cls.getName();
        if ("long".equals(name)) {
            setter.invoke(left, 456);
            assertNotEquals(left, right);
            setter.invoke(right, 12);
            assertNotEquals(left, right);
            setter.invoke(right, 456);
            assertEquals(left, right);
        } else if ("int".equals(name)) {
            setter.invoke(left, 111);
            assertNotEquals(left, right);
            setter.invoke(right, 22);
            assertNotEquals(left, right);
            setter.invoke(right, 111);
            assertEquals(left, right);
        } else if ("boolean".equals(name)) {
            // defaults to false
            setter.invoke(left, true);
            assertNotEquals(left, right);
            setter.invoke(right, true);
            assertEquals(left, right);
        } else {
            throw new IllegalArgumentException("No code to handle primitive type " + name);
        }
    }

    private void checkEnum(Class<?> cls, Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Object[] enumValues = cls.getEnumConstants();
        Object prevValue = null;
        for (Object aValue : enumValues) {
            if (null == prevValue) {
                prevValue = aValue;
                continue;
            } else {
                setter.invoke(left, aValue);
                setter.invoke(right, prevValue);
                assertNotEquals(left, right);
                setter.invoke(right, aValue);
                assertEquals(left, right);
            }
        }
    }

    private void checkFilterVersion(Method setter, Object left, Object right) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        FilterVersion fv1 = new FilterVersion();
        FilterVersion fv2 = new FilterVersion();
        fv1.setLabel("Fred");
        fv2.setLabel("Bill");
        setter.invoke(left, fv1);
        assertNotEquals(left, right);
        setter.invoke(right, fv2);
        assertNotEquals(left, right);
        setter.invoke(right, fv1);
        assertEquals(left, right);
    }

    private void checkLocale(Method setter, Object left, Object right) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        setter.invoke(left, Locale.ENGLISH);
        assertNotEquals(left, right);
        setter.invoke(right, Locale.FRANCE);
        assertNotEquals(left, right);
        setter.invoke(right, Locale.ENGLISH);
        assertEquals(left, right);
    }

    private void checkImageDetails(Method setter, Object left, Object right) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // both should be null, and hence equal
        assertEquals(left, right);
        ImageDetails image1 = new ImageDetails();
        image1.setHeight(5);
        image1.setWidth(3);
        ImageDetails image2 = new ImageDetails();
        image2.setHeight(534);
        image2.setWidth(343);
        // Image 3 has same values as image 1 but is a different object
        ImageDetails image3 = new ImageDetails();
        image3.setHeight(5);
        image3.setWidth(3);
        setter.invoke(left, image1);
        assertNotEquals(left, right);
        setter.invoke(right, image2);
        assertNotEquals(left, right);
        setter.invoke(right, image3);
        assertEquals(left, right);
    }

    private void checkString(Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setter.invoke(right, (Object) null);
        setter.invoke(left, "test");
        assertNotEquals(left, right);
        setter.invoke(right, "notmatchtest");
        assertNotEquals(left, right);
        setter.invoke(right, "test");
        assertEquals(left, right);
    }

    private void checkCollectionOfString(Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HashSet<String> leftSet = new HashSet<String>();
        leftSet.add("Test");
        setter.invoke(left, leftSet);
        assertNotEquals(left, right);
        HashSet<String> rightSet = new HashSet<String>();
        setter.invoke(right, rightSet);
        assertNotEquals(left, right);
        rightSet.add("Test");
        assertEquals(left, right);
    }

    private void checkCollectionOfObjects(Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HashSet<Object> leftSet = new HashSet<Object>();
        leftSet.add("test data");
        setter.invoke(left, leftSet);
        assertNotEquals(left, right);
        HashSet<Object> rightSet = new HashSet<Object>();
        rightSet.add("different data");
        setter.invoke(right, rightSet);
        assertNotEquals(left, right);
        setter.invoke(right, leftSet);
        assertEquals(left, right);
    }

    private void checkListOfObjects(Method setter, Object left,
                                    Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        ArrayList<Object> leftSet = new ArrayList<Object>();
        ArrayList<Object> rightSet = new ArrayList<Object>();
        // Have to special case setAttachments as it does more than
        // just "store the suplied value".
        if (setter.getName().equals("setAttachments")) {
            Attachment leftAttachment = new Attachment();
            leftAttachment.setName("left attachment");
            Attachment rightAttachment = new Attachment();
            rightAttachment.setName("right attachment ");
            leftSet.add(leftAttachment);
            rightSet.add(rightAttachment);

        } else {
            leftSet.add("Test data");
            rightSet.add("different data");
        }
        setter.invoke(left, leftSet);
        assertNotEquals(left, right);
        setter.invoke(right, rightSet);
        assertNotEquals(left, right);
        setter.invoke(right, leftSet);
        assertEquals(left, right);
    }

    @SuppressWarnings("deprecation")
    private void checkDate(Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Date leftDate = new Date();
        leftDate.setYear(2001);
        setter.invoke(left, leftDate);
        assertNotEquals(left, right);
        Date rightDate = new Date();
        setter.invoke(right, rightDate);
        assertNotEquals(left, right);
        setter.invoke(right, leftDate);
        assertEquals(left, right);
    }

    private void checkCalendar(Method setter, Object left, Object right) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Calendar leftDate = Calendar.getInstance();
        leftDate.set(2001, 6, 12);
        setter.invoke(left, leftDate);
        assertNotEquals(left, right);
        Calendar rightDate = Calendar.getInstance();
        setter.invoke(right, rightDate);
        assertNotEquals(left, right);
        setter.invoke(right, leftDate);
        assertEquals(left, right);
    }

    private void assertNotEquals(Object left, Object right) {
        assertFalse("The 2 objects were the same when they should not have been",
                    left.equals(right));
    }
}
