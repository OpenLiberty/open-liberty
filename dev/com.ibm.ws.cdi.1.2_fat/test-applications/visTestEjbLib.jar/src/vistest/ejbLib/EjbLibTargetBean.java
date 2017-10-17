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
package vistest.ejbLib;

import javax.enterprise.context.ApplicationScoped;

import vistest.framework.TargetBean;
import vistest.qualifiers.InEjbLib;

@ApplicationScoped
@InEjbLib
public class EjbLibTargetBean implements TargetBean {

}
