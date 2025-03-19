/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package componenttest.topology.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.net.ssl.HttpsURLConnection;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This class represents an external service that has been defined in a central registry with additional properties about it.
 */
public class ExternalTestService {

    private static final Class<?> c = ExternalTestService.class;

    private final String address;
    private final String serviceName;
    private final int port;
    private final String hostname;
    private final Map<String, ServiceProperty> props;
    private static Random rand = new Random();
    private static Map<String, Collection<String>> unhealthyServiceInstances = new HashMap<String, Collection<String>>();
    private static ExternalTestService propertiesDecrypter;

    private static final String PROP_NETWORK_LOCATION = "global.network.location";
    private static final String PROP_SERVER_ORIGIN = "server.origin";
    private static final String PROP_CONSUL_SERVERLIST = "global.consulServerList";
    private static final String ENCRYPTED_PREFIX = "encrypted!";
    private static final String PROP_ACCESS_TOKEN = "global.ghe.access.token";

    private ExternalTestService(JsonObject data, Map<String, ServiceProperty> props) {
        JsonObject serviceData = data.getJsonObject("Service");
        JsonObject nodeData = data.getJsonObject("Node");
        String networkLocationProp = getNetworkLocation() + "_address";
        String address = null;

        if (props.get(networkLocationProp) != null) {
            //The service has a private IP on the same network, so use that
            try {
                address = props.get(networkLocationProp).getStringValue();
            } catch (Exception ex) {
                address = nodeData.getString("Address");
            }
        } else if (!serviceData.getString("Address", "").isEmpty()) {
            //Use the service address
            address = serviceData.getString("Address");
        } else {
            //No Service address so use the node address
            address = nodeData.getString("Address");
        }
        this.hostname = nodeData.getString("Node");
        this.address = address;
        this.serviceName = serviceData.getString("Service");
        this.port = serviceData.getInt("Port", -1);
        this.props = props;

    }

    /**
     * Retrieves a Consul value from a key/value pair that may or may not be associated with a Consul service.
     *
     * @param propertyName The property name or path. It should be available in a web browser at:
     *                         ${consulServer}/v1/kv/service/${propertyName}
     */
    public static String getProperty(String propertyName) throws Exception {
        Exception firstEx = null;
        for (String consulServer : getConsulServers()) {
            try {
                JsonArray propertiesJson = new HttpsRequest(consulServer + "/v1/kv/service/" + propertyName + "?recurse=true&stale")
                                .allowInsecure()
                                .timeout(30000)
                                .expectCode(HttpsURLConnection.HTTP_OK)
                                .expectCode(HttpsURLConnection.HTTP_NOT_FOUND)
                                .run(JsonArray.class);

                if (propertiesJson == null) {
                    throw new Exception("The Consul server (" + consulServer
                                        + ") was unavailable or did not return a result for the property: " + propertyName
                                        + ". Look on #was-liberty-ops for outages, or updates to global.consulServerList");
                }

                if (propertiesJson.size() != 1) {
                    throw new Exception("Expected to find exactly 1 property but found " + propertiesJson.size() +
                                        ". Full JSON is: " + propertiesJson);

                }
                JsonObject propertyObject = propertiesJson.getJsonObject(0);
                if (!propertyObject.containsKey("Value")) {
                    throw new Exception("Property " + propertyName + " was found but contained no value. Full JSON is: " + propertyObject);
                }

                ServiceProperty prop = new ServiceProperty("", propertyName, propertyObject.getString("Value"));
                return prop.getStringValue();
            } catch (Exception e) {
                if (firstEx == null)
                    firstEx = e;
                continue;
            }
        }

        throw firstEx;
    }

    /**
     * Returns an ExternalTestService that matches the given service name.
     * The service returned is randomized so that load is distributed across all healthy instances.
     * This method consider the health of the service both in the central registry and locally.
     * The service must be healthy in the central registry for it to be returned.
     * Additional it will avoid returned services that have been reported as locally unhealthy as long as local healthy instances remain.
     *
     * It is desirable for release() to be called when the service is finished with to ensure any locking on the service is efficiently cleaned up
     *
     * @param  serviceName the name of the service type in the central registry e.g. selenium
     * @return             the ExternalTestService selected at random
     * @throws Exception   If either no healthy services could be found or no consul server could be contacted.
     */
    public static ExternalTestService getService(String serviceName) throws Exception {
        return getService(serviceName, null);
    }

