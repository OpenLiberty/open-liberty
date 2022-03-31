/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce.beancreatorlookup;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

@Dependent
public class DependantBean {

    public static final AtomicInteger testCallCount = new AtomicInteger(0);

    public boolean test() {
        testCallCount.incrementAndGet();
        return true;
    }

}
