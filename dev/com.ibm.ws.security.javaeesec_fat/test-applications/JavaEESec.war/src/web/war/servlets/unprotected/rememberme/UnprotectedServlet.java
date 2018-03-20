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
package web.war.servlets.unprotected.rememberme;

import web.jar.base.FlexibleBaseServlet;

public class UnprotectedServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public UnprotectedServlet() {
        super("UnprotectedServlet");

        mySteps.add(new ProcessSecurityContextAuthenticateStep());
        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }

}