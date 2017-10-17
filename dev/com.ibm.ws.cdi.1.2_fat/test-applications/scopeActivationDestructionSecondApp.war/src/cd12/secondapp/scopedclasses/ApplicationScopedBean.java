/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package cd12.secondapp.scopedclasses;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationScopedBean {

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

}
