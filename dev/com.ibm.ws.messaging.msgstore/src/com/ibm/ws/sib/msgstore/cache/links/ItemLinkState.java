package com.ibm.ws.sib.msgstore.cache.links;
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
 * This class is a parent class for transaction states. It holds 
 * a set of singleton instances of named transaction state objects
 * that are used instead of an int/String so that state can be 
 * checked in a heap dump by looking at the object type.
 */
public interface ItemLinkState
{
    public static final ItemLinkState STATE_ADDING_LOCKED = ItemLinkStateAddingLocked.instance();

    public static final ItemLinkState STATE_ADDING_UNLOCKED = ItemLinkStateAddingUnlocked.instance();;

    public static final ItemLinkState STATE_AVAILABLE = ItemLinkStateAvailable.instance();;

    public static final ItemLinkState STATE_LOCKED = ItemLinkStateLocked.instance();;

    public static final ItemLinkState STATE_LOCKED_FOR_EXPIRY = ItemLinkStateLockedForExpiry.instance();;

    public static final ItemLinkState STATE_NOT_STORED = ItemLinkStateNotStored.instance();;

    public static final ItemLinkState STATE_PERSISTENTLY_LOCKED = ItemLinkStatePersistentlyLocked.instance();;

    public static final ItemLinkState STATE_PERSISTING_LOCK = ItemLinkStatePersistingLock.instance();;

    public static final ItemLinkState STATE_REMOVING_EXPIRING = ItemLinkStateRemovingExpiring.instance();;

    public static final ItemLinkState STATE_REMOVING_LOCKED = ItemLinkStateRemovingLocked.instance();;

    public static final ItemLinkState STATE_REMOVING_PERSISTENTLY_LOCKED = ItemLinkStateRemovingPersistentlyLocked.instance();;

    public static final ItemLinkState STATE_REMOVING_WITHOUT_LOCK = ItemLinkStateRemovingWithoutLock.instance();;

    public static final ItemLinkState STATE_UNLOCKING_PERSISTENTLY_LOCKED = ItemLinkStateUnlockingPersistentlyLocked.instance();;

    public static final ItemLinkState STATE_UPDATING_DATA = ItemLinkStateUpdatingData.instance();;
}

