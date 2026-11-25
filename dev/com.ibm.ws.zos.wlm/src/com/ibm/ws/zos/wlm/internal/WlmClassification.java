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
package com.ibm.ws.zos.wlm.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.EnclaveManager;
import com.ibm.ws.zos.wlm.ejb.mdb.MDBClassifier;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.WorkClassifier;

/**
 * z/OS WLM WorkClassifier provides an execution environment by using WLM Enclaves. The
 * WLM Enclaves are created with information returned from classifying the request.
 */
public class WlmClassification implements WorkClassifier, ManagedServiceFactory {

    private static final TraceComponent tc = Tr.register(WlmClassification.class);

    EnclaveManager enclaveManager;
    ExecutorService executorService;

    List<ClassificationData> allClassificationsList;
    ConcurrentHashMap<Integer, List<ClassificationData>> classificationDataMapByPorts;
    String currentPid;

    /** ConfigurationAdmin reference. */
    private ConfigurationAdmin configAdmin;

    /** MDB Classifier. */
    private MDBClassifier mdbClassifier;

    /** Native Utility object reference. */
    private NativeUtils nativeUtils;

    /** Cache of the last set of configuration properties. */
    @SuppressWarnings("rawtypes")
    private Dictionary lastConfigProperties;

    public WlmClassification() {
        configAdmin = null;
    }

    protected void setConfigurationAdmin(ConfigurationAdmin ca) {
        configAdmin = ca;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin ca) {
        if (ca == configAdmin) {
            configAdmin = null;
        }
    }

    protected void setWlmEnclaveManager(EnclaveManager enclaveManager) {
        this.enclaveManager = enclaveManager;
    }

    protected void unsetWlmEnclaveManager(EnclaveManager enclaveManager) {
        if (this.enclaveManager == enclaveManager) {
            this.enclaveManager = null;
        }
    }

    protected void setMdbClassifier(MDBClassifier mdbClassifier) {
        this.mdbClassifier = mdbClassifier;

        // If the service reactivated with no wlmClassification update, make sure
        // that the MDB classifier receives the last known config.
        if (lastConfigProperties != null) {
            mdbClassifier.update(lastConfigProperties);
        }
    }

    protected void unsetMdbClassifier(MDBClassifier mdbClassifier) {
        if (this.mdbClassifier == mdbClassifier) {
            this.mdbClassifier = null;
        }
    }

    protected void activate(BundleContext context, Map<String, Object> properties) {
        allClassificationsList = new LinkedList<ClassificationData>();
        classificationDataMapByPorts = new ConcurrentHashMap<Integer, List<ClassificationData>>();
    }

    protected void deactivate(int reason) {
    }

    protected byte[] classifyHTTP(String host, Integer port, String resource, String method) {
        List<ClassificationData> matchesPortAndMethod = filterPortAndMethod(port, method);
        List<ClassificationData> matchesHost = filterByHost(matchesPortAndMethod, host);
        byte[] returnClass = filterByResource(matchesHost, resource);

        return returnClass;
    }

    static protected boolean isValidTransactionClass(byte[] inTranClass) {
        // May add more checking later
        return (inTranClass != null);
    }

    private List<ClassificationData> filterPortAndMethod(Integer port, String method) {
        List<ClassificationData> matchesPort;

        /*
         * Index into the portMap to find all matching ports. If the result isn't null, skip
         * through the sync block. If the result is null, this is the first pass and the
         * cache hasn't been created... enter the sync block.
         */
        matchesPort = classificationDataMapByPorts.get(port);

        if (matchesPort == null) {
            synchronized (allClassificationsList) {
                /*
                 * Inside the synchronized block another get(port)/null check should be done.
                 * It is possible that while we were blocked, the request that was blocking us was
                 * building the cache we need.
                 */
                matchesPort = classificationDataMapByPorts.get(port);

                if (matchesPort == null) {
                    List<ClassificationData> mappedList = new LinkedList<ClassificationData>();

                    for (ClassificationData current : allClassificationsList) {
                        if (current.matchesPort(port))
                            mappedList.add(current);
                    }

                    // Add the newly created List of matching classification rules to the port map
                    classificationDataMapByPorts.put(port, mappedList);

                    // For use in the rest of this method, fill in the matching ports variable
                    matchesPort = mappedList;
                }
            }
        }

        /*
         * At this point we have the List<ClassificationData> that matches port.
         * This List needs to be filtered through to find the entries which match the method
         */
        List<ClassificationData> returnList = new LinkedList<ClassificationData>();

        for (ClassificationData current : matchesPort) {
            if (current.matchesMethod(method))
                returnList.add(current);
        }

        // The returnList now contains only ClassificationData objects which match both port and method
        return returnList;
    }

