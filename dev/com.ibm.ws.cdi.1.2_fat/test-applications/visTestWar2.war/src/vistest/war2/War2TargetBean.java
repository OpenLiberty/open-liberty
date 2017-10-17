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
package vistest.war2;

import javax.enterprise.context.ApplicationScoped;

import vistest.framework.TargetBean;
import vistest.qualifiers.InWar2;

@ApplicationScoped
@InWar2
public class War2TargetBean implements TargetBean {

}
