package com.ibm.ws.sib.msgstore.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

 
/**
 * This class is a parent class for MessageStore states. It
 * holds a set of singleton instances of named MessageStore state
 * objects that are used instead of an int/String so that state
 * can be checked in a heap dump by looking at the object type.
 */
public interface MessageStoreState
{
    /**
     * MessageStore has not yet been initialized.
     */
    public static final MessageStoreState STATE_UNINITIALIZED = MessageStoreStateUninitialized.instance();
    /**
     * MessageStore is in the process of starting.
     */
    public static final MessageStoreState STATE_STARTING = MessageStoreStateStarting.instance();
    /**
     * MessageStore has started.
     */
    public static final MessageStoreState STATE_STARTED = MessageStoreStateStarted.instance();
    /**
     * MessageStore is stopped.
     */
    public static final MessageStoreState STATE_STOPPED = MessageStoreStateStopped.instance();
}