    /**
     * Returns an ExternalTestService that matches the given service name.
     * The service returned is randomized so that load is distributed across all healthy instances.
     * This method consider the health of the service both in the central registry and locally.
     * The service must be healthy in the central registry for it to be returned.
     * Additional it will avoid returned services that have been reported as locally unhealthy as long as local healthy instances remain.
     *
     * The filter will only allow elements that match to be returned. This allows for more efficient retrieval of services rather than keeping asking for each service until finding
     * an appropriate one
     *
     * It is desirable for release() to be called when the service is finished with to ensure any locking on the service is efficiently cleaned up
     *
     * @param  serviceName the name of the service type in the central registry e.g. selenium
     * @param  filter      a filter that will allow only matched Services to be returned or null for no filter
     * @return             the ExternalTestService selected at random
     * @throws Exception   If either no healthy services could be found or no consul server could be contacted.
     */
    public static ExternalTestService getService(String serviceName, ExternalTestServiceFilter filter) throws Exception {
        return getServices(1, serviceName, filter).iterator().next();
    }

    /**
     * Returns an Collection of ExternalTestServices that matches the given service name.
     * The services returned are randomized so that load is distributed across all healthy instances.
     * This method considers the health of the service both in the central registry and locally.
     * The services must be healthy in the central registry for it to be returned.
     * Additional it will avoid returned services that have been reported as locally unhealthy as long as local healthy instances remain.
     *
     * It is desirable for release() to be called when the service is finished with to ensure any locking on the service is efficiently cleaned up
     *
     * @param  count       the number of given service that should be returned
     * @param  serviceName the name of the service type in the central registry e.g. selenium
     * @return             Collection of ExternalTestService selected at random, where the collection size matches the supplied count
     * @throws Exception   If either not enough healthy services could be found or no consul server could be contacted.
     */
    public static Collection<ExternalTestService> getServices(int count, String serviceName) throws Exception {
        return getServices(count, serviceName, null);
    }

    private static List<String> consulServers = null;

    private static List<String> getConsulServers() throws Exception {
        if (consulServers != null)
            return consulServers;

        String consulServerList = System.getProperty(PROP_CONSUL_SERVERLIST);
        if (consulServerList == null) {
            throw new Exception("There are no Consul hosts defined. Please ensure that the '" + PROP_CONSUL_SERVERLIST
                                + "' property contains a comma separated list of Consul hosts and is included in the "
                                + "user.build.properties file in your home directory. If not running on the IBM nework, "
                                + "this message can be ignored.");
        }

	// Add all the servers to the list twice, effectively giving us a retry so double the chance of working if consul is slow
        List<String> servers = Arrays.asList((consulServerList+","+consulServerList).split(","));
        return consulServers = servers;
    }

