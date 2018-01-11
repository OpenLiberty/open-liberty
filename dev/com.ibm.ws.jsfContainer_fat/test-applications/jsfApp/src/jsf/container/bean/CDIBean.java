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
package jsf.container.bean;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named("cdiBean")
@SessionScoped
public class CDIBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private String data = ":" + getClass().getSimpleName() + ":";

    @EJB
    TestEJB ejb;

    @Resource
    ManagedExecutorService defaultExec;

    @PostConstruct
    public void start() {
        System.out.println("CDIBean postConstruct called");
        this.data += ":PostConstructCalled:";
        if (ejb != null && ejb.verifyPostConstruct())
            this.data += ":EJB-injected:";
        if (defaultExec != null)
            this.data += ":Resource-injected:";
        System.out.println("CDIBean data is: " + data);
    }

    @PreDestroy
    public void stop() {
        System.out.println("CDIBean preDestroy called.");
    }

    public void setData(String newData) {
        this.data += newData;
    }

    public String getData() {
        return this.data;
    }

    public String nextPage() {
        return "TestBean";
    }
}
