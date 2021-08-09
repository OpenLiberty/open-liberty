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
 * List of work items for the handler threads to process.
 * This class is only used as an aid to help in performance analysis so that
 * this use of LinkedList can be differentiated from other instances of LinkedList
 */
public class CompletedFuturesLinkedList extends LinkedList<CompletedFutureWorkItem> {
    // required SUID since this is serializable
    private static final long serialVersionUID = 4648399594223090738L;

}
