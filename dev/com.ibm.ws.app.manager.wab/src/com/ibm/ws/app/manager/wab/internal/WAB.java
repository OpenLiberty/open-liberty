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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.app.manager.wab.internal.WABState.State;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Lock hierarchy..
 *
 * This class wraps the tracker lifecycle controls in a lock on the AtomicReference 'terminated'
 * - this lock is used to guard the reasoning when the tracker is started,
 * to prevent add/remove callbacks occuring before we are complete making lifecycle choices.
 * - the lock protects add / remove bundle callbacks for the tracker customizer, preventing
 * any processing from occurring until the lock is free.
 * - additionally, the lock is used to protect the boolean behind the AtomicReference itself.
 * this allows us to treat the reference both as a gate to protect function, and as a flag
 * checked after the lock is acquired to know that the tracker has been terminated, and no
 * action should be taken.
 *
 * The other lock is the 'add remove lock' .
 * - used to ensure we do not attempt an add and a remove operation concurrently for the same WAB.
 * - Usually this is called while holding the terminated lock, but it is also called from
 * wabgroup _AFTER_ the wab has been terminated.
 * - Once terminated, the add remove lock is used without an enclosing terminated lock, from wab
 * group to shutdown and remove the wabs from the webcontainer.
 *
 * Critically, code inside the add remove lock never attempts to obtain the terminated lock.
 *
 */
class WAB implements BundleTrackerCustomizer<WAB> {

    static final String OSGI_WEB_EVENT_TOPIC_PREFIX = "org/osgi/service/web/";

    private static final Bundle extenderBundle;
    private static final long extenderId;
    private static final String extenderSymbolicName;
    private static final Version extenderVersion;

    static {
        extenderBundle = FrameworkUtil.getBundle(WAB.class);
        extenderId = extenderBundle.getBundleId();
        extenderSymbolicName = extenderBundle.getSymbolicName();
        extenderVersion = extenderBundle.getVersion();
    }

    private final Bundle wabBundle;
    private final long wabBundleId;
    private final String wabBundleSymbolicName;
    private final Version wabBundleVersion;
    private final String wabContextPath;
    private final WABInstaller installer;
    private final String rawVirtualHost;
    private final String resolvedVirtualHost;

    private final WABTracker<WAB> trackerForThisWAB;

    private static class AddRemoveLock extends Object {};

    private final AddRemoveLock addRemoveLock = new AddRemoveLock();

    private final AtomicReference<Boolean> terminated = new AtomicReference<Boolean>(Boolean.FALSE);

    WAB(Bundle wabBundle, String wabContextPath, WABInstaller installer) {
        this.wabBundle = wabBundle;
        this.wabBundleId = wabBundle.getBundleId();
        this.wabBundleSymbolicName = wabBundle.getSymbolicName();
        this.wabBundleVersion = wabBundle.getVersion();
        this.wabContextPath = wabContextPath;
        this.installer = installer;
        this.rawVirtualHost = wabBundle.getHeaders().get("OL-VirtualHost");
        this.resolvedVirtualHost = resolveVirtualHost();
        trackerForThisWAB = installer.getTracker(this);
    }

    void terminateWAB() {
        synchronized (terminated) {
            installer.wabLifecycleDebug("SubTracker being terminated ", this);
            terminated.set(true);
            //Shut the tracker down.. it can't process events now we are terminated
            //and any pending events just became no-ops.
            closeTracker();
        }
    }

    void performCollisionResolution() {
        //kick any other wabs waiting for this ctx path
        installer.attemptRedeployOfPreviouslyCollidedContextPath(wabContextPath);
    }

