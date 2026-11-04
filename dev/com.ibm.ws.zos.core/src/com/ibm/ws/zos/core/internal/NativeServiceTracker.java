/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.zos.core.Angel;
import com.ibm.ws.zos.core.NativeClientService;
import com.ibm.ws.zos.core.NativeService;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * Component that is responsible for registering with the Angel and
 * populating the service registry with information about the authorized
 * native services.
 */
public class NativeServiceTracker implements BundleActivator {

    /**
     * Trace component used to issue messages.
     */
    private static final TraceComponent tc = Tr.register(NativeServiceTracker.class);

    /**
     * The maximum length of an angel name.
     */
    private static final int ANGEL_NAME_MAX_LENGTH = 54;

    /**
     * The location of the install root's {@code lib} directory. Native code
     * and bundles should be resolved relative to this directory.
     */
    static String WAS_LIB_DIR;

    /**
     * The location of the authorized function module relative to the install
     * root's {@code lib} directory.
     */
    final static String AUTHORIZED_FUNCTION_MODULE = "native/zos/s390x/bbgzsafm";

    /**
     * The location of the unauthorized function module relative to the install
     * root's {@code lib} directory.
     */
    final static String UNAUTHORIZED_FUNCTION_MODULE = "native/zos/s390x/bbgzsufm";

    /**
     * Used to look up the com.ibm.ws.zos.core.angelName=<NAMED_ANGEL> property
     */
    private static final String ANGEL_NAME_KEY = "com.ibm.ws.zos.core.angelName";

    /**
     * Used to look up the com.ibm.ws.zos.core.angelRequired=true|false property
     */
    private static final String ANGEL_REQUIRED_KEY = "com.ibm.ws.zos.core.angelRequired";

    /**
     * Used to look up the com.ibm.ws.zos.core.angelWaitTimeSeconds=<int WAIT_TIME> property
     */
    private static final String ANGEL_WAIT_TIME_KEY = "com.ibm.ws.zos.core.angelWaitTimeSeconds";

    /**
     * Used to set the max Angel waiting time if com.ibm.ws.zos.core.angelWaitTimeSeconds is invalid
     */
    private static final int ANGEL_STARTUP_WAIT_TIME_MAX = 300; // Shortern it for ServerWaitAbgel Fat test, but remember change this back to 300

    /**
     * The native method manager to use for bootstrapping native code.
     */
    final NativeMethodManager nativeMethodManager;

    /**
     * Variable registry for resolving bootstrap properties
     */
    final VariableRegistry variableRegistry;

    /**
     * The bundle context of the host bundle.
     */
    BundleContext bundleContext = null;

    /**
     * Indication of whether or not we successfully registered with the
     * angel.
     */
    boolean registeredWithAngel = false;

    /**
     * The set of server service registrations performed by this component. This field
     * must only be accessed by synchronized methods.
     */
    Set<ServiceRegistration<NativeService>> registrations = new HashSet<ServiceRegistration<NativeService>>();

    /**
     * The set of client service registrations performed by this component. This field
     * must only be accessed by synchronized methods.
     */
    Set<ServiceRegistration<NativeClientService>> clientRegistrations = new HashSet<ServiceRegistration<NativeClientService>>();

    /**
     * The Angel service that we've registered with OSGi. This field must only
     * be accessed by synchronized methods.
     */
    ServiceRegistration<Angel> angelRegistration = null;

    /**
     * Pattern used to determine if an angel name is valid.
     * The first "group" is a reluctant match against any characters.
     * The second "group" is a greedy match against any supported characters.
     * The third "group" is a reluctant match against any characters.
     * This should match anything, and if the first and third groups are empty, we have a valid angel name.
     */
    private final Pattern angelNamePattern = Pattern.compile("(.*?)([A-Z0-9\\!\\#\\$\\+\\-\\/\\:\\<\\>\\=\\?\\@\\[\\]\\^\\_\\`\\{\\}\\|\\~]*)(.*?)");

