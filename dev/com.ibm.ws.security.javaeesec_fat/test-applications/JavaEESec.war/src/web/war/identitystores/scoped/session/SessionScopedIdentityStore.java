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

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;

import web.war.identitystores.BaseIdentityStore;

@SessionScoped
public class SessionScopedIdentityStore extends BaseIdentityStore implements Serializable {

    private static final long serialVersionUID = 1L;

    {
        sourceClass = SessionScopedIdentityStore.class.getName();
    }

    public SessionScopedIdentityStore() {
        expectedUser = Constants.javaeesec_basicRoleUser_sessionscoped;
    }

}