    void enableTracker() {
        //tracking count is -1 when the tracker is not open yet.
        if (trackerForThisWAB.getTrackingCount() == -1) {
            installer.executeRunnable(new Runnable() {
                @Override
                public void run() {
                    // Don't bother running if the server is shutting down.
                    if (FrameworkState.isStopping()) {
                        return;
                    }

                    installer.wabLifecycleDebug("SubTracker open has initiated. ", WAB.this);
                    synchronized (terminated) {
                        installer.wabLifecycleDebug("SubTracker open has obtained terminate lock ", WAB.this);
                        //quick exit if we were terminated before we woke up.
                        if (terminated.get())
                            return;

                        //Only start the WAB if we are still in state undeploying.
                        //because this runs async, we may have uninstalled the bundle
                        //before we wake up to track the state.
                        if (getState() == State.DEPLOYING) {
                            installer.wabLifecycleDebug("SubTracker opening tracker.", WAB.this);

                            trackerForThisWAB.open();

                            installer.wabLifecycleDebug("SubTracker open.", WAB.this);
                        }

                        //after opening the tracker, we should be in deployed state!
                        //because the open call should have synchronously invoked
                        //addingBundle for matching bundles, causing us to perform
                        //the deploy, resulting in DEPLOYED, or FAILED.

                        //if we are stuck in DEPLOYING, that means the
                        //opening of the tracker did not deploy us.

                        //which is kinda odd, because we were created by WABInstaller
                        //because our bundle was in starting/active, and normally calling
                        //tracker.open should have driven the addingBundle method on us,
                        //that should have installed the WAB and moved it's state along.

                        //it's possible that at this point, the WAB we represent
                        //has already left active state before we woke up to create the
                        //tracker.. so here we take the approach that under those circumstances
                        //we will fail the WAB deploy.

                        //if the WAB is still in active state, then osgi has chosen not
                        //to call us back on our thread during open, and has likely already
                        //called us back on a different thread, which is now blocked
                        //at the addingBundle method, by the lock we are holding on terminated.

                        int bundleState = wabBundle.getState();
                        //if our bundle is still in active/deploying.. then we 'should' be told about it
                        //via open.. but mebbe it will come in later..
                        if (getState() == State.DEPLOYING) {
                            //still in deploying.. are we still eligible to start
                            //(eg, could there be an addingBundle call pending on another thread)
                            if (!(bundleState == Bundle.ACTIVE || bundleState == Bundle.STARTING)) {
                                //bundle state is no longer eligible..
                                //most likely we missed the bundle, it reverted to stopped
                                //before we woke up to open the tracker.

                                installer.wabLifecycleDebug("SubTracker detected WAB state has not advanced, and bundle state no longer eligible.", WAB.this, wabBundle.getState());

                                //our only option in this case is to move to FAILED, as the state diagram
                                //for WABS does not allow us to move back from DEPLOYING to UNDEPLOYED.
                                setState(State.FAILED);

                                //set terminated, to block other threads that may be blocked at
                                //the sync blocks in added/removed bundles from proceeding to install
                                //the wab we just declared failed.
                                terminated.set(true);

                                //remove use from the collision set in the installer..
                                installer.removeWabFromEligibleForCollisionResolution(WAB.this);

                                //finally, kill off the tracker, as we've said we're done here.
                                closeTracker();

                                //and.. tell the installer to trigger collision resolution for
                                //this context path.
                                performCollisionResolution();
                            } else {
                                //the wab state was still DEPLOYING, and the bundle state must still be active or starting.
                                //we allow the wab to proceed, we should get our callback soon.
                                installer.wabLifecycleDebug("SubTracker did not advance WAB state during open, and bundle is still eligible.", this, wabBundle.getState());
                            }
                        }
                    }
                }
            });
        }
    }

    //WABs always start in the undeployed state
    private final AtomicReference<State> state = new AtomicReference<State>(State.UNDEPLOYED);

    String getContextRoot() {
        return this.wabContextPath;
    }

    Bundle getBundle() {
        return this.wabBundle;
    }

    State getState() {
        return state.get();
    }

    String getVirtualHost() {
        return this.resolvedVirtualHost;
    }

    boolean isResolvedVirtualHostValid() {
        String reResolvedVirtualHost = resolveVirtualHost();

        if (resolvedVirtualHost == null) {
            return reResolvedVirtualHost == null;
        }

        return resolvedVirtualHost.equals(reResolvedVirtualHost);
    }

    private String resolveVirtualHost() {
        if (rawVirtualHost == null) return null;
    
        if (rawVirtualHost.startsWith("${")) {
            String resolvedVirtHost = installer.resolveVariable(rawVirtualHost);
            if (rawVirtualHost.equals(resolvedVirtHost)) {
                return null;
            } else {
                return resolvedVirtHost;
            }
        }
        return rawVirtualHost;
    }

