/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelResolver;
import com.ibm.ws.kernel.boot.internal.KernelResolver.KernelBundleElement;
import com.ibm.ws.kernel.boot.internal.KernelStartLevel;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.LibertyBootRuntime;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 * The BootstrapProvisioner loads the initial framework and platform bundles to
 * complete the server bootstrapping process. More bundles are resolved and
 * loaded by the RuntimeProvisioner after the OSGi framework is running.
 * <p>
 * Trace must be accomplished via Logger, as Tr is not available outside of the framework
 */
public class ProvisionerImpl implements Provisioner {
    private static final String ME = ProvisionerImpl.class.getName();
    private static final TraceComponent tc = Tr.register(ProvisionerImpl.class);
    private static final String BUNDLE_LOC_KERNEL_TAG = "kernel@";
    private ServiceTracker<LibertyBootRuntime, LibertyBootRuntime> serviceTracker;

    /**
     * Reference to a bundle context (could be the system bundle or the kernel
     * bundle)
     */
    protected BundleContext context = null;
    protected FrameworkStartLevel frameworkStartLevel = null;

    /**
     * Install the platform bundles, and check the returned install status for
     * exceptions, and issue appropriate diagnostics. Specifically, look for
     * pre-install exceptions, install
     * exceptions, or flat-out missing bundles (things listed in the bundle list
     * that didn't match any of the jars in the install dir).
     *
     * @param provisioner
     * @param platformBundles
     * @return installStatus
     * @throws LaunchException
     */
    @Override
    public void initialProvisioning(BundleContext systemBundleCtx, BootstrapConfig config) throws InvalidBundleContextException {
        BundleInstallStatus installStatus;
        BundleStartStatus startStatus;

        // Obtain/initialize provisioner-related services and resources
        getServices(systemBundleCtx);
        try {

            // Install the platform bundles (default start level of kernel,
            // minimum level of bootstrap)
            installStatus = installBundles(config);
            checkInstallStatus(installStatus);

            // Un-publicized boolean switch: if true, we won't start the
            // initially provisioned bundles: they would have to be started manually from the osgi
            // console. Also, only start bundles if the framework isn't already stopping
            // for some other reason..
            String bundleDebug = config.get("kernel.debug.bundlestart.enabled");

            // getBundle() will throw IllegalStateException (per spec)
            // if context is no longer valid (i.e. framework is stoppping.. )
            if (systemBundleCtx.getBundle() != null && (bundleDebug == null || "false".equals(bundleDebug))) {
                startStatus = startBundles(installStatus.getBundlesToStart());
                checkStartStatus(startStatus);

                // Update start level through the feature prepare layer.
                // we'll get errors if any of the bundles we need have issues
                // starting...
                startStatus = setFrameworkStartLevel(KernelStartLevel.FEATURE_PREPARE.getLevel());
                checkStartStatus(startStatus);
            }
        } finally {
            // Cleanup provisioner-related services and resources
            releaseServices();
        }
    }

    protected void checkInstallStatus(BundleInstallStatus installStatus) {
        final String m = "checkInstallStatus";

        if (installStatus.installExceptions()) {
            Map<String, Throwable> installExceptions = installStatus.getInstallExceptions();
            for (Entry<String, Throwable> entry : installExceptions.entrySet()) {
                FFDCFilter.processException(entry.getValue(), ME, m, this,
                                            new Object[] { entry.getKey() });
            }
            throw new LaunchException("Exceptions occurred while installing platform bundles", BootstrapConstants.messages.getString("error.platformBundleException"));
        }

        if (installStatus.bundlesMissing())
            throw new LaunchException("Missing platform bundles: " + installStatus.listMissingBundles(), BootstrapConstants.messages.getString("error.missingBundleException"));

        if (installStatus.bundlesToStart() == false) {
            throw new LaunchException("No required bundles installed", BootstrapConstants.messages.getString("error.noStartedBundles"));
        }
    }

    /**
     * Check the passed in start status for exceptions starting bundles, and
     * issue appropriate diagnostics & messages for this environment.
     *
     * @param startStatus
     * @throws InvalidBundleContextException
     */
    protected void checkStartStatus(BundleStartStatus startStatus) throws InvalidBundleContextException {
        final String m = "checkInstallStatus";
        if (startStatus.startExceptions()) {
            Map<Bundle, Throwable> startExceptions = startStatus.getStartExceptions();
            for (Entry<Bundle, Throwable> entry : startExceptions.entrySet()) {
                Bundle b = entry.getKey();
                FFDCFilter.processException(entry.getValue(), ME, m, this,
                                            new Object[] { b.getLocation() });
            }

            throw new LaunchException("Exceptions occurred while starting platform bundles", BootstrapConstants.messages.getString("error.platformBundleException"));
        }

        if (!startStatus.contextIsValid())
            throw new InvalidBundleContextException();
    }

