/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package io.openliberty.wsoc.util.wsoc;

/*
 * Taken from io.openliberty.wsoc.internal_fat
 */
public abstract class PublishTask implements Runnable {
    MultiClientTestContext mcct = null;

    public void setMultiTestContext(MultiClientTestContext mcct) {
        this.mcct = mcct;
    }

    public MultiClientTestContext getMultiTestContext() {
        return mcct;
    }
}
