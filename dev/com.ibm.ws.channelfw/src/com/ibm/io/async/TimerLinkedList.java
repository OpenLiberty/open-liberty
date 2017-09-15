/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.util.LinkedList;

/**
 * List of work items for the timer thread to process.
 * This class is only used as an aid to help in performance analysis so that
 * this use of LinkedList can be differentiated from other instances of LinkedList
 */
public class TimerLinkedList extends LinkedList<TimerWorkItem> {
    // required SUID since this is serializable
    private static final long serialVersionUID = -1590870373192807194L;

    /**
     * a unique ID for each work item that will be in the timer slots that are
     * in this list. It is put here since the code will be synchronizing
     * on this list when creating uniqueID's.
     */
    // public long uniqueID = 0;

}
