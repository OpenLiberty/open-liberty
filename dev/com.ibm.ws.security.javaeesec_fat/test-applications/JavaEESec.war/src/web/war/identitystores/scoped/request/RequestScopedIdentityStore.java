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
package web.war.identitystores.scoped.request;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;

import web.war.identitystores.BaseIdentityStore;

@RequestScoped
public class RequestScopedIdentityStore extends BaseIdentityStore {

    {
        sourceClass = RequestScopedIdentityStore.class.getName();
    }

    public RequestScopedIdentityStore() {
        expectedUser = Constants.javaeesec_basicRoleUser_requestscoped;
    }

}
