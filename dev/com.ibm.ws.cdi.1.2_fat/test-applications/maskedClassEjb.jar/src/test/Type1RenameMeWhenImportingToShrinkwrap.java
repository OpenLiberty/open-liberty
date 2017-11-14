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
package test;

/**
 * This class masks the test.Type1 class in maskedClassWeb.war
 */
public class Type1RenameMeWhenImportingToShrinkwrap {

    public String getMessage() {
        return "This is Type3 in the EJB jar";
    }
}
