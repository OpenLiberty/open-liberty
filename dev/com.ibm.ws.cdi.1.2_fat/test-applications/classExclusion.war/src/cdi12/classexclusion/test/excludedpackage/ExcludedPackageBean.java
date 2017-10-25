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
package cdi12.classexclusion.test.excludedpackage;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedPackageBean;

@RequestScoped
public class ExcludedPackageBean implements IExcludedPackageBean {
    @Override
    public String getOutput() {
        return "ExcludedPackageBean was correctly injected";
    }

}
