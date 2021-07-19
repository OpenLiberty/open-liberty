/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.classloading.sharedlib;

import ra.DummyQueue;
import ra.DummyTopic;

/**
 * A shared library class that accesses some resource adapter classes.
 */
public class LibraryClassThatUsesResourceAdapterClasses {
    public Object createQueue() {
        return new DummyQueue();
    }

    public Object createTopic() {
        return new DummyTopic();
    }
}