    /**
     * Helper class to hold return code data from native services.
     */
    final static class ServiceResults {
        final int returnValue;
        final int returnCode;
        final int reasonCode;

        ServiceResults(int returnValue, int returnCode, int reasonCode) {
            this.returnValue = returnValue;
            this.returnCode = returnCode;
            this.reasonCode = reasonCode;
        }
    }

    /**
     * Create a native service tracker.
     */
    public NativeServiceTracker(NativeMethodManager nativeMethodManager, VariableRegistry variableRegistry) {
        this.nativeMethodManager = nativeMethodManager;
        this.variableRegistry = variableRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext) throws BundleException {
        this.bundleContext = bundleContext;

        WAS_LIB_DIR = CoreBundleActivator.firstNotNull(bundleContext.getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR), "");

        // Register our own native code
        nativeMethodManager.registerNatives(NativeServiceTracker.class);

        // Print out LDA and MEMLIMIT information to the messages.log file
        getLDAInfo();
        getMemlimitInfo();

        // Load the unauthorized code to access the registration stub
        ServiceResults loadServiceResult = loadUnauthorized();
        if (loadServiceResult.returnValue != 0) {
            Tr.error(tc,
                     "UNABLE_TO_LOAD_UNAUTHORIZED_BPX4LOD",
                     UNAUTHORIZED_FUNCTION_MODULE,
                     loadServiceResult.returnValue,
                     Integer.toHexString(loadServiceResult.returnCode),
                     Integer.toHexString(loadServiceResult.reasonCode));
            throw new BundleException("Unable to load the z/OS unauthorized native library " + UNAUTHORIZED_FUNCTION_MODULE);
        }

        // Read angel name out of bootstrap property.  Not set means use the
        // default angel (null).
        String angelName = resolveVariable(ANGEL_NAME_KEY);

        // Validate the angel name.
        boolean angelNameValid = true;
        if (angelName != null) {
            if (angelName.trim().isEmpty()) {
                // Set the angel name to null to use the default angel.
                angelName = null;
            } else if (angelName.length() > ANGEL_NAME_MAX_LENGTH) {
                angelNameValid = false;
                Tr.error(tc, "ANGEL_NAME_TOO_LONG");
            } else {
                Matcher m = angelNamePattern.matcher(angelName);
                if (m.matches()) {
                    // Go see if the first or last part of the matcher are empty.  If they are empty,
                    // we're good.  If they are not empty, we found some invalid characters that we
                    // need to report.
                    int badCharOffset = -1;
                    char badChar = ' ';
                    String badGroup = m.group(1); // Get the beginning non-matching part
                    if ((badGroup != null) && (badGroup.length() > 0)) {
                        badCharOffset = m.end(1) - 1;
                    } else {
                        badGroup = m.group(3); // Get the end non-matching part
                        if ((badGroup != null) && (badGroup.length() > 0)) {
                            badCharOffset = m.start(3);
                        }
                    }

                    if (badCharOffset >= 0) {
                        angelNameValid = false;
                        badChar = angelName.charAt(badCharOffset);
                        Tr.error(tc, "ANGEL_NAME_UNSUPPORTED_CHARACTER", new Object[] { badChar, badCharOffset });
                    }
                } else {
                    // The matcher is written to handle just about anything.  If we couldn't parse
                    // it, throw and spit the whole name out in the exception.
                    throw new IllegalArgumentException("Could not parse angel name " + angelName);
                }
            }
        }