    /**
     * Fetch required services: should be called before provisioning operations.
     */
    protected void getServices(BundleContext bundleCtx) {
        context = bundleCtx;

        // OSGi 4.3 uses the bundle.adapt() mechanism as a way to get to
        // behavior that is fundamentally a part of the framework
        frameworkStartLevel = bundleCtx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkStartLevel.class);
        frameworkStartLevel.setInitialBundleStartLevel(KernelStartLevel.ACTIVE.getLevel());

        serviceTracker = new ServiceTracker<LibertyBootRuntime, LibertyBootRuntime>(context, LibertyBootRuntime.class, null);
        serviceTracker.open();
    }

    /**
     * Release services; should be called once provisioning operations are
     * completed.
     */
    protected void releaseServices() {
        context = null;
        frameworkStartLevel = null;
    }

    /**
     * Set the start level of the framework, and listen for framework events to
     * ensure we wait until the start level operation is complete before
     * continuing (due to timing, this translates into waiting until the next
     * start level event is fired.. we don't necessarily know that it's ours..).
     *
     * @param level
     *            StartLevel to change to
     * @return BundleStartStatus containing any exceptions encountered during
     *         the StartLevel change operation.
     */
    protected BundleStartStatus setFrameworkStartLevel(int level) {
        BundleStartStatus startStatus = null;

        if (frameworkStartLevel != null) {
            // The framework listener passed as a paramter will be notified with a
            // FrameworkEvent (START_LEVEL_CHANGED or ERROR) when the setStartLevel
            // operation completes.
            StartLevelFrameworkListener slfw = new StartLevelFrameworkListener(true);
            frameworkStartLevel.setStartLevel(level, slfw);
            slfw.waitForLevel();
            startStatus = slfw.getStatus();
        } else
            startStatus = new BundleStartStatus();

        return startStatus;
    }

    /**
     * Install framework bundles.
     *
     * @param bundleList
     *            Properties describing the bundles to install
     * @oaran config
     *        Bootstrap configuration containing information about
     *        initial configuration parameters and file locations
     * @return BundleInstallStatus containing details about the bundles
     *         installed, exceptions that occurred, bundles that couldn't be
     *         found, etc.
     */
    protected BundleInstallStatus installBundles(BootstrapConfig config) throws InvalidBundleContextException {
        BundleInstallStatus installStatus = new BundleInstallStatus();

        KernelResolver resolver = config.getKernelResolver();
        ContentBasedLocalBundleRepository repo = BundleRepositoryRegistry.getInstallBundleRepository();

        List<KernelBundleElement> bundleList = resolver.getKernelBundles();
        if (bundleList == null || bundleList.size() <= 0)
            return installStatus;

        boolean libertyBoot = Boolean.parseBoolean(config.get(BootstrapConstants.LIBERTY_BOOT_PROPERTY));
        for (final KernelBundleElement element : bundleList) {
            if (libertyBoot) {
                // For boot bundles the LibertyBootRuntime must be used to install the bundles
                installLibertBootBundle(element, installStatus);
            } else {
                installKernelBundle(element, installStatus, repo);
            }
        }

        return installStatus;
    }

    private void installKernelBundle(KernelBundleElement element, BundleInstallStatus installStatus, ContentBasedLocalBundleRepository repo) throws InvalidBundleContextException {
        // First, check/use previously stored Bundle path..
        File bundleFile = element.getCachedBestMatch();
        if (bundleFile == null || !bundleFile.exists()) {
            // If we didn't have a previous path, or it's no good, we need to look
            // for the bundle in the repository
            bundleFile = repo.selectBundle(element.getLocation(), element.getSymbolicName(),
                                           VersionUtility.stringToVersionRange(element.getRangeString()));
            element.setBestMatch(bundleFile);
        }

        if (bundleFile == null || !bundleFile.exists()) {
            // make note of bundle that could not be found
            installStatus.addMissingBundle(element.toNameVersionString());
        } else {
            // attempt to install bundle resource
            Bundle bundle;

            try {
                // Get URL from resource
                URL resourceURL = bundleFile.toURI().toURL();
                String urlString = resourceURL.toString();

                // Install this bundle as a "reference"-- this means that
                // the framework will not copy this bundle into it's private cache, it
                // will run from the actual jar (wherever it is).
                urlString = "reference:" + urlString;

                //Defect 44222: We were using the stored bundleID to check
                //for a previously-installed bundle matching that ID and throwing
                //an error if another bundle was found at that ID.  This restriction
                //has been relaxed and now we just install the bundle.
                bundle = context.installBundle(BUNDLE_LOC_KERNEL_TAG + urlString, new URL(urlString).openStream());

                setStartLevel(bundle, element, installStatus);
            } catch (IllegalStateException e) {
                // The framework is stopping: this is an expected but not ideal occurrence.
                e.getCause();
                throw (InvalidBundleContextException) new InvalidBundleContextException().initCause(e);
            } catch (Throwable e) {
                // We encountered an error installing a bundle, add it to
                // the status, and continue. The caller will handle as
                // appropriate
                installStatus.addInstallException(element.toNameVersionString(), e);
            }
        }
    }

    private void setStartLevel(Bundle bundle, KernelBundleElement element, BundleInstallStatus installStatus) throws InvalidBundleContextException {
        try {
            BundleRevision bRev = bundle.adapt(BundleRevision.class);
            // Add non-fragment bundles to the list of bundles to be
            // started and set startLevel
            if ((bRev.getTypes() & BundleRevision.TYPE_FRAGMENT) != BundleRevision.TYPE_FRAGMENT) {
                installStatus.addBundleToStart(bundle);
                BundleStartLevel bStartLevel = bundle.adapt(BundleStartLevel.class);

                int startLevel = element.getStartLevel();
                int currentLevel = bStartLevel.getStartLevel();

                // Change the start level if necessary
                if (currentLevel != startLevel) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Changing the start level of bundle {0} from {1} to the current level of {2}", bundle, currentLevel, startLevel);

                    bStartLevel.setStartLevel(startLevel);
                }
            }
        } catch (IllegalStateException e) {
            // The framework is stopping: this is an expected but not ideal occurrence.
            e.getCause();
            throw (InvalidBundleContextException) new InvalidBundleContextException().initCause(e);
        } catch (Throwable e) {
            // We encountered an error installing a bundle, add it to
            // the status, and continue. The caller will handle as
            // appropriate
            installStatus.addInstallException(element.toNameVersionString(), e);
        }
    }

    /**
     * @param element
     * @param installStatus
     * @throws InvalidBundleContextException
     */
    private void installLibertBootBundle(KernelBundleElement kernelBundle, BundleInstallStatus installStatus) throws InvalidBundleContextException {

        if (kernelBundle.getStartLevel() == KernelStartLevel.LIBERTY_BOOT.getLevel()) {
            // Ignore boot.jar bundles
            return;
        }

        //getting the LibertyBootRuntime service and installing boot bundle
        LibertyBootRuntime rt = serviceTracker.getService();
        if (rt == null) {
            throw new IllegalStateException("No LibertyBootRuntime service found!");
        }

        Bundle b = null;
        try {
            b = rt.installBootBundle(kernelBundle.getSymbolicName(), VersionUtility.stringToVersionRange(kernelBundle.getRangeString()), BUNDLE_LOC_KERNEL_TAG);
        } catch (IllegalStateException e) {
            // The framework is stopping: this is an expected but not ideal occurrence.
            e.getCause();
            throw (InvalidBundleContextException) new InvalidBundleContextException().initCause(e);
        } catch (Throwable e) {
            // We encountered an error installing a bundle, add it to
            // the status, and continue. The caller will handle as
            // appropriate
            installStatus.addInstallException(kernelBundle.toNameVersionString(), e);
        }
        if (b == null) {
            installStatus.addMissingBundle(kernelBundle.toNameVersionString());
        } else if (b.getBundleId() != 0) {
            setStartLevel(b, kernelBundle, installStatus);
        }

    }

    /**
     * Start all previously installed bundles, but defer (or not) the ACTIVATION
     * of those bundles based on the Bundle-ActivationPolicy value set in
     * MANIFEST.MF.
     *
     * @return BundleStartStatus object containing exceptions encountered while
     *         starting bundles
     * @see BundleStartStatus
     */
    protected BundleStartStatus startBundles(List<Bundle> installedBundles) {
        BundleStartStatus startStatus = new BundleStartStatus();

        if (installedBundles == null || installedBundles.size() == 0)
            return startStatus;

        for (Bundle b : installedBundles) {
            int state = b.getState();

            // Only start bundles that aren't UNINSTALLED, and haven't already
            // been started
            if (state == Bundle.UNINSTALLED || state >= org.osgi.framework.Bundle.STARTING)
                continue;

            try {
                b.start(Bundle.START_ACTIVATION_POLICY);
            } catch (BundleException e) {
                startStatus.addStartException(b, e);
            }
        }

        return startStatus;
    }
}