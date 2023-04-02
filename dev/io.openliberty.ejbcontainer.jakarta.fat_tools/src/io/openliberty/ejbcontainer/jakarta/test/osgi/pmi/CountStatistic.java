/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
