/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.miscbean.jar;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;

public class ViewScopedManagedBean implements Serializable {
    /**  */
    private static final long serialVersionUID = -1825264952719408954L;

    private EmailBean emailBean;

    private AppManagerBean appManagerBean;

    private String str1;

    private transient HtmlInputText emailComp;

    @PostConstruct
    public void init() {}

    @PreDestroy
    public void goodBye() {}

    public String updateEmail() {

        try {
            if ((emailBean.getEmail() == null) || (emailBean.getEmail().isEmpty())) {
                appManagerBean.setErrorMessage("Invalid Email: Email can Not be empty");
                return "error";
            }

        } catch (Exception rpe) {
            return "error";
        }
        return "success";
    }

    public EmailBean getEmailBean() {
        return emailBean;
    }

    public void setEmailBean(EmailBean emailBean) {
        this.emailBean = emailBean;
    }

    public AppManagerBean getAppManagerBean() {
        return appManagerBean;
    }

    public void setAppManagerBean(AppManagerBean appManagerBean) {
        this.appManagerBean = appManagerBean;
    }

    public void validateUserUpdate(FacesContext context, UIComponent component, Object value) {}

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public String getStr1() {
        return this.str1;
    }

    public HtmlInputText getEmailComp() {
        return emailComp;
    }

    public void setEmailComp(HtmlInputText emailComp) {
        this.emailComp = emailComp;
    }

}
