/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

package com.ibm.ejs.ras;

/**
 * The interface for objects that supply a trace-specific string representation.
 * <p>
 * A <code>Traceable</code> object implements <code>toTraceString</code> to
 * supply a trace-specific string representation of itself. An object should
 * only implement this interface if it wants its representation in a trace
 * stream to differ from that provided by its <code>toString</code> method.
 * <p>
 * If an object does not implement the <code>Traceable</code> interface the
 * trace system will just use the result of its <code>toString</code> method to
 * represent it in the trace stream.
 * <p>
 * This version of Traceable exists to allow components that are common between
 * WAS Liberty and WAS Classic to continue implementing the interface from the
 * com.ibm.ejs.ras package (the Traceable interface does not exist in the
 * com.ibm.websphere.ras package in WAS Classic).
 */
public interface Traceable extends com.ibm.websphere.ras.Traceable {

}