        // Angel status flag
        int registerReturnCode = -1;
        if (angelNameValid == true) {

            // if the server requires angel to run, add wait time
            if (angelRequired()) {
                Tr.debug(tc, "ANGEL IS REQUIRED TO RUN THE LIBERTY SERVER.");

                int maxWaitTime = getAngelWaitTime();

                registerReturnCode = -1;

                int waitTimeCounter = 0;
                ////////////////////////////////////////////////////////////////////////////////////
                //  Checking Angel starting status for maxWaitTime seconds
                ////////////////////////////////////////////////////////////////////////////////////
                Tr.info(tc, "REQUIRED_ANGEL_WAITTIME_NAME_PROMPT", maxWaitTime, ((angelName == null) ? "default" : angelName));
                while (waitTimeCounter <= maxWaitTime) {
                    // Attempt to register with the angel
                    registerReturnCode = registerServer(angelName);
                    waitTimeCounter++;
                    if (registerReturnCode == 0) {
                        // Angel is up
                        break;
                    } else if (1 <= registerReturnCode && registerReturnCode < 256) {
                        // Angel hasn't start yet, keep loop running (waiting)
                    } else {
                        Tr.event(tc, "Angel is not up, but exception code received, stop waiting.");
                        break;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Tr.debug(tc, "Thread pause failure, stop waiting Angel and continue starting server.");
                        break;
                    }
                }

                Tr.event(tc, "Total " + waitTimeCounter + " seconds were used to register with required Angel.");

                // Done checking Angel status
                if (registerReturnCode == 0) { // if Angel up
                    registeredWithAngel = true;
                    Tr.info(tc, "SERVER_CONNECTED_TO_ANGEL", ((angelName == null) ? "default" : angelName));
                } else {
                    registeredWithAngel = false;
                    switch (registerReturnCode) {
                        case NativeReturnCodes.ANGEL_REGISTER_DRM_NOT_AUTHORIZED:
                            if (null == angelName) {
                                Tr.info(tc, "SERVER_NOT_AUTHORIZED_TO_CONNECT_TO_ANGEL");
                            } else {
                                Tr.info(tc, "SERVER_NOT_AUTHORIZED_TO_CONNECT_TO_ANGEL_NAME", ((angelName == null) ? "default" : angelName));
                            }
                            break;
                        case NativeReturnCodes.ANGEL_REGISTER_DRM_SAFM_NOT_APF_AUTHORIZED:
                            Tr.info(tc, "SERVER_SAFM_NOT_APF_AUTHORIZED");
                            break;
                        case NativeReturnCodes.ANGEL_REGISTER_DRM_NOT_AUTHORIZED_BBGZSAFM:
                            if (null == angelName) {
                                Tr.info(tc, "ANGEL_NOT_AVAILABLE", registerReturnCode);
                            } else {
                                Tr.info(tc, "ANGEL_NOT_AVAILABLE_NAME", ((angelName == null) ? "default" : angelName), registerReturnCode);
                            }
                            Tr.info(tc, "SERVER_SAFM_NOT_SAF_AUTHORIZED");
                            break;
                        default:
                            if (registerReturnCode < 0) {
                                Tr.event(tc, "Angel registration failed, status code " + registerReturnCode);
                                if (registerReturnCode == -1) {
                                    Tr.event(tc, "Time out to register with required Angel " + ((angelName == null) ? "default" : angelName));
                                }
                            }
                            if (null == angelName) {
                                Tr.info(tc, "ANGEL_NOT_AVAILABLE", registerReturnCode);
                            } else {
                                Tr.info(tc, "ANGEL_NOT_AVAILABLE_NAME", ((angelName == null) ? "default" : angelName), registerReturnCode);
                            }
                            break;
                    }
                }
            } else {
                // if server does not require Angel but gave an Angel name, try to register once

                registeredWithAngel = false; // fail safe reset

                // Attempt to register with the angel
                registerReturnCode = registerServer(angelName);

                if (registerReturnCode == 0) {
                    registeredWithAngel = true;
                    Tr.info(tc, "SERVER_CONNECTED_TO_ANGEL", ((angelName == null) ? "default" : angelName));
                } else if (registerReturnCode == NativeReturnCodes.ANGEL_REGISTER_DRM_NOT_AUTHORIZED) {
                    if (null == angelName) {
                        Tr.info(tc, "SERVER_NOT_AUTHORIZED_TO_CONNECT_TO_ANGEL");
                    } else {
                        Tr.info(tc, "SERVER_NOT_AUTHORIZED_TO_CONNECT_TO_ANGEL_NAME", ((angelName == null) ? "default" : angelName));
                    }
                } else if (registerReturnCode == NativeReturnCodes.ANGEL_REGISTER_DRM_SAFM_NOT_APF_AUTHORIZED) {
                    Tr.info(tc, "SERVER_SAFM_NOT_APF_AUTHORIZED");
                } else if (registerReturnCode == NativeReturnCodes.ANGEL_REGISTER_DRM_NOT_AUTHORIZED_BBGZSAFM) {
                    if (null == angelName) {
                        Tr.info(tc, "ANGEL_NOT_AVAILABLE", registerReturnCode);
                    } else {
                        Tr.info(tc, "ANGEL_NOT_AVAILABLE_NAME", ((angelName == null) ? "default" : angelName), registerReturnCode);
                    }
                    Tr.info(tc, "SERVER_SAFM_NOT_SAF_AUTHORIZED");
                } else {
                    if (null == angelName) {
                        Tr.info(tc, "ANGEL_NOT_AVAILABLE", registerReturnCode);
                    } else {
                        Tr.info(tc, "ANGEL_NOT_AVAILABLE_NAME", ((angelName == null) ? "default" : angelName), registerReturnCode);
                    }

                    if (registerReturnCode < 0) {
                        Tr.event(tc, "Angel registration failed, status code " + registerReturnCode);
                        if (registerReturnCode == -1) {
                            Tr.event(tc, "Time out to register with required Angel " + ((angelName == null) ? "default" : angelName));
                        }
                    }
                }
            }
        }

