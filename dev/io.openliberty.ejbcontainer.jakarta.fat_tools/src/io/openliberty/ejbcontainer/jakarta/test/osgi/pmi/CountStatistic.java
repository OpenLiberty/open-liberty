/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.osgi.pmi;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.internal.AbstractIncrementableStatistic;

@Trivial
public class CountStatistic extends AbstractIncrementableStatistic {
    public long getCount() {
        return value.get();
    }
}
