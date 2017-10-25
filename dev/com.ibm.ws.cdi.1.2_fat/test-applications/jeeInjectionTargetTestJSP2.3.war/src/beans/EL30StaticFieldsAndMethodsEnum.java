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
package beans;

/**
 * A simple enumeration class needed for EL 3.0 testing
 */
public enum EL30StaticFieldsAndMethodsEnum {

    TEST_ONE(1),
    TEST_TWO(2),
    TEST_THREE(3);

    private final int testCode;

    EL30StaticFieldsAndMethodsEnum(int levelCode) {
        this.testCode = levelCode;
    }

    public int gettestCode() {
        return this.testCode;
    }
}
