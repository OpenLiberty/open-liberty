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

import javax.enterprise.context.ApplicationScoped;

/**
 * This class will not be loaded because it is masked by test.Type1 in the EJB jar.
 * <p>
 * The naming of these classes is important so that when CDI tries to validate this BDA, it will look at test.Type1 first.
 */
@ApplicationScoped
public class Type1 {

    public String getMessage() {
        return "This is Type1 in the war";
    }
}
