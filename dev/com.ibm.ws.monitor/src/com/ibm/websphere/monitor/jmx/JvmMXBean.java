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
package com.ibm.websphere.monitor.jmx;

/**
 * Management interface for the MBean "WebSphere:type=JvmStats".
 * The Liberty profile makes this MBean available in its platform MBean server when the monitor-1.0 feature is
 * enabled to allow monitoring of the JVM process. This interface can be used to request a proxy object via the {@link javax.management.JMX#newMXBeanProxy} method.
 * 
 * This MXBean is responsible for reporting performance of JVM.
 * The following attributes are available for JVM.
 * 
 * <br>
 * <br>
 * Heap Information
 * <ul>
 * <li>FreeMemory</li>
 * <li>UsedMemory</li>
 * <li>Heap</li>
 * </ul>
 * <br>
 * 
 * CPU Information
 * <ul>
 * <li>ProcessCPU</li>
 * </ul>
 * <br>
 * 
 * 
 * Garbage Collection Information
 * <ul>
 * <li>GCCount</li>
 * <li>GCTime</li>
 * </ul>
 * <br>
 * 
 * JVM Information
 * <ul>
 * <li>UpTime</li>
 * </ul>
 * 
 * @ibm-api
 */
public interface JvmMXBean {

    /**
     * Retrieves the value of the read-only attribute UsedMemory, which is the size of the used heap space in bytes.
     * 
     * @return used memory
     */
    public long getUsedMemory();

    /**
     * Retrieves the value of the read-only attribute FreeMemory, which is the size of unused heap space in bytes.
     * 
     * @return free memory
     */
    public long getFreeMemory();

    /**
     * Retrieves the value of the read-only attribute Heap, which is the total size of the heap space in bytes.
     * 
     * @return heap
     */
    public long getHeap();

    /**
     * Retrieves the value of the read-only attribute UpTime, which is the time in milliseconds since JVM has started.
     * 
     * @return up time
     */
    public long getUpTime();

    /**
     * Retrieves the value of the read-only attribute ProcessCPU, which is the CPU time consumed by JVM.
     * 
     * @return process CPU, or -1 if CPU time is not supported for this JVM
     */
    public double getProcessCPU();

    /**
     * Retrieves the value of the read-only attribute GcCount, which is the number of times garbage collection has been triggered since JVM start.
     * 
     * @return gc count
     */
    public long getGcCount();

    /**
     * Retrieves the value of the read-only attribute GcTime, which is the total garbage collection time in milliseconds. This is an accumulated value.
     * 
     * @return gc time
     */
    public long getGcTime();
}
