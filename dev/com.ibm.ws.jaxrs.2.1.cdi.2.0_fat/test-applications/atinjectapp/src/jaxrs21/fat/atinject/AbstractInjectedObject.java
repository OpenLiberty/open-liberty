/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.atinject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public abstract class AbstractInjectedObject {

    private static AtomicInteger _nextId = new AtomicInteger();
    private final int id;

    public AbstractInjectedObject() {
        this.id = _nextId.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    public abstract String getInjectionTargetType();
}