    private List<ClassificationData> filterByHost(List<ClassificationData> incomingList, String host) {
        List<ClassificationData> returnList = new LinkedList<ClassificationData>();

        for (ClassificationData current : incomingList) {
            if (current.matchesHost(host)) {
                returnList.add(current);
            }
        }

        return returnList;
    }

    private byte[] filterByResource(List<ClassificationData> incomingList, String resource) {
        for (ClassificationData current : incomingList) {
            if (current.matchesResource(resource))
                return current.getTransactionClassEBCDIC();
        }

        return null;
    }

    /**
     * DS method for setting the ExecutorService reference.
     *
     * @param service
     */
    protected void setExecutorService(ExecutorService service) {
        executorService = service;
    }

    /**
     * DS method for removing the ExecutorService reference.
     *
     * @param service
     */
    protected void unsetExecutorService(ExecutorService service) {
        if (executorService == service)
            executorService = null;
    }

    /**
     * Access the collaboration engine.
     *
     * @return CollaborationEngine - null if not found
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected EnclaveManager getEnclaveManager() {
        return enclaveManager;
    }

    /** {@inheritDoc} */
    @Override
    public Executor classify(HttpRequest request, HttpInboundConnection inboundConnection) {
        String host;
        int port;
        String uri;
        String method;
        Executor executor = null;

        host = request.getVirtualHost();
        port = request.getVirtualPort();
        uri = request.getURI();
        method = request.getMethod();

        byte[] classificationClass = classifyHTTP(host, port, uri, method);

        // Return skip WLM Enclave services if classifies returns an invalid Transaction Class and the
        // native utility object reference is null.
        // It Indicates that for this request its "disabled".
        if (isValidTransactionClass(classificationClass) && nativeUtils != null) {
            long arrivalTime = nativeUtils.getSTCK();
            executor = new WlmExecutor(this.getEnclaveManager(), classificationClass, arrivalTime, host, port, uri, method);
        }

        return executor;
    }

