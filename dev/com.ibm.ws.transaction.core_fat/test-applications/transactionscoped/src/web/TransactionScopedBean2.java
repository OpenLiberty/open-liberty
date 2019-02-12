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
package web;

import java.io.Serializable;

import javax.annotation.PreDestroy;
import javax.transaction.TransactionScoped;

@TransactionScoped
@SuppressWarnings("serial")
public class TransactionScopedBean2 implements Serializable {

    private DestroyCallback destroyCallback;

    @PreDestroy
    private void destroy() {
        if (destroyCallback != null) {
            destroyCallback.destroy();
        }
    }

    public void setDestroyCallback(DestroyCallback destroyCallback) {
        this.destroyCallback = destroyCallback;
    }

}