    /**
     * state should only transition while the terminated lock is held.
     *
     * @param newState
     * @return
     */
    private boolean setState(State newState) {
        switch (newState) {
            case DEPLOYED:
                return changeState(State.DEPLOYING, State.DEPLOYED);
            case DEPLOYING:
                //can move from either UNDEPLOYED or FAILED into DEPLOYING state
                return (changeState(State.UNDEPLOYED, State.DEPLOYING) || changeState(State.FAILED, State.DEPLOYING));
            case UNDEPLOYING:
                return changeState(State.DEPLOYED, State.UNDEPLOYING);
            case UNDEPLOYED:
                return changeState(State.UNDEPLOYING, State.UNDEPLOYED);
            case FAILED:
                return changeState(State.DEPLOYING, State.FAILED);
            default:
                return false;
        }
    }

    private boolean changeState(State oldState, State newState) {
        if (state.compareAndSet(oldState, newState)) {
            //post the web extender event corresponding to the state change
            //Failed events are posted elsewhere
            if (newState != State.FAILED)
                installer.postEvent(createEvent(newState));
            return true;
        } else
            return false;
    }

    public void attemptDeployOfPreviouslyBlockedWab() {
        //perform the re-enable inside a sync on terminated, like all state transitions should be.
        synchronized (terminated) {
            if (terminated.get()) {
                return;
            }
            //transitions from failed to deploying are allowed..
            //any colliding wabs should be in failed state.
            if (setState(State.DEPLOYING)) {
                installer.wabLifecycleDebug("WAB opening SubTracker for WAB as a result of collision resolution ", this);
                enableTracker();
            }
        }
    }

    public boolean moveToDeploying() {
        synchronized (terminated) {
            if (terminated.get())
                return false;
            return setState(State.DEPLOYING);
        }
    }

    private ApplicationInfo appInfo;
    private boolean createdAppInfo = false;

    void setApplicationInfo(ApplicationInfo appInfo) {
        this.appInfo = appInfo;
    }

    ApplicationInfo getApplicationInfo() {
        return appInfo;
    }

    void setCreatedApplicationInfo() {
        this.createdAppInfo = true;
    }

    boolean getCreatedApplicationInfo() {
        return this.createdAppInfo;
    }

    private DeployedModuleInfo deployedModuleInfo = null;

    void setDeployedModuleInfo(DeployedModuleInfo deployedModuleInfo) {
        if (this.deployedModuleInfo == null)
            this.deployedModuleInfo = deployedModuleInfo;
    }

    DeployedModuleInfo getDeployedModuleInfo() {
        return deployedModuleInfo;
    }

    Event createEvent(State t) {
        return createEvent(t, null, null, null);
    }

    //This version of createFailed event also removes the wab from the collision set
    //and kicks off the collision resolution process
    Event createFailedEvent(Throwable t) {
        synchronized (terminated) {
            if (terminated.get()) {
                return null;
            }
            if (setState(State.FAILED)) {
                installer.removeWabFromEligibleForCollisionResolution(this);
                installer.attemptRedeployOfPreviouslyCollidedContextPath(this.wabContextPath);
                return createEvent(State.FAILED, t, null, null);
            }
        }
        return null;
    }

    //This version of createFailed DOES NOT remove the wab from the eligible set..
    //as it's used only to report collision failures =)
    Event createFailedEvent(String collisionContext, long[] cIds) {
        synchronized (terminated) {
            if (terminated.get()) {
                return null;
            }
            if (setState(State.FAILED))
                return createEvent(State.FAILED, null, collisionContext, cIds);
        }
        return null;
    }

    private Event createEvent(State t, Throwable e, String collisionContext, long[] cIds) {
        Dictionary<String, Object> eventProps = createEventProperties();
        if (State.FAILED.equals(t)) {
            if (collisionContext != null && cIds.length > 0) {
                eventProps.put("collision", collisionContext); //string
                eventProps.put("collision.bundles", cIds); //long bundle ids for WABs with same context path
            } else if (e != null) {
                eventProps.put("exception", e);//throwable
            }
        }
        return new Event(OSGI_WEB_EVENT_TOPIC_PREFIX + t.toString(), eventProps);
    }

