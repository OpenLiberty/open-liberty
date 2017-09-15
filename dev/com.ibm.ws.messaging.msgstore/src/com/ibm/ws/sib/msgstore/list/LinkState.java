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
package com.ibm.ws.sib.msgstore.list;

/**
 * This class is a parent class for states of links. It holds 
 * a set of singleton instances of link state objects
 * that are used instead of an int/String so that state can be 
 * checked in a heap dump by looking at the object type.
 */
public interface LinkState
{
    /* Head of List - it does not participate in the list proper */
	public static final LinkState HEAD = LinkStateHead.instance();

    /* the link has been linked */
	public static final LinkState LINKED = LinkStateLinked.instance();

    /* the link been logically unlinked, but is still part of the list */
	public static final LinkState LOGICALLY_UNLINKED = LinkStateLogicallyUnlinked.instance();

    /* the link been removed from the list */
	public static final LinkState PHYSICALLY_UNLINKED = LinkStatePhysicallyUnlinked.instance();

    /* Tail of List - it does not participate in the list proper */
	public static final LinkState TAIL = LinkStateTail.instance();
}
