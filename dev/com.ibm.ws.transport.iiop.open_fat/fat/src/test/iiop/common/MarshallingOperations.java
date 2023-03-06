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
package test.iiop.common;

public interface MarshallingOperations {
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