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
package web.war.mechanisms.scoped.session;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import web.war.mechanisms.BaseAuthMech;

@SessionScoped
public class SessionScopedAuthMech extends BaseAuthMech implements Serializable {

    private static final long serialVersionUID = 1L;

    public SessionScopedAuthMech() {
        sourceClass = SessionScopedAuthMech.class.getName();
    }

}