    Dictionary<String, Object> createEventProperties() {
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        //add the extender properties
        props.put("extender.bundle", extenderBundle); //bundle
        props.put("extender.bundle.id", extenderId); //long
        props.put("extender.bundle.symbolicName", extenderSymbolicName); //string
        props.put("extender.bundle.version", extenderVersion); //version

        //add the properties for this wab Bundle
        props.put("bundle.symbolicName", wabBundleSymbolicName); //string
        props.put("bundle.id", wabBundleId); //long
        props.put("bundle", wabBundle); //bundle
        props.put("bundle.version", wabBundleVersion); //version
        props.put("context.path", wabContextPath); //string

        //add the timestamp
        props.put("timestamp", System.currentTimeMillis()); //long

        return props;
    }

    /**
     * ONLY INVOKE WHILE HOLDING THE terminated lock
     */
    private boolean addToWebContainer() {
        if (!Thread.holdsLock(terminated)) {
            throw new IllegalStateException();
        }
        synchronized (addRemoveLock) {
            if (installer.installIntoWebContainer(this)) {
                setState(State.DEPLOYED);
                return true;
            } else {
                closeTracker();
                return false;
            }
        }
    }

    /**
     * ONLY INVOKE WHILE HOLDING THE terminated lock
     */
    private void removeFromWebContainer() {
        if (!Thread.holdsLock(terminated)) {
            throw new IllegalStateException();
        }
        synchronized (addRemoveLock) {
            //we'll either succeed or fail at the removal, but either
            //way we want to guarantee the tracker is closed.
            closeTracker();
            if (getDeployedModuleInfo() != null) {
                //uninstall can only fail if the webcontainer has gone away..
                //so we do not need to check if it returns true/false
                //as in both cases, we wish to unregister, and declare we are
                //undeployed.
                installer.uninstallFromWebContainer(this);
                unregister();
                setState(State.UNDEPLOYED);
            }
        }
    }

    /* Service registration of the WAB's Bundle object */

    private ServiceRegistration<Bundle> reg = null;

    synchronized void setRegistration(ServiceRegistration<Bundle> reg) {
        this.reg = reg;
    }

    private ServiceRegistration<ServletContext> scReg = null;

    void registerServletContext(ServletContext sc) {
        //Register the ServletContext in the service registry
        Dictionary<String, Object> scRegProps = new Hashtable<String, Object>(3);
        scRegProps.put("osgi.web.symbolicname", wabBundleSymbolicName);
        scRegProps.put("osgi.web.version", wabBundleVersion);
        scRegProps.put("osgi.web.contextpath", wabContextPath);
        scReg = wabBundle.getBundleContext().registerService(ServletContext.class, sc, scRegProps);
    }

