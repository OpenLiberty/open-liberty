/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package web.war.mechanisms.scoped.application;

import javax.enterprise.context.ApplicationScoped;

import web.war.mechanisms.BaseAuthMech;

@ApplicationScoped
public class ApplicationScopedAuthMech extends BaseAuthMech {

    public ApplicationScopedAuthMech() {
        sourceClass = ApplicationScopedAuthMech.class.getName();
    }

}
