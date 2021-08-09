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
package web.war.servlets.el.ltc;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;

@Named
@ApplicationScoped
public class LoginToContinuePropsBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger LOGGER = Logger.getLogger(LoginToContinuePropsBean.class.getSimpleName());

    protected static String _errorPage = "/original/loginError.jsp";
    protected static String _loginPage = "/original/login.jsp";
    protected static Boolean _useForwardToLogin = Boolean.FALSE;

    public String getErrorPage() {
        LOGGER.info("getErrorPage : " + _errorPage);
        return _errorPage;
    }

    public void setErrorPage(String errorPage) {
        LOGGER.info("setErrorPage : " + errorPage);
        _errorPage = errorPage;
    }

    public String getLoginPage() {
        LOGGER.info("getLoginPage : " + _loginPage);
        return _loginPage;
    }

    public void setLoginPage(String loginPage) {
        LOGGER.info("setLoginPage : " + loginPage);
        _loginPage = loginPage;
    }

    public Boolean getUseForwardToLogin() {
        LOGGER.info("getUseForwardToLogin : " + _useForwardToLogin);
        return _useForwardToLogin;
    }

    public void setUseForwardToLogin(Boolean useForwardToLogin) {
        LOGGER.info("setUseForwardToLogin : " + useForwardToLogin);
        _useForwardToLogin = useForwardToLogin;
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("PreDestroy Invoked");
    }

    @PostConstruct
    public void create() {
        LOGGER.info("PostConstruct Invoked");
    }
}
