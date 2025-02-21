/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.appclientsupport;

import javax.ejb.Remote;
import javax.ejb.Singleton;

import io.openliberty.checkpoint.fat.appclientsupport.view.SimpleGlobalEJBInjectionBeanRemote;

@Singleton
@Remote(SimpleGlobalEJBInjectionBeanRemote.class)
public class SimpleGlobalEJBInjectionBean implements SimpleGlobalEJBInjectionBeanRemote {

    @Override
    public int add(int x, int y) {
        return x + y;
    }
}
