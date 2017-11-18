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
package web.war.servlets.customform;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.Password;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

@Named("customLogin")
@RequestScoped
public class CustomLoginBean {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @Inject
    private SecurityContext securityContext;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void login() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Credential credential = new UsernamePasswordCredential(username, new Password(password));

        AuthenticationStatus status = null;
        System.out.println("username : " + username + ", password : " + password);
        status = securityContext.authenticate(getRequest(facesContext), getResponse(facesContext), AuthenticationParameters.withParams().credential(credential));
        System.out.println("AuthenticationStatus : " + status);
    }

    private HttpServletRequest getRequest(FacesContext facesContext) {
        return (HttpServletRequest) facesContext.getExternalContext().getRequest();
    }

    private HttpServletResponse getResponse(FacesContext facesContext) {
        return (HttpServletResponse) facesContext.getExternalContext().getResponse();
    }

}