    /**
     * Returns an Collection of ExternalTestServices that matches the given service name.
     * The services returned are randomized so that load is distributed across all healthy instances.
     * This method considers the health of the service both in the central registry and locally.
     * The services must be healthy in the central registry for it to be returned.
     * Additional it will avoid returned services that have been reported as locally unhealthy as long as local healthy instances remain.
     *
     * The filter will only allow elements that match to be returned. This allows for more efficient retrieval of services rather than keeping asking for each service until finding
     * an appropriate one
     *
     * It is desirable for release() to be called when the service is finished with to ensure any locking on the service is efficiently cleaned up
     *
     * @param  count       the number of given service that should be returned
     * @param  serviceName the name of the service type in the central registry e.g. selenium
     * @param  filter      a filter that will allow only matched Services to be returned or null for no filter
     * @return             Collection of ExternalTestService selected at random, where the collection size matches the supplied count
     * @throws Exception   If either not enough healthy services could be found or no consul server could be contacted.
     */
    public static Collection<ExternalTestService> getServices(int count, String serviceName, ExternalTestServiceFilter filter) throws Exception {
        if (filter == null) {
            filter = new ExternalTestServiceFilterAlwaysMatched();
        }

        Collection<String> unhealthyReadOnly;
        synchronized (unhealthyServiceInstances) {

            Collection<String> unhealthyList = unhealthyServiceInstances.get(serviceName);
            if (unhealthyList == null) {
                unhealthyList = new HashSet<String>();
                unhealthyServiceInstances.put(serviceName, unhealthyList);
            }
            unhealthyReadOnly = new HashSet<String>(unhealthyList);
        }

        Exception finalError = null;
        for (String consulServer : getConsulServers()) {
            JsonArray instances;
            try {
                HttpsRequest instancesRequest = new HttpsRequest(consulServer + "/v1/health/service/" + serviceName + "?passing=true&stale");
                instancesRequest.timeout(30000);
                instancesRequest.allowInsecure();
                instances = instancesRequest.run(JsonArray.class);
            } catch (Exception e) {
                finalError = e;
                continue;
            }
            //fail if no instances available
            if (instances.isEmpty()) {
                throw new Exception("There are no healthy services available for " + serviceName);
            }

            JsonArray propertiesJson;
            try {
                HttpsRequest propsRequest = new HttpsRequest(consulServer + "/v1/kv/service/" + serviceName + "/?recurse=true&stale");
                propsRequest.allowInsecure();
                propsRequest.timeout(10000);
                propsRequest.expectCode(HttpsURLConnection.HTTP_OK).expectCode(HttpsURLConnection.HTTP_NOT_FOUND);
                propertiesJson = propsRequest.run(JsonArray.class);
            } catch (Exception e) {
                finalError = e;
                continue;
            }

            // Extract properties for each service instance
            // propMap maps NodeName -> Collection<ServiceProperty>
            Map<String, Collection<ServiceProperty>> propMap = new HashMap<String, Collection<ServiceProperty>>();
            if (propertiesJson != null) {
                for (int index = 0; index < propertiesJson.size(); index++) {
                    ServiceProperty property = parseServiceProperty(propertiesJson.getJsonObject(index));
                    if (property == null) {
                        continue;
                    } else {
                        Collection<ServiceProperty> propList = propMap.get(property.getNodeName());
                        if (propList == null) {
                            propList = new ArrayList<ServiceProperty>();
                            propMap.put(property.getNodeName(), propList);
                        }

                        propList.add(property);
                    }
                }
            }

            //convert to list of external test services
            List<ExternalTestService> healthyTestServices = new ArrayList<ExternalTestService>();
            List<ExternalTestService> unhealthyTestServices = new ArrayList<ExternalTestService>();
            for (int index = 0; index < instances.size(); index++) {
                JsonObject instanceJson = instances.getJsonObject(index);
                String nodeName = instanceJson.getJsonObject("Node").getString("Node");

                Map<String, ServiceProperty> instancePropMap = new HashMap<String, ServiceProperty>();

                Collection<ServiceProperty> commonProps = propMap.get("common");
                if (commonProps != null) {
                    for (ServiceProperty prop : commonProps) {
                        instancePropMap.put(prop.key, prop);
                    }
                }
                Collection<ServiceProperty> serviceProps = propMap.get(nodeName);
                if (serviceProps != null) {
                    for (ServiceProperty prop : serviceProps) {
                        instancePropMap.put(prop.key, prop);
                    }
                }

                ExternalTestService instance = new ExternalTestService(instanceJson, instancePropMap);

                boolean unhealthy = unhealthyReadOnly.contains(instance.getAddress());
                if (!unhealthy) {
                    //Add to possible list
                    healthyTestServices.add(instance);
                } else {
                    unhealthyTestServices.add(instance);
                }
            }

            //pick random healthy instance
            Collection<ExternalTestService> matchedServices = getMatchedService(count, healthyTestServices, filter);
            if (matchedServices != null && matchedServices.size() == count) {
                return matchedServices;
            }

            throw new Exception("There are not enough healthy services available for " + serviceName + " that match the filter provided");

        }

        //Http requests above ended in exception
        Exception e = new Exception("Exception attempting to connect to Consul servers");
        e.initCause(finalError);
        throw e;

    }

