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
package cdi12.classexclusion.test.fallbackbeans;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedByPropertyBean;

@RequestScoped
public class FallbackForExcludedByPropertyBean implements IExcludedByPropertyBean {
    @Override
    public String getOutput() {
        return "ExcludedByPropertyBean was correctly rejected";
    }

}
