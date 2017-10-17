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
 * Basic bean to test the EL 3.0 Coercion Rules 1.23.1 which states:
 * If X is null and Y is not a primitive type and also not a String, return null
 */
public class EL30CoercionRulesTestBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Integer myNumber;

    public EL30CoercionRulesTestBean() {
        myNumber = null;
    }

    public Integer getNumber() {
        return myNumber;
    }

    public void setNumber(Integer n) {
        myNumber = n;
    }

}
