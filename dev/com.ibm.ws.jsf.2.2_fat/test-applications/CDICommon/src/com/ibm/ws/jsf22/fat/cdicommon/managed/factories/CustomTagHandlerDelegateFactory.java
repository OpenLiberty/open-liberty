/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.managed.factories;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.BehaviorHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.TagHandlerDelegate;
import javax.faces.view.facelets.TagHandlerDelegateFactory;
import javax.faces.view.facelets.ValidatorHandler;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryAppBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryDepBean;

public class CustomTagHandlerDelegateFactory extends TagHandlerDelegateFactory {

    private final TagHandlerDelegateFactory tagHandlerDelegateFactory;

    // Field injected bean
    @Inject
    private FactoryAppBean fieldBean;

    // Method Injected bean
    private FactoryDepBean methodBean;

    public CustomTagHandlerDelegateFactory(TagHandlerDelegateFactory tagHandlerDelegateFactory) {
        this.tagHandlerDelegateFactory = tagHandlerDelegateFactory;
    }

    String _postConstruct = ":PostConstructNotCalled";

    @PostConstruct
    public void start() {
        _postConstruct = ":PostConstructCalled";
    }

    @PreDestroy
    public void stop() {
        System.out.println(this.getClass().getSimpleName() + " preDestroy called.");
    }

    @Inject
    public void setMethodBean(FactoryDepBean bean) {
        methodBean = bean;
    }

    @Override
    public TagHandlerDelegate createConverterHandlerDelegate(ConverterHandler owner) {

        return tagHandlerDelegateFactory.createConverterHandlerDelegate(owner);
    }

    @Override
    public TagHandlerDelegate createBehaviorHandlerDelegate(BehaviorHandler owner) {

        return tagHandlerDelegateFactory.createBehaviorHandlerDelegate(owner);
    }

    @Override
    public TagHandlerDelegate createComponentHandlerDelegate(ComponentHandler owner) {

        String output = "Field Injected App Bean is NULL";

        if (fieldBean != null) {
            output = fieldBean.getName();
        }

        output += _postConstruct;

        if (methodBean != null) {
            methodBean.incrementAppCount();
            methodBean.logFirst(FacesContext.getCurrentInstance().getExternalContext(), this.getClass().getSimpleName(), "createComponentHandlerDelegate", output);
        }
        else {
            FacesContext.getCurrentInstance().getExternalContext().log("CustomTagHandlerDelegateFactory method injection failed.");
        }

        return tagHandlerDelegateFactory.createComponentHandlerDelegate(owner);

    }

    @Override
    public TagHandlerDelegate createValidatorHandlerDelegate(ValidatorHandler owner) {
        return tagHandlerDelegateFactory.createValidatorHandlerDelegate(owner);

    }
}