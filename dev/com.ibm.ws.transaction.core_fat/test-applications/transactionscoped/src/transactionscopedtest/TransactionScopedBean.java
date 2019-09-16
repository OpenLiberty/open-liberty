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
package transactionscopedtest;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.transaction.TransactionScoped;

import com.ibm.wsspi.uow.UOWManager;

@TransactionScoped
@SuppressWarnings("serial")
public class TransactionScopedBean implements Serializable {

    @Resource
    UOWManager uowm;

    private String txid;

    private DestroyCallback destroyCallback;

    @PostConstruct
    private void init() {
        txid = uowm.getLocalUOWId() + ":" + System.nanoTime();
        System.out.println("Created " + this.getClass().getCanonicalName() + " instance: " + txid);
    }

    @PreDestroy
    private void destroy() {
        TransactionScopedTestServlet.registerBeanDestroyed(this);
        txid = uowm.getLocalUOWId() + ":" + System.nanoTime();
        System.out.println("Destroyed " + this.getClass().getCanonicalName() + " instance: " + txid);
        if (destroyCallback != null) {
            destroyCallback.destroy();
        }
    }

    public String test() {
        System.out.println("Accessed " + this.getClass().getCanonicalName() + " instance: " + txid);
        return txid;
    }

    public void setDestroyCallback(DestroyCallback destroyCallback) {
        this.destroyCallback = destroyCallback;
    }

}
