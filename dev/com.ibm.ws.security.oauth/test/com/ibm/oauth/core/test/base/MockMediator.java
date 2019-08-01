/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;

public class MockMediator implements OAuth20Mediator {

    private static int gid;
    private static ThreadLocal<LinkedList<MockMediator>> threadLocal = new ThreadLocal<LinkedList<MockMediator>>();

    private String id;

    private List<OAuthComponentConfiguration> initConfig = new ArrayList<OAuthComponentConfiguration>();
    private List<AttributeList> mediateAuthorizeAttr = new ArrayList<AttributeList>();
    private List<AttributeList> mediateTokenAttr = new ArrayList<AttributeList>();
    private List<AttributeList> mediateResourceAttr = new ArrayList<AttributeList>();
    private List<AttributeList> mediateAuthorizeExceptionAttr = new ArrayList<AttributeList>();
    private List<AttributeList> mediateTokenExceptionAttr = new ArrayList<AttributeList>();
    private List<AttributeList> mediateResourceExceptionAttr = new ArrayList<AttributeList>();
    private List<OAuthException> mediateAuthorizeExceptionEx = new ArrayList<OAuthException>();
    private List<OAuthException> mediateTokenExceptionEx = new ArrayList<OAuthException>();
    private List<OAuthException> mediateResourceExceptionEx = new ArrayList<OAuthException>();

    public static void init() {
        threadLocal.set(new LinkedList<MockMediator>());
    }

    public MockMediator() {
        this.id = "mediator-" + gid++;
        if (threadLocal.get() == null) {
            threadLocal.set(new LinkedList<MockMediator>());
        }
        threadLocal.get().add(this);
    }

    public static LinkedList<MockMediator> getMediators() {
        return threadLocal.get();
    }

    public void init(OAuthComponentConfiguration config) {
        initConfig.add(config);
    }

    public void mediateAuthorize(AttributeList attributeList)
                    throws OAuth20MediatorException {
        mediateAuthorizeAttr.add(attributeList);
    }

    public void mediateToken(AttributeList attributeList)
                    throws OAuth20MediatorException {
        mediateTokenAttr.add(attributeList);
    }

    public void mediateResource(AttributeList attributeList)
                    throws OAuth20MediatorException {
        mediateResourceAttr.add(attributeList);
    }

    public void mediateAuthorizeException(AttributeList attributeList,
                                          OAuthException exception) throws OAuth20MediatorException {
        mediateAuthorizeExceptionAttr.add(attributeList);
        mediateAuthorizeExceptionEx.add(exception);
    }

    public void mediateTokenException(AttributeList attributeList,
                                      OAuthException exception) throws OAuth20MediatorException {
        mediateTokenExceptionAttr.add(attributeList);
        mediateTokenExceptionEx.add(exception);
    }

    public void mediateResourceException(AttributeList attributeList,
                                         OAuthException exception) throws OAuth20MediatorException {
        mediateResourceExceptionAttr.add(attributeList);
        mediateResourceExceptionEx.add(exception);
    }

    public List<AttributeList> getMediateAuthorizeAttr() {
        return Collections.unmodifiableList(mediateAuthorizeAttr);
    }

    public List<AttributeList> getMediateTokenAttr() {
        return Collections.unmodifiableList(mediateTokenAttr);
    }

    public List<AttributeList> getMediateResourceAttr() {
        return Collections.unmodifiableList(mediateResourceAttr);
    }

    public List<AttributeList> getMediateAuthorizeExceptionAttr() {
        return Collections.unmodifiableList(mediateAuthorizeExceptionAttr);
    }

    public List<AttributeList> getMediateTokenExceptionAttr() {
        return Collections.unmodifiableList(mediateTokenExceptionAttr);
    }

    public List<AttributeList> getMediateResourceExceptionAttr() {
        return Collections.unmodifiableList(mediateResourceExceptionAttr);
    }

    public List<OAuthException> getMediateAuthorizeExceptionEx() {
        return Collections.unmodifiableList(mediateAuthorizeExceptionEx);
    }

    public List<OAuthException> getMediateTokenExceptionEx() {
        return Collections.unmodifiableList(mediateTokenExceptionEx);
    }

    public List<OAuthException> getMediateResourceExceptionEx() {
        return Collections.unmodifiableList(mediateResourceExceptionEx);
    }

    public List<OAuthComponentConfiguration> getInitConfig() {
        return initConfig;
    }

    public String getId() {
        return id;
    }
}
