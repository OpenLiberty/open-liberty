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
package web.war.identitystores.scoped.session;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.PassivationCapable;

import web.war.identitystores.BaseIdentityStore;

@SessionScoped
public class SessionScopedIdentityStore extends BaseIdentityStore implements PassivationCapable {

    {
        sourceClass = SessionScopedIdentityStore.class.getName();
    }

    @Override
    public String getId() {
        return String.valueOf(SessionScopedIdentityStore.class.getName() + "#" + this.hashCode() + "#" + System.nanoTime());
    }

}