        // Populate the OSGi service registry
        // Does not depend on Angel up or down
        populateServiceRegistry(angelName);
        Tr.event(tc, "Finished populate OSGi service.");

        checkAngelRequirement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext bundleContext) {
        // Remove native service representations from service registry
        unregisterOSGiServices();

        // Attempt to deregister from the angel
        if (registeredWithAngel) {
            deregisterServer();
            registeredWithAngel = false;
        }
    }

    /**
     * Check the configuration to determine whether this server
     * is allowed to start without the angel and take it down
     * if the angel is required and not connected
     */
    private void checkAngelRequirement() {
        // Angel is not registered but the Server requires Angel
        if (!registeredWithAngel && angelRequired()) {
            Tr.info(tc, "NOT_REGISTERED_WITH_REQUIRED_ANGEL");
            shutdownFramework();
        }
    }

    /**
     * Check the configuration to determine whether this server
     * is requiring angel, return boolean
     */
    private boolean angelRequired() {
        // Boolean.getBoolean() returns false by default if it
        // can't find a value for the given key. This is fine
        // because our property should default to false anyway.
        String isAngelReqStr = resolveVariable(ANGEL_REQUIRED_KEY);
        isAngelReqStr = (null == isAngelReqStr) ? "false" : isAngelReqStr.trim().toLowerCase();
        boolean isAngelRequired = Boolean.parseBoolean(isAngelReqStr);
        return isAngelRequired;
    }

    /**
     * Return (int) max waitting time for Angel to be started.
     *
     * The maximum for this value should be 300 seconds,
     * and if the value is less than zero or greater than 300,
     * we should issue a message saying that the value is out of range
     * and we are using the maximum value of 300 seconds instead.
     * The default value if nothing is specified should be zero seconds.
     */
    private int getAngelWaitTime() {
        int returnWaitTime = 0; // use 0 by default

        try {
            String waitTimeIntStr = resolveVariable(ANGEL_WAIT_TIME_KEY) != null ? resolveVariable(ANGEL_WAIT_TIME_KEY) : "0";

            returnWaitTime = Integer.parseInt(waitTimeIntStr); // parse startup time to int value

            Tr.debug(tc, "Preset Angel startup wait time is " + returnWaitTime + "s.");

            if (returnWaitTime < 0 || returnWaitTime > ANGEL_STARTUP_WAIT_TIME_MAX) { // if the time is less than 0s or greater than 300s
                Tr.warning(tc, "ANGEL_WAIT_TIME_OUT_OF_RANGE_OR_INVLID");
                // keep default min
                returnWaitTime = 0;
            }

        } catch (Exception e) { // input wait time can't be parse to int, keep default min
            returnWaitTime = ANGEL_STARTUP_WAIT_TIME_MAX;
            Tr.warning(tc, "ANGEL_WAIT_TIME_OUT_OF_RANGE_OR_INVLID");
        }

        return returnWaitTime;
    }

    /**
     * This method is used to stop the root bundle
     * thus bringing down the OSGi framework.
     */
    @FFDCIgnore(Exception.class)
    final void shutdownFramework() {

        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
            // do not FFDC this.
            // exceptions during bundle stop occur if framework is already stopping or stopped
        }
    }

    /**
     * Test if the specified file exists using this class's security context.
     *
     * @param file the file to test
     *
     * @return true if the file exists
     */
    boolean fileExists(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    /**
     * Load the module containing the unauthorized native code.
     *
     * @return the &quot;load HFS&quot; return codes as a <code>ServiceResults</code>.
     */
    private ServiceResults loadUnauthorized() {
        File library = new File(WAS_LIB_DIR, UNAUTHORIZED_FUNCTION_MODULE);
        if (!fileExists(library)) {
            Tr.error(tc, "LIBRARY_DOES_NOT_EXIST", library.getAbsolutePath());
        }
        return ntv_loadUnauthorized(library.getAbsolutePath());
    }

    /**
     * Attempt to register this server with the angel and access the authorized
     * code infrastructure.
     *
     * @param angelName The angel name we want to connect to.
     *
     * @return the return code from server registration
     */
    private int registerServer(String angelName) {
        File library = new File(WAS_LIB_DIR, AUTHORIZED_FUNCTION_MODULE);
        if (!fileExists(library)) {
            Tr.error(tc, "LIBRARY_DOES_NOT_EXIST", library.getAbsolutePath());
        }

        return ntv_registerServer(library.getAbsolutePath(), angelName);
    }

    /**
     * Deregister this server and tear down the authorized code infrastructure.
     *
     * @return the deregistration return code
     */
    private int deregisterServer() {
        return ntv_deregisterServer();
    }

    /**
     * Prints the LDA information to the messages.log file on server startup
     *
     */
    private void getLDAInfo() {

        // Try to get the LDA information
        try {

            // The information is built into a string separated by the pipe character
            String[] ldaInfo = nvt_getLDAInformation().split("\\|");

            // The region size is calculated in KB before being passed up from C code
            String regionSizeString = ldaInfo[0] + "KB";

            // The above/below line information is calculated in MB before being passed from C code
            String aboveLineString = ldaInfo[1] + "MB";
            String belowLineString = ldaInfo[2] + "MB";

            // Show a message with the LDA information
            Tr.info(tc, "REGION_REQUESTED", new Object[] { regionSizeString, aboveLineString, belowLineString });
        } catch (Throwable t) {
            // Catching so we get an FFDC with the error in it
        }
    }

    /**
     * Prints the MEMLIMIT info to the messages.log file on server startup
     */
    private void getMemlimitInfo() {

        // Try to get the MEMLIMIT information
        try {
            // Expecting info as a string seperated by the pipe character
            String[] memInfo = ntv_getMemlimitInformation().split("\\|");

            // Create an array filled with each possible source mapped to the corresponiding int that will be returned..
            // Example memInfo[1] returning 1 = SMF and memInfo[1] returning 2 = JCL
            String[] configurationSourceLookup = { "BAD", "SMF", "JCL", "REG0", "USI", "OMVS", "SETR", "SPW", "SETO", "AUTH", "URG" };

            // Lookup the configuration source
            String configurationSource = configurationSourceLookup[Integer.parseInt(memInfo[1])];

            // Build a message for the user based on the information from the native C method
            Tr.info(tc, "MEMORY_LIMIT_INFORMATION", new Object[] { memInfo[0], configurationSource });
        } catch (Throwable t) {
            // Catching se we get an FFDC with the error in it
        }
    }

    /**
     * Populate the OSGi service registry with information about the native
     * services from the service vector table.
     */
    synchronized void populateServiceRegistry(String angelName) {
        List<String> permittedServices = new ArrayList<String>();
        List<String> deniedServices = new ArrayList<String>();
        List<String> permittedClientServices = new ArrayList<String>();
        List<String> deniedClientServices = new ArrayList<String>();

        Set<String> permittedProfiles = new TreeSet<String>();
        Set<String> deniedProfiles = new TreeSet<String>();
        Set<String> permittedClientProfiles = new TreeSet<String>();
        Set<String> deniedClientProfiles = new TreeSet<String>();

        getNativeServiceEntries(permittedServices, deniedServices, permittedClientServices, deniedClientServices);

        for (int i = 0; i < permittedServices.size(); i += 2) {
            registerOSGiService(permittedServices.get(i), permittedServices.get(i + 1), true, false);
            permittedProfiles.add(permittedServices.get(i + 1));
        }

        for (int i = 0; i < deniedServices.size(); i += 2) {
            registerOSGiService(deniedServices.get(i), deniedServices.get(i + 1), false, false);
            deniedProfiles.add(deniedServices.get(i + 1));
        }

        for (int i = 0; i < permittedClientServices.size(); i += 2) {
            registerOSGiService(permittedClientServices.get(i), permittedClientServices.get(i + 1), true, true);
            permittedClientProfiles.add(permittedClientServices.get(i + 1));
        }

        for (int i = 0; i < deniedClientServices.size(); i += 2) {
            registerOSGiService(deniedClientServices.get(i), deniedClientServices.get(i + 1), false, true);
            deniedClientProfiles.add(deniedClientServices.get(i + 1));
        }

        for (String profile : permittedProfiles) {
            Tr.info(tc, "AUTHORIZED_SERVICE_AVAILABLE", profile);
        }

        for (String profile : deniedProfiles) {
            Tr.info(tc, "AUTHORIZED_SERVICE_NOT_AVAILABLE", profile);
        }

        for (String profile : permittedClientProfiles) {
            Tr.info(tc, "AUTHORIZED_SERVICE_AVAILABLE", "CLIENT." + profile);
        }

        for (String profile : deniedClientProfiles) {
            Tr.info(tc, "AUTHORIZED_SERVICE_NOT_AVAILABLE", "CLIENT." + profile);
        }

        if (registeredWithAngel) {
            int angelVersion = ntv_getAngelVersion();
            //Gets the server's angel version to compare against the current version
            int expectedAngelVersion = ntv_getExpectedAngelVersion();
            if (angelVersion != -1) {
                if (expectedAngelVersion > angelVersion) {
                    //issues a warning if the current version is older than the server version
                    Tr.warning(tc, "OLD_ANGEL_VERSION");
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Current angel version is " + angelVersion + " and expected angel version is " + expectedAngelVersion + ".");
                    }
                }
                Angel angel = new AngelImpl(ntv_getAngelVersion(), angelName);
                Dictionary<String, String> properties = new Hashtable<String, String>();
                properties.put(Constants.SERVICE_VENDOR, "IBM");
                properties.put(Angel.ANGEL_DRM_VERSION, Integer.toString(angel.getDRM_Version()));
                if (angelName != null) {
                    properties.put(Angel.ANGEL_NAME, angelName);
                }

                angelRegistration = bundleContext.registerService(Angel.class, angel, properties);
            }
        }
    }

    /**
     * Register a {@code NativeService} representation with the specified
     * name and indicate whether or not this server is authorized to use it.
     *
     * @param name               the service name from the services vector table
     * @param authorizationGroup the name of the SAF authorization group that
     *                               controls access to the authorized service
     * @param isAuthorized       indication of whether or not this server can use the
     *                               specified service
     * @param client             indication of whether this is a service that the client calls,
     *                               or the server calls (BBGZSCFM vs BBGZSAFM)
     */
    synchronized void registerOSGiService(String name, String authorizationGroup, boolean isAuthorized, boolean client) {
        NativeClientService service = new NativeServiceImpl(name.trim(), authorizationGroup.trim(), isAuthorized, client);
        Dictionary<String, String> properties = new Hashtable<String, String>();

        properties.put(Constants.SERVICE_VENDOR, "IBM");
        properties.put(NativeService.NATIVE_SERVICE_NAME, service.getServiceName());
        properties.put(NativeService.AUTHORIZATION_GROUP_NAME, service.getAuthorizationGroup());
        properties.put(NativeService.IS_AUTHORIZED, Boolean.toString(service.isPermitted()));

        if (client) {
            clientRegistrations.add(bundleContext.registerService(NativeClientService.class, service, properties));
        } else {
            registrations.add(bundleContext.registerService(NativeService.class, service, properties));
        }
    }

    /**
     * Unregister all {@code NativeService} representations from the OSGi
     * service registry.
     */
    synchronized void unregisterOSGiServices() {
        for (ServiceRegistration<NativeService> service : registrations) {
            service.unregister();
        }
        for (ServiceRegistration<NativeClientService> service : clientRegistrations) {
            service.unregister();
        }

        registrations.clear();
        clientRegistrations.clear();

        if (angelRegistration != null) {
            angelRegistration.unregister();
            angelRegistration = null;
        }
    }

    /**
     * Resolves a variable from the variable registry.
     *
     * @param variable the variable to resolve
     *
     * @return the resolved variable, or null if the variable was not defined
     */
    private String resolveVariable(String variableName) {
        // the behavior of variableRegistry.resolveRawString() is to leave undefined variables unresolved in the returned
        // string...  this method converts undefined variables to a null return, which is easier for callers to deal with
        String unresolvedVariable = "${" + variableName + "}";
        String resolvedVariable = variableRegistry.resolveRawString(unresolvedVariable);
        if (unresolvedVariable.equals(resolvedVariable)) {
            return null;
        } else {
            return resolvedVariable;
        }
    }

    /**
     * Get information about the native services in the authorized load module.
     *
     * @param permittedServices       the list to populate with permitted service names.
     *                                    Each permitted service uses two entries in the list where the first
     *                                    entry is the name of the service and the next entry is the name of
     *                                    the authorization group.
     * @param deniedServices          the list to populate with denied service names
     *                                    Each denied service uses two entries in the list where the first
     *                                    entry is the name of the service and the next entry is the name of
     *                                    the authorization group.
     * @param permittedClientServices the list to populate with permitted service names.
     *                                    Each permitted service uses two entries in the list where the first
     *                                    entry is the name of the service and the next entry is the name of
     *                                    the authorization group.
     * @param deniedClientServices    the list to populate with denied service names
     *                                    Each denied service uses two entries in the list where the first
     *                                    entry is the name of the service and the next entry is the name of
     *                                    the authorization group.
     *
     * @return the number of combined entries in the services lists.
     */
    int getNativeServiceEntries(List<String> permittedServices, List<String> deniedServices, List<String> permittedClientServices, List<String> deniedClientServices) {
        return ntv_getNativeServiceEntries(permittedServices, deniedServices, permittedClientServices, deniedClientServices);
    }

    protected native ServiceResults ntv_loadUnauthorized(String unauthorizedModulePath);

    protected native int ntv_registerServer(String authorizedModulePath, String angelName);

    protected native int ntv_deregisterServer();

    protected native int ntv_getNativeServiceEntries(List<String> permittedServices, List<String> deniedServices, List<String> permittedClientServices,
                                                     List<String> deniedClientServices);

    protected native int ntv_getAngelVersion();

    protected native int ntv_getExpectedAngelVersion();

    protected native String nvt_getLDAInformation();

    protected native String ntv_getMemlimitInformation();

}
