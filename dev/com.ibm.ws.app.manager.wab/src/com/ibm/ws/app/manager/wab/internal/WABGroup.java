/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ibm.ws.app.manager.module.DeployedAppInfo;

final class WABGroup {
    private ConcurrentLinkedQueue<WAB> wabs;
    private volatile boolean groupUninstalled = false;
    private final DeployedAppInfo deployedAppInfo;

    WABGroup(DeployedAppInfo deployedAppInfo) {
        this.deployedAppInfo = deployedAppInfo;
    }

    DeployedAppInfo getDeployedAppInfo() {
        return this.deployedAppInfo;
    }

    //this is called by the WAB itself when it responds to it's own sub tracker opening
    //after adding the wab to the web container etc..
    void addWab(WAB wab, WABInstaller installer) {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException();
        }
        if (wabs == null) {
            wabs = new ConcurrentLinkedQueue<WAB>();
        }
        wabs.add(wab);

        //if the group is uninstalled, this is a late wab addition, so clean it up.
        if (groupUninstalled)
            uninstallGroup(installer);
    }

    Queue<WAB> getWABs() {
      return wabs;
    }

    //this method MUST NOT be called with the wabgroup locked.. (and must not lock the wabGroup)
    //this method is only used when the entire wabGroup is being removed, which occurs during
    //server-stop and in response to the removal of the wab feature from the server config.
    void uninstallGroup(WABInstaller installer) {
        //call for each of them to be removed
        //in practice this will only mean one web container removal for the group
        //but we need all the removes to update the WAB states.
        if (wabs != null) {
            //iterating over a concurrentlinkedqueue without a lock, means we'll see everything in the list
            //thats present when we get the iterator. it will NOT throw concurrentmodification if the list is
            //modified during iteration, but also does not guarantee to reflect all external modifications
            List<WAB> toRemove = new ArrayList<WAB>();
            for (WAB wab : wabs) {
                //remove the wab from the webcontainer, if needed.
                wab.terminateWAB();
                wab.removeWAB();
                toRemove.add(wab);
            }
            wabs.removeAll(toRemove);
            if (wabs.size() != 0) {
                installer.wabLifecycleDebug("First pass wab group uninstall detected outstanding WABs", wabs);
                toRemove.clear();
                for (WAB wab : wabs) {
                    //remove the wab from the webcontainer, if needed.
                    wab.terminateWAB();
                    wab.removeWAB();
                    toRemove.add(wab);
                }
                wabs.removeAll(toRemove);
                //any left in the 2nd pass just don't get removed.
                if (wabs.size() != 0) {
                    installer.wabLifecycleDebug("Second pass wab group uninstall detected outstanding WABs", wabs);
                }
            }
        }
    }

    /**
     * Tells this wab group to forget this wab
     * DOES NOT UNINSTALL the wab.
     *
     * @param wab
     * @return true if group is empty
     */
    boolean removeWAB(WAB wab) {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException();
        }

        //this is invoked _after_ the wab is uninstalled from the web container,
        //and is our chance to forget the wab, thus we don't need to care about
        //the group uninstallation state when we forget this one.
        wabs.remove(wab);
        return wabs.isEmpty();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.hashCode());
        sb.append(":");
        sb.append(wabs);
        return sb.toString();
    }
}
