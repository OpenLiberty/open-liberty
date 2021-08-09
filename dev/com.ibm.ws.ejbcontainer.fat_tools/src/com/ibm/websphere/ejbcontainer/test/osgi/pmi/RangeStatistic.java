/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer.test.osgi.pmi;

import com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal.AbstractIncrementableStatistic;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class RangeStatistic extends AbstractIncrementableStatistic {
    public void set(long newValue) {
        value.set(newValue);
    }

    public long getCurrent() {
        return value.get();
    }
}
