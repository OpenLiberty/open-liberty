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
package web.war.mechanisms.scoped.request;

import javax.enterprise.context.RequestScoped;

import web.war.mechanisms.BaseAuthMech;

@RequestScoped
public class RequestScopedAuthMech extends BaseAuthMech {

    public RequestScopedAuthMech() {
        sourceClass = RequestScopedAuthMech.class.getName();
    }

}
