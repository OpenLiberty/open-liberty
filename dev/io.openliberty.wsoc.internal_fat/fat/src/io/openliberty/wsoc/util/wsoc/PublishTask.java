/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

public abstract class PublishTask implements Runnable {
    MultiClientTestContext mcct = null;

    public void setMultiTestContext(MultiClientTestContext mcct) {
        this.mcct = mcct;
    }

    public MultiClientTestContext getMultiTestContext() {
        return mcct;
    }
}
