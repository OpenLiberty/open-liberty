/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.bean;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.JspTranslationException;

public class BeanRepository {

    Vector sessionBeans;
    Vector pageBeans;
    Vector appBeans;
    Vector requestBeans;
    Hashtable beanTypes;
    ClassLoader loader;

    public BeanRepository(ClassLoader loader) {
        sessionBeans = new Vector(11);
        pageBeans = new Vector(11);
        appBeans = new Vector(11);
        requestBeans = new Vector(11);
        beanTypes = new Hashtable();
        this.loader = loader;
    }

    public boolean checkSessionBean(String s) {
        return sessionBeans.contains(s);
    }

    public void addSessionBean(String s, String type) throws JspCoreException {
        sessionBeans.addElement(s);
        putBeanType(s, type);
    }

    public boolean hasSessionBeans() {
        return !sessionBeans.isEmpty();
    }

    public Enumeration getSessionBeans() {
        return sessionBeans.elements();
    }

    public boolean checkApplicationBean(String s) {
        return appBeans.contains(s);
    }

    public void addApplicationBean(String s, String type) throws JspCoreException {
        appBeans.addElement(s);
        putBeanType(s, type);
    }

    public boolean hasApplicationBeans() {
        return !appBeans.isEmpty();
    }

    public Enumeration getApplicationBeans() {
        return appBeans.elements();
    }

    public boolean checkRequestBean(String s) {
        return requestBeans.contains(s);
    }

    public void addRequestBean(String s, String type) throws JspCoreException {
        requestBeans.addElement(s);
        putBeanType(s, type);
    }

    public boolean hasRequestBeans() {
        return !requestBeans.isEmpty();
    }

    public Enumeration getRequestBeans() {
        return requestBeans.elements();
    }

    public boolean checkPageBean(String s) {
        return pageBeans.contains(s);
    }

    public void addPageBean(String s, String type) throws JspCoreException {
        pageBeans.addElement(s);
        putBeanType(s, type);
    }

    public boolean hasPageBeans() {
        return !pageBeans.isEmpty();
    }

    public Enumeration getPageBeans() {
        return pageBeans.elements();
    }

    public boolean ClassFound(String clsname) throws ClassNotFoundException {
        Class cls = null;
        cls = loader.loadClass(clsname);
        return !(cls == null);
    }

    public Class getBeanType(String bean, Element element) throws JspCoreException {
        Class cls = null;
        try {
            cls = loader.loadClass((String) beanTypes.get(bean));
        }
        catch (ClassNotFoundException ex) {
            throw new JspTranslationException(element, ex.toString());
        }
        return cls;
    }

    public void putBeanType(String bean, String type) throws JspCoreException {
        try {
            beanTypes.put(bean, type);
        }
        catch (Exception ex) {
            throw new JspCoreException(ex.toString());
        }
    }

    public void removeBeanType(String bean) {
        beanTypes.remove(bean);
    }

    public boolean checkVariable(String bean) {
        return (checkPageBean(bean) || checkSessionBean(bean) || checkRequestBean(bean) || checkApplicationBean(bean));
    }
}
