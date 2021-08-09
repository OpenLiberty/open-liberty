/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.client;

/**
 * @deprecated As of 6.0, PMI Client API is replaced with
 *             JMX interface and MBean StatisticsProvider model.
 *             PMI CpdCollection data structure is replaced by J2EE
 *             Performance Data Framework defined in
 *             <code>javax.management.j2ee.statistics</code> package.
 * 
 *             <p>
 *             The <code> PerfLevelSpec </code> is WebSphere 4.0 interface used to represent the PMI module
 *             instrumentation level. This interface is replaced by <code>com.ibm.websphere.pmi.stat.StatLevelSpec</code>.
 * 
 * @ibm-api
 */
public interface PerfLevelSpec extends java.io.Serializable {

    /**
     * Get the path of the PerfLevelSpec.
     * It has preleading root "pmi".
     */
    public String[] getPath();

    /**
     * Get the path without root "pmi"
     * It should look like module.instance....
     */
    public String[] getShortPath();

    /**
     * Returns 0 if same
     */
    public int comparePath(PerfLevelSpec otherDesc);

    /**
     * Returns 0 if same
     */
    public int comparePath(String[] otherPath);

    /**
     * Returns true if it's path is a subpath of otherDesc
     */
    public boolean isSubPath(PerfLevelSpec otherDesc);

    public boolean isSubPath(String[] otherPath);

    /**
     * Get module name in the path
     */
    public String getModuleName();

    /**
     * Get submodule name in the path
     */
    public String getSubmoduleName();

    /**
     * Get instrumentation level for the path
     */
    public int getLevel();

    /**
     * Set instrumentation level for the path
     */
    public void setLevel(int level);
}
