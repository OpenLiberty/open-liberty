/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.servlets.customform.post.redirect;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.validation.constraints.NotNull;

@Named("addressbook")
@RequestScoped
@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/customLoginError.jsp", loginPage="/customLogin.xhtml", useForwardToLogin=false, useForwardToLoginExpression=""))
public class AddressBookBean {
    @NotNull
    private String firstname;
    @NotNull
    private String lastname;
    private String emailaddr;
    private String phonenum;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String name) {
        this.firstname = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String name) {
        this.lastname = name;
    }

    public String getEmailaddr() {
        return emailaddr;
    }

    public void setEmailaddr(String value) {
        this.emailaddr = value;
    }

    public String getPhonenum() {
        return phonenum;
    }

    public void setPhonenum(String value) {
        this.phonenum = value;
    }

    public void add() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse res = getResponse(facesContext);
        HttpServletRequest req = getRequest(facesContext);
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<h1>AddressBook</h1>");
        out.println("RemoteUser : " + req.getRemoteUser() + ", firstName : " + firstname + ", lastName : "  + lastname  + ", eMailAddr : " + emailaddr + ", phoneNum : "  + phonenum);
        facesContext.responseComplete();
    }

    private HttpServletRequest getRequest(FacesContext facesContext) {
        return (HttpServletRequest) facesContext.getExternalContext().getRequest();
    }

    private HttpServletResponse getResponse(FacesContext facesContext) {
        return (HttpServletResponse) facesContext.getExternalContext().getResponse();
    }

}
