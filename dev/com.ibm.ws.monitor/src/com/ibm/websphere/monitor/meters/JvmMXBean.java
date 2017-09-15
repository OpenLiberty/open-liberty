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
package com.ibm.websphere.monitor.meters;

/**
 * This is a monitor interface for WebSphere JVM Process.
 * <p>
 * Each WebSphere Application Server (Liberty Profile) instance would have one
 * JVM MXBean.<p>
 * The ObjectName for identifying JVM MXBean is:
 * <p>
 * <b>
 * WebSphere:type=JVM.PerformanceData
 * </b>
 * <p>
 * <br>
 * <br>
 * <br>
 * This MXBean is responsible for reporting performance of JVM.
 * Following attributes are available for JVM.
 * 
 * Heap Information <p>
 * - FreeMemory<p>
 * - UsedMemory<p>
 * - Heap<p>
 * <br>
 * 
 * 
 * CPU Information<p>
 * - ProcessCPU<p>
 * <br>
 * 
 * Garbage Collection Information<p>
 * - GCCount<p>
 * - GCTime<p>
 * <br>
 * 
 * JVM Information<p>
 * - UpTime<p>
 * <br>
 * 
 * <p><p>
 * 
 * 
 * 
 * 
 */
public interface JvmMXBean extends com.ibm.websphere.monitor.jmx.JvmMXBean {

}
