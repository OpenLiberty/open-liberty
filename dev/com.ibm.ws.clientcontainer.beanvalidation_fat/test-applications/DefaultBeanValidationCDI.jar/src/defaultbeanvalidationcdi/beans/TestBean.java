/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package defaultbeanvalidationcdi.beans;

import javax.enterprise.context.ApplicationScoped;

import defaultbeanvalidationcdi.validation.TestAnnotation;

/**
 * Simple test CDI managed bean that can be injected into other CDI managed
 * beans.
 */
@ApplicationScoped
public class TestBean {

    @TestAnnotation
    String testAnnotation1 = "testAnnotation";

}
