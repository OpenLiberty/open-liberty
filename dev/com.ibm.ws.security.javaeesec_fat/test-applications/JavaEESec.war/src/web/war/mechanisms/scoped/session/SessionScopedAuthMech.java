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

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.PassivationCapable;

import web.war.mechanisms.BaseAuthMech;

@SessionScoped
public class SessionScopedAuthMech extends BaseAuthMech implements PassivationCapable {

    public SessionScopedAuthMech() {
        sourceClass = SessionScopedAuthMech.class.getName();
    }

    @Override
    public String getId() {
        return String.valueOf(SessionScopedAuthMech.class.getName() + "#" + this.hashCode() + "#" + System.nanoTime());
    }

}