    /**
     * Remember the enclave manager
     *
     * @param em the enclave manager
     */
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Forget the enclave manager (if we had remembered it)
     *
     * @param em The enclave manager to forget
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    class WlmExecutor implements Executor {
        EnclaveManager enclaveManager = null;
        byte[] tranClass = null;
        long originalArrivalTime = 0;
        long arrivalTime = 0;
        String host = "";
        int port = 0;
        String uri = "";
        String method = "";

        WlmExecutor(EnclaveManager enclaveManager, byte[] tranClass, long arrivalTime,
                    String host, int port, String uri, String method) {
            this.enclaveManager = enclaveManager;
            this.tranClass = tranClass;
            this.arrivalTime = arrivalTime;
            this.host = host;
            this.port = port;
            this.uri = uri;
            this.method = method;
        }

        @Override
        public void execute(Runnable work) {
            HashMap<String, Object> wlmData = new HashMap<String, Object>();
            Enclave enclave = this.wlmPreInvoke(wlmData);
            wlmData.put(WLMNativeServices.WLM_DATA_TRAN_CLASS, tranClass);
            wlmData.put(WLMNativeServices.WLM_DATA_ARRIVAL_TIME, Long.valueOf(arrivalTime));
            wlmData.put(WLMNativeServices.WLM_DATA_HOST, host);
            wlmData.put(WLMNativeServices.WLM_DATA_PORT, Integer.valueOf(port));
            wlmData.put(WLMNativeServices.WLM_DATA_URI, uri);

            // Mark this Executor as used.  To allow it to be re-used for subsequent dispatches (WebSocket requests).
            this.setExecutorUsed();

            wlmRunWork(wlmData, work, enclave);
        }

        /*
         * Note:
         * This method is instrumented. If you change its signature, you need to update
         * descOfMethod in WlmRunWorkTransformDescriptor.java.
         */
        private void wlmRunWork(HashMap<String, Object> wlmData, Runnable work, Enclave enclave) {
            try {
                work.run();
            } finally {
                wlmPostInvoke(enclave);
            }
        }

        private Enclave wlmPreInvoke(HashMap<String, Object> wlmData) {

            Enclave enclave = null;
            if (null != enclaveManager) {
                if (arrivalTime == 0) {
                    arrivalTime = Long.valueOf(nativeUtils.getSTCK());
                }

                enclave = enclaveManager.joinNewEnclave(this.tranClass, arrivalTime);
                wlmData.put(WLMNativeServices.WLM_DATA_ENCLAVE_TOKEN, enclaveManager.getCurrentEnclave().getStringToken());
            }

            return enclave;
        }

        private void wlmPostInvoke(Enclave enclave) {
            // Leave WLM Enclave and destroy it.
            // Note: leave drives delete
            if ((null != enclaveManager) && (null != enclave)) {
                enclaveManager.leaveEnclave(enclave);
            }
        }

        /**
         * Mark this Executor as used to allow it be re-used for subsequent dispatches.
         */
        public void setExecutorUsed() {
            // Clear arrival time so current time will be used if this Executor is re-used (ex. WebSocket request flows
            // may propagate this Executor to use for WebSocket requests).
            if (originalArrivalTime == 0) {
                originalArrivalTime = arrivalTime;
            }

            arrivalTime = 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleted(String pid) {
        // Check if the incoming pid is the one we know about
        if (currentPid.equals(pid)) {
            /*
             * This means that our entire classification has gone away.
             * We should clear out all relevant data.
             */
            synchronized (allClassificationsList) {
                classificationDataMapByPorts.clear();

                allClassificationsList.clear();
            }

            if (mdbClassifier != null) {
                mdbClassifier.cleanup();
            }

            currentPid = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "WLM Http Work Classifier";
    }

    /** {@inheritDoc} */
    @Override
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        if (pid.equals(currentPid) || currentPid == null) {
            currentPid = pid;
            lastConfigProperties = properties;

            // Perform any mdbClassification updates if any.
            if (mdbClassifier != null) {
                mdbClassifier.update(properties);
            }

            /*
             * We're being updated. The incoming list is all of the classifications,
             * not just a single new one. Becuase of this we need to recreate the entire master list.
             */
            List<ClassificationData> tempAllClassificationsList = new LinkedList<ClassificationData>();
            try {
                // Pull the list of httpClassifications out of the properties
                String[] classifications = (String[]) properties.get("httpClassification");

                // Step through each httpclassification element
                if (configAdmin != null && classifications != null) {
                    classificationRule: for (int i = 0; i < classifications.length; i++) {
                        // Pull out an individual classification and its related properties
                        Configuration config = configAdmin.getConfiguration(classifications[i], null);
                        Dictionary<?, ?> prop = config.getProperties();

                        // Pull relevant data out of this configuration
                        String tclass = (String) prop.get("transactionClass");
                        String host = (String) prop.get("host");
                        String port = (String) prop.get("port");
                        String method = (String) prop.get("method");
                        String resource = (String) prop.get("resource");

                        // Check the length of the transactionClass. If it's too long, truncate it to the max length
                        if (tclass.length() > WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH) {
                            String originalTclass = tclass;
                            tclass = tclass.substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH);
                            Tr.warning(tc, "CLASSIFICATION_TRANCLASS_TRUNCATED", new Object[] { originalTclass, tclass });
                        }

                        /*
                         * Check the resource name for wildcards. Valid wildcarding involves either a single "*" or
                         * two ("**"). Any combination of wildcards more than two in a row is an invalid configuration.
                         * If we see this, we shouldn't add the rule to the list. This will guarantee that only "*" and "**"
                         * are possible strings of wildcards in our resource name by the time it gets to being stored.
                         */
                        if (resource.contains("***")) {
                            // Issue a message to the user to inform them the current rule is being dropped
                            Tr.warning(tc, "CLASSIFICATION_DROPPED_TOO_MANY_WILDCARDS", new Object[] { resource });
                            continue;
                        }

                        /*
                         * The resource passed to us may have wildcards in it. If it does, we should parse
                         * through it and turn the faux-regex of the server.xml wildcarding into a real
                         * regular expression.
                         */
                        if (resource.contains("*") && !resource.equals("*")) {
                            // Split the resource up, splitting on the string "/**/"
                            String[] splitResource = resource.split("/\\*\\*/");
                            String resourceAsRegex = "";

                            // If there are still any "**" strings left, the user has provided an invalid resource. Drop the rule.
                            for (String current : splitResource) {
                                if (current.contains("**")) {
                                    // Issue a message to the user to inform them the current rule is being dropped
                                    Tr.warning(tc, "CLASSIFICATION_DROPPED_BAD_WILDCARD", new Object[] { resource });
                                    continue classificationRule;
                                }
                            }

                            // Iterate over each split up piece of the resource, replacing each "*" with a regex.
                            StringBuffer buffer = new StringBuffer();
                            for (String current : splitResource) {
                                // Replace all "*"'s with a regex and concatenate it to the final resource
                                String currentAsRegex = current.replaceAll("\\*", "[^/]\\*");
                                buffer.append(currentAsRegex);

                                // Add the regex replacing "/**/" between each of the pieces
                                buffer.append("(/.*)?/");
                            }
                            resourceAsRegex = buffer.toString();

                            // The previous loop will leave a trailing "(/.*)?/" on the string that shouldn't be there: clean it up.
                            resourceAsRegex = resourceAsRegex.substring(0, resourceAsRegex.length() - 7);

                            // Finally, switch the resource in storage to the regex'd version
                            resource = resourceAsRegex;
                        }

                        /*
                         * Both port and method are allowed to be a String which contains a comma separated list.
                         * All incoming ports and methods should be preprocessed to remove these commas and turn
                         * them into HashSets for use in the ClassificationData Object.
                         */
                        HashSet<String> portSetCommasRemoved = removeCommas(port);
                        HashSet<Integer> portSet = parsePorts(portSetCommasRemoved);
                        HashSet<String> methodSet = removeCommas(method);

                        // New up a Classification Data entry and add it to the list
                        ClassificationData newEntry = new ClassificationData(tclass, host, portSet, resource, methodSet);
                        tempAllClassificationsList.add(newEntry);
                    }
                }
            } catch (IOException ioe) {
                throw new ConfigurationException(pid, pid, ioe);
            }

            synchronized (allClassificationsList) {
                // Can't just assign to allClassificationList here since that will mess up the
                // synchronization, so we copy it across into the existing list
                allClassificationsList.clear();
                allClassificationsList.addAll(tempAllClassificationsList);
                // Because we've updated the master list of rules, the cached map is now out of date. It needs to be cleared.
                classificationDataMapByPorts.clear();
            }
        }
    }

    private HashSet<String> removeCommas(String input) {
        String[] allElements = input.split(",");
        HashSet<String> returnSet = new HashSet<String>();

        for (String element : allElements) {
            if (!element.equals(""))
                returnSet.add(element);
        }

        return returnSet;
    }

    private HashSet<Integer> parsePorts(HashSet<String> input) {
        HashSet<Integer> returnSet = new HashSet<Integer>();
        for (String current : input) {
            if (current.contains("*")) {
                /*
                 * This is the case where the user has specified the port as a wildcard.
                 * This should be stored as a special int value internally (-1).
                 */
                returnSet.add(-1);
            } else if (current.contains("-")) {
                /*
                 * This is the case where the user has specified the port as a range. Instead of
                 * storing the range, every value between the two endpoints should be added to the
                 * set that we return.
                 */
                String[] ports = current.split("-", 2);
                try {
                    if (ports.length == 2) {
                        Integer startPort = Integer.parseInt(ports[0]);
                        Integer endPort = Integer.parseInt(ports[1]);

                        // We should only move forward if the port range is valid
                        if (startPort > 0 && endPort < 65535) {
                            for (int i = startPort; i <= endPort; i++) {
                                returnSet.add(i);
                            }
                        }
                    }
                } catch (NumberFormatException exception) {
                    //TODO: What should be done here?
                }
            } else {
                try {
                    returnSet.add(Integer.parseInt(current));
                } catch (NumberFormatException exception) {
                    //TODO: What should be done here?
                }
            }
        }

        return returnSet;
    }
}
