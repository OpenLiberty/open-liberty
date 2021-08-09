/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

  /**
   * The interface for objects that support dumping their internal state
   * to the trace system.
   * <p>
   * The <code>dump</code> method differs from <code>toString</code> in that 
   * it is expected that <code>dump</code> provides a verbose description of
   * the object's internal state suitable for debugging purposes. 
   * <code>toString</code> typcially provides a compact string representation
   * that uniquely identifies the object without necessarily revealing details
   * of its internal state.
   * <p>
   * Additionally, <code>dump</code> does not return a string result. Rather,
   * it is expected to format its data to the given output stream.
   */

  public interface Dumpable 
  {
    /**
     * Dump the internal state of an object to the trace stream. <p>
     * <p>
     * Note, this method may be called multiple times while the RAS
     * component manager is processing a dump request. The
     * <code>Dumpable</code> must only perform a dump the first time
     * this method is called. The component manager is guaranteed to
     * call the <code>resetDump()</code> method (see below) before
     * processing another dump request. The first <code>dump</code>()
     * invocation after a call to <code>resetDump</code> must cause
     * the <code>Dumpable</code> to dump its internal state.
     */
    public void dump();

    /**
     * Reset the dump state of this <code>Dumpable</code>.
     * <p>
     * A <code>Dumpable</code> must only dump its internal state on the
     * first <code>dump()</code> invocation after a <code>resetDump()</code>
     * invocation. <p>
     */
    public void resetDump();
  }

