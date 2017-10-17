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
package jp.test.bean;

import java.io.PrintWriter;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class MyExecutor {
    @Inject
    private MyBeanHolder accessor;

    public void execute(PrintWriter out) {
        out.print(accessor.test1()); // public method
        out.print(":");
        out.println(accessor.test2()); // package private method
    }

}