    @FFDCIgnore(IllegalStateException.class)
    void unregister() {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                /*
                 * The OSGi documentation specifies that an IllegalStateException
                 * is thrown only if the service has already been unregistered -
                 * if we are shutting down we may encounter this scenario, as the
                 * Bundle which registered this service, namely the WABInstaller,
                 * may be stopping or have already stopped
                 */
            }
        }
        if (scReg != null) {
            try {
                scReg.unregister();
            } catch (IllegalStateException e) {
                /*
                 * The OSGi documentation specifies that an IllegalStateException
                 * is thrown only if the service has already been unregistered -
                 * if we are shutting down we may encounter this scenario, as the
                 * Bundle which registered this service, namely the WABInstaller,
                 * may be stopping or have already stopped
                 */
            }
        }
    }

    /** {@inheritDoc} */
    //marked trivial as there will be one of these per wab, and they get told about all bundles in the framework..
    //so we don't want that in the trace all the time
    @Override
    @Trivial
    public WAB addingBundle(Bundle bundle, BundleEvent event) {
        //the bundle is in STARTING | ACTIVE state because of our state mask
        //only action work for the bundle represented by this WAB.
        if (bundle.getBundleId() == wabBundleId) {

            //sync lock inside bundle id check to avoid locking on every bundle!
            synchronized (terminated) {
                if (terminated.get()) {
                    installer.wabLifecycleDebug("SubTracker unable to add bundle, as has been terminated.", this, bundle.getBundleId());
                    //if we are terminated, then the WABGroup will be handling the uninstall
                    //and state transitions.. so it's important not to get involved here.
                    return null;
                }
                installer.wabLifecycleDebug("SubTracker adding bundle.", this);

                //only deploy the wab, if the state is still deploying..
                //it pretty much should be..
                if (getState() == State.DEPLOYING) {
                    installer.wabLifecycleDebug("SubTracker adding WAB to WebContainer", this);

                    //the holder will already have the wab in its list from construction
                    //no collision, just add to the web container - only return the WAB tracked object if successful
                    return addToWebContainer() ? WAB.this : null;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    //marked trivial as there will be one of these per wab, and they get told about all bundles in the framework..
    //so we don't want that in the trace all the time
    public void modifiedBundle(final Bundle bundle, BundleEvent event, WAB wab) {
        //No-op, we don't mind about changes between starting/active
        //if we ever add code here, it should be sync'd on terminated
        //and become a no-op if terminated is true.
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    //marked trivial as there will be one of these per wab, and they get told about all bundles in the framework..
    //so we don't want that in the trace all the time
    public void removedBundle(Bundle bundle, BundleEvent event, WAB wab) {
        if (bundle.getBundleId() == wabBundleId) {
            synchronized (terminated) {
                if (terminated.get()) {
                    installer.wabLifecycleDebug("SubTracker unable to remove bundle, as has been terminated.", this, wab);
                    //if we are terminated, then the WABGroup will be handling the uninstall
                    //and state transitions.. so it's important not to get involved here.
                    return;
                } else {
                    installer.wabLifecycleDebug("Sub Tracker for WAB processing remove event", wab);
                    //invoke the removal logic.
                    //we already hold the terminated lock, so will be allowed to continue in that method.
                    removeWAB();
                    terminateWAB();
                }
            }
        }
    }

    public boolean removeWAB() {
        synchronized (terminated) {
            if (terminated.get()) {
                installer.wabLifecycleDebug("WAB processing removal of terminated WAB.", this);
                //do not exit early for a terminated wab.. this is the one method that
                //will be used to tidy up a wab after it's been killed off.
            } else {
                installer.wabLifecycleDebug("WAB processing removal.", this);
            }

            //first up, it's time to remove us from the set that's eligible for reinstall.
            installer.removeWabFromEligibleForCollisionResolution(this);

            boolean result = true;
            switch (getState()) {
                case DEPLOYED:
                    //easy one this.. wab is deployed
                    result &= setState(State.UNDEPLOYING);
                    if (result) {
                        removeFromWebContainer();
                    }
                    //if the wab makes it out of the web container,
                    //collision resolution will occur normally.
                    //the only alternative currently is the web container
                    //has gone away, in which case, collision resolution
                    //is impossible.
                    //undeploying can only move to undeployed in
                    //the current code.
                    return result;
                case DEPLOYING:
                    //if state is still deploying, then the
                    //wab was scheduled to be deployed, but its
                    //tracker didn't open before we terminated it.
                    //we could go thru deployed->undeploying, but
                    //going thru failed seems more appropriate,
                    //the wab was never deployed, and "failed" because
                    //the extender is going away.
                    result &= setState(State.FAILED);
                    //we must perform collision resolution here, as
                    //the state will never become undeployed.
                    performCollisionResolution();
                    return result;
                case UNDEPLOYING:
                    //state is already undeploying..
                    removeFromWebContainer();
                    //if the wab makes it out of the web container,
                    //collision resolution will occur normally.
                    //undeploying can only move to undeployed in
                    //the current code.
                    return result;
                case UNDEPLOYED:
                    return result;
                case FAILED:
                    return result;
                default:
                    return false;
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.hashCode());
        sb.append(":");
        sb.append(this.wabBundleId);
        sb.append(":");
        sb.append(getState());
        sb.append(":");
        sb.append(this.wabContextPath);
        return sb.toString();
    }

    /**
     *
     */
    private void closeTracker() {
        installer.wabLifecycleDebug("SubTracker closing tracker.", WAB.this, wabBundle.getState());
        //-1 means the tracker isn't open.. if it's not open, we don't need to close it.
        if (trackerForThisWAB.getTrackingCount() != -1) {
            trackerForThisWAB.close();
            installer.wabLifecycleDebug("SubTracker closed.", WAB.this, wabBundle.getState());
        }
    }
}
