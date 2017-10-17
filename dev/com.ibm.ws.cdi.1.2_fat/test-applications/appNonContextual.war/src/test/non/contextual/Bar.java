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
package test.non.contextual;

import javax.ejb.Stateful;
import javax.inject.Inject;

@Stateful(name = "Bar")
public class Bar {

    @Inject
    private Foo foo;

    public Foo getFoo() {
        return foo;
    }

}
