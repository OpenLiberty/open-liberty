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
 * Simple bean, with a few fields and methods needed to test EL 3.0
 */
public class EL30StaticFieldsAndMethodsBean {

    /*
     * A static string
     */
    public static final String staticReference = "static reference";

    /*
     * A non-static string
     */
    public String nonStaticReference = "non-static reference";

    /*
     * A static method
     */
    public static String staticMethod() {
        return "static method";
    }

    /*
     * A static one-parameter method
     */
    public static String staticMethodParam(String s) {
        return s;
    }

    /*
     * A non-static method
     */
    public String nonStaticMethod() {
        return "non-static method";
    }
}
