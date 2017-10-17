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
package main.bean;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InSameWarBean {
    private final Date created;

    public InSameWarBean() {
        created = new Date();
    }

    public String getCreated() {
        return created.toString();
    }

    @Override
    public String toString() {
        return "created in " + created;
    }
}