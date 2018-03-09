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
package web.war.mechanisms.rememberme.secureonlyfalse;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;

import web.war.mechanisms.BaseAuthMech;

@ApplicationScoped
@RememberMe(cookieSecureOnly = false, cookieHttpOnly = false)
public class SecureOnlyFalseHttpOnlyFalseRememberMeAuthMech extends BaseAuthMech {

    public SecureOnlyFalseHttpOnlyFalseRememberMeAuthMech() {
        sourceClass = SecureOnlyFalseHttpOnlyFalseRememberMeAuthMech.class.getName();
    }

}