    private static ServiceProperty parseServiceProperty(JsonObject json) {
        String key = json.getString("Key");
        String[] keyParts = key.split("/", 4);

        if (keyParts.length < 3) {
            return null;
        }

        String instanceName = keyParts[2];
        String keyName = keyParts[3];

        JsonValue value = json.get("Value");
        String base64EncodedValue;
        // Value can be null, so check the type before decoding it
        if (value.getValueType() == ValueType.STRING) {
            base64EncodedValue = ((JsonString) value).getString();
        } else {
            base64EncodedValue = "";
        }

        return new ServiceProperty(instanceName, keyName, base64EncodedValue);
    }

    private static class ServiceProperty {

        private final String instance;
        private final String key;
        private String base64EncodedValue;
        private byte[] value = null;
        private String stringValue = null;

        private ServiceProperty(String instance, String key, String base64EncodedValue) {
            this.instance = instance;
            this.key = key;
            this.base64EncodedValue = base64EncodedValue;
        }

        private String getNodeName() {
            return instance;
        }

        private String getKey() {
            return key;
        }

        private synchronized byte[] getValue() {
            if (value == null) {
                value = Base64.getDecoder().decode(base64EncodedValue);
                base64EncodedValue = null;
            }
            return value;
        }

        /**
         * @return                          the value as a string
         * @throws CharacterCodingException if the value is not a UTF-8 encoded string
         * @throws Exception                if the value is encrypted and cannot be decrypted
         */
        private String getStringValue() throws CharacterCodingException, Exception {
            if (stringValue == null) {
                CharBuffer charValue = Charset.forName("UTF-8")
                                .newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(getValue()));
                String utf8Value = charValue.toString();
                if (utf8Value.startsWith(ENCRYPTED_PREFIX)) {
                    stringValue = ExternalTestService.decrypt(utf8Value);
                } else {
                    stringValue = utf8Value;
                }
            }
            return stringValue;
        }
    }

    private static Collection<ExternalTestService> getMatchedService(int count, List<ExternalTestService> testServices, ExternalTestServiceFilter filter) throws Exception {
        Collections.shuffle(testServices, rand);
        Exception exception = null;
        Collection<ExternalTestService> matchedServices = new ArrayList<ExternalTestService>();
        for (ExternalTestService externalTestService : testServices) {
            //Do Network Location Filter
            try {
                externalTestService.decryptProperties();
                String locationString = externalTestService.getProperties().get("allowed.networks");
                if (locationString != null) {
                    List<String> allowedNetworks = Arrays.asList(locationString.split(","));
                    String networkLocation = getNetworkLocation();
                    if (!allowedNetworks.contains(networkLocation)) {
                        //Network is not allowed
                        externalTestService.reportUnhealthy("Build Machine cannot use instance as its networks location ("
                                                            + networkLocation +
                                                            ") is not in allowed.networks ("
                                                            + locationString +
                                                            ")");
                        continue;
                    }

                }
                //If it reached here network is allowable

                //Do Filter
                boolean isMatched = filter.isMatched(externalTestService);
                if (isMatched) {
                    //We found one
                    matchedServices.add(externalTestService);
                    if (matchedServices.size() == count) {
                        return matchedServices;
                    }
                }
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return null;
    }

    private interface RetryPolicy {

        /**
         * @return true if the operation should be retried; false otherwise.
         */
        boolean retryable();

    }

    private static class CappedExponentialSleeps implements RetryPolicy {

        private int sleepMsecs;
        private final int numSleeps;
        private int completedSleeps;
        private final int capMsecs;

        /**
         * @param sleepMsecs initial sleep time in msecs (doubles on each retry)
         * @param numSleeps  number of times to do a sleep before it becomes non-retryable
         * @param capMsecs   maximum number of msecs to sleep
         */
        public CappedExponentialSleeps(int sleepMsecs, int numSleeps, int capMsecs) {
            this.sleepMsecs = sleepMsecs;
            this.numSleeps = numSleeps;
            this.capMsecs = capMsecs;
            this.completedSleeps = 0;
        }

        /*
         */
        @Override
        public boolean retryable() {
            if (completedSleeps < numSleeps) {
                sleepMsecs *= 2;
                if (sleepMsecs > capMsecs)
                    sleepMsecs = capMsecs;

                completedSleeps++;
                try {
                    Thread.sleep(sleepMsecs);
                } catch (InterruptedException e) {
                    return false;
                }
                return true;
            }
            return false;
        }

    }

    /**
     * @param  encryptedValue
     * @return                decryptedValue
     * @throws Exception      If either no healthy liberty-properties-decrypter services could be found or no consul server could be contacted.
     */
    private static synchronized String decrypt(String encryptedValue) throws Exception {
        if (!encryptedValue.startsWith(ENCRYPTED_PREFIX)) {
            return encryptedValue;
        }

        // Identify Access Token
        String accessToken = System.getProperty(PROP_ACCESS_TOKEN);
        if (accessToken == null) {
            throw new Exception("Missing Property called: '" + PROP_ACCESS_TOKEN
                                + "', this property is needed to decrypt secure properties, see https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally for more info");
        }

        RetryPolicy retry = new CappedExponentialSleeps(1000, 8, 128000);

        // Locate Properties Decrypter Instance
        String decrypted = null;
        while (decrypted == null) {
            while (propertiesDecrypter == null) {
                try {
                    propertiesDecrypter = ExternalTestService.getService("liberty-properties-decrypter");
                } catch (Exception e) {
                    if (retry.retryable()) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            }

            // Decrypt Properties
            HttpsRequest propsRequest = new HttpsRequest("https://" + propertiesDecrypter.getAddress() + ":" + propertiesDecrypter.getPort() + "/decrypt?value=" + encryptedValue
                                                         + "&access_token=" + accessToken);
            propsRequest.allowInsecure();
            propsRequest.timeout(10000);
            propsRequest.expectCode(HttpsURLConnection.HTTP_OK).expectCode(HttpsURLConnection.HTTP_UNAUTHORIZED).expectCode(HttpsURLConnection.HTTP_FORBIDDEN);
            propsRequest.silent();
            try {
                decrypted = propsRequest.run(String.class);
            } catch (Exception e) {
                switch (propsRequest.getResponseCode()) {
                    case HttpsURLConnection.HTTP_INTERNAL_ERROR:
                    case HttpsURLConnection.HTTP_BAD_GATEWAY:
                    case HttpsURLConnection.HTTP_UNAVAILABLE:
                    case HttpsURLConnection.HTTP_NOT_IMPLEMENTED:
                        propertiesDecrypter.reportUnhealthy(e.getMessage());
                        propertiesDecrypter = null;
                        if (retry.retryable()) {
                            break;
                        } else {
                            ; //fall through
                        }

                    default:
                        propertiesDecrypter.reportUnhealthy(e.getMessage());
                        propertiesDecrypter = null;
                        break;
                }

            }
            switch (propsRequest.getResponseCode()) {
                case HttpsURLConnection.HTTP_UNAUTHORIZED:
                    throw new Exception(PROP_ACCESS_TOKEN
                                        + " is not recognized by github.ibm.com, see https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally for more info");
                case HttpsURLConnection.HTTP_FORBIDDEN:
                    throw new Exception(PROP_ACCESS_TOKEN
                                        + " is not able to be access organisation data, Access Token requires read:org permission, see https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally for more info");
                case HttpsURLConnection.HTTP_OK:
                    //Do nothing
            }

        }

        return decrypted;
    }

    /**
     * @return
     */
    private static String getNetworkLocation() {
        String networkLocation = System.getProperty(PROP_NETWORK_LOCATION);
        if (networkLocation != null) {
            return networkLocation;
        }
        String serverOrigin = System.getProperty(PROP_SERVER_ORIGIN);
	// Attempt to guess where the closest services will be located
	if (serverOrigin.startsWith("9.20.")) {
	    // Hursley
	    return "IBM9UK";
	} else if (serverOrigin.startsWith("9.42.") || serverOrigin.startsWith("9.46.")) {
	    // RTP
	    return "IBM9US";
	} else if (serverOrigin.startsWith("9.30.")) {
	    // SVL
	    return "IBM9US";
	}  else if (serverOrigin.startsWith("9.57.")) {
	    // POK
	    return "IBM9US";
        } else if (serverOrigin.startsWith("10.34.") || serverOrigin.startsWith("10.36.")) {	    
            return "HURPROD";
	} else if (serverOrigin.startsWith("10.51.")) {
	    return "FYREHUR";
	} else if (serverOrigin.startsWith("10.17.") || serverOrigin.startsWith("10.11.") || serverOrigin.startsWith("10.15.")) {
	    return "FYRESVL";
	} else if (serverOrigin.startsWith("10.21.") || serverOrigin.startsWith("10.26.")) {
	    return "FYRERTP";
        } else {
	    System.out.println("Unknown host/IP address " + serverOrigin + ".  For better effeciency, please add global.network.location=IBM9UK or IBM9US to your user.build.properties.  If appropriate, please update fattest.simplicity/src/componenttest/topology/utils/ExternalTestService.getNetworkLocation()");
            return "UNKNOWN";
        }
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return the serviceName as defined in the central registry
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @return the port of the service to use
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns a map of properties found about the service
     * <p>
     * Properties whose values are not Strings are not returned by this method but can be written to a file with {@link #writePropertyAsFile}
     *
     * @return a map of properties found about the service in the central registry
     */
    public Map<String, String> getProperties() {
        if (props == null) {
            return Collections.emptyMap();
        }
        Map<String, String> properties = new HashMap<String, String>();
        for (ServiceProperty prop : props.values()) {
            try {
                properties.put(prop.getKey(), prop.getStringValue());
            } catch (CharacterCodingException e) {
                // Skip value
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return properties;
    }

    private void decryptProperties() throws Exception {
        for (ServiceProperty prop : props.values()) {
            try {
                prop.getStringValue();
            } catch (CharacterCodingException e) {
                // Skip value
                continue;
            }
        }
    }

    /**
     * Write a service property value to a file
     * <p>
     * Useful if you have a truststore or keyfile stored in your service properties
     *
     * @param  keyName               the name of the service property to write to the file
     * @param  file                  the file to write to
     * @throws IOException           if there is an error writing the file
     * @throws IllegalStateException if there is no service property with the given key name
     */
    public void writePropertyAsFile(String keyName, File file) throws IOException {
        if (props == null) {
            throw new IllegalStateException("Key not found in service properties: " + keyName);
        }

        ServiceProperty prop = props.get(keyName);
        if (prop == null) {
            throw new IllegalStateException("Key not found in service properties: " + keyName);
        }

        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(prop.getValue());
        } finally {
            out.close();
        }
    }

    /**
     * This method should be used to report the instance as not working locally and will not be randomly selected again unless no other options remain
     *
     * Note: This implicitly calls release
     *
     * @param reason A simple explaination of what was unhealthy about it. e.g. Could not connect to selenium on machine.
     */
    public void reportUnhealthy(String reason) {
        synchronized (unhealthyServiceInstances) {
            Collection<String> unhealthyList = unhealthyServiceInstances.get(serviceName);
            unhealthyList.add(address);

        }
        Log.info(getClass(), "reportUnhealthy", getServiceName() + " Service at " + getAddress() + " reported as unhealthy because: " + reason);
        release();
    }

    /**
     * This method releases any locks acquired for this service, to allow reuse on other machines.
     */
    public void release() {
        //Placeholder to implement later
    }

    /**
     * Testing method to ensure the ExternalTestService Works
     *
     * @param  args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Collection<String> serviceAddresses = new HashSet<String>();
        for (int i = 0; i < 30; i++) {
            long startTime = System.currentTimeMillis();

            Collection<ExternalTestService> services = new ArrayList<ExternalTestService>();
            services.add(ExternalTestService.getService("EBC-Manager"));
            System.out.println("Took " + (System.currentTimeMillis() - startTime) + " ms");
            for (ExternalTestService service : services) {

                serviceAddresses.add(service.getAddress() + ":" + service.getPort());
                System.out.println(service.getProperties());
                //service.reportUnhealthy("Testing");
            }

        }

        System.out.println("Found " + serviceAddresses.size() + " EBC services");
    }

}
