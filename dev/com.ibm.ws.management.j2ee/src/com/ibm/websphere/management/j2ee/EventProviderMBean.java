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
package com.ibm.websphere.management.j2ee;

/**
 * The EventProvider model specifies the eventTypes attribute, which must be
 * implemented by all managed objects that emit events.
 */
public interface EventProviderMBean {

    /**
     * A list of the types of events the managed object emits. The contents of the list
     * are type strings.
     */
    String[] geteventTypes();

}
