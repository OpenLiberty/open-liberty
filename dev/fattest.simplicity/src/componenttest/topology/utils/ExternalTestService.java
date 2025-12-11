/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
import java.util.Objects;
import java.util.Random;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.net.ssl.HttpsURLConnection;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

/**
 * This class represents an external service that has been defined in a central registry with additional properties about it.
 * TODO write unit tests for this class
 */
public class ExternalTestService {

    private static final Class<?> c = ExternalTestService.class;

    // Service properties from consul
    private final Map<String, ServiceProperty> props;

    // Network properties to external service
    private final String address;
    private final String serviceName;
    private final int port;
    private final String hostname;

    // Keys for client JVM properties
    private static final String PROP_NETWORK_LOCATION = "global.network.location";
    private static final String PROP_SERVER_ORIGIN = "server.origin";
    private static final String PROP_CONSUL_SERVERLIST = "global.consulServerList";

    // Client network location
    private static final String CLIENT_NETWORK_LOCATION = getNetworkLocation();

    // Random number generator to scramble service order
    private static final Random rand = new Random();

    /**
     * Private constructor - new instances or collections of external test service
     * are created via builder methods on this class.
     *
     * @param data  - consul json response with data that represents this external test service
     * @param props - consul service properties for this external test service
     */
    private ExternalTestService(JsonObject data, Map<String, ServiceProperty> props) {
        JsonObject serviceData = data.getJsonObject("Service");
        JsonObject nodeData = data.getJsonObject("Node");
        String networkLocationProp = CLIENT_NETWORK_LOCATION + "_address";
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

    ///// BUILDER METHODS /////

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
        final String m = "getServices";

        if (filter == null) {
            filter = new ExternalTestServiceFilterAlwaysMatched();
        }

        Collection<String> unhealthyReadOnly = ExternalTestServiceReporter.getUnhealthyReport(serviceName);

        Log.info(c, m, "Getting " + count + " external test service(s) named '" + serviceName + "' with a " + filter.getClass().getName());
        if (!unhealthyReadOnly.isEmpty()) {
            Log.info(c, m, "\tExisting unhealthy instances: " + unhealthyReadOnly.toString());
        }

        Exception finalError = null;
        for (String consulServer : getConsulServers()) {
            JsonArray instances;
            try {
                HttpsRequest instancesRequest = new HttpsRequest(consulServer + "/v1/health/service/" + serviceName + "?passing=true&stale")
                                .timeout(30000)
                                .allowInsecure();
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

            Log.info(c, m, "Found " + healthyTestServices.size() + " potential " + serviceName + " services, attempting to find a matching service.");

            //pick random healthy instance
            Collection<ExternalTestService> matchedServices = getMatchedServices(count, healthyTestServices, filter);
            if (matchedServices != null && matchedServices.size() == count) {
                Log.info(c, m, "Found " + matchedServices.size() + " service(s): " + matchedServices);
                return matchedServices;
            }

            throw new Exception("There are not enough healthy services available for " + serviceName + " that match the filter provided");

        }

        Log.info(c, m, "Ran out of consul servers before finding enough match services");

        //Http requests above ended in exception
        Exception e = new Exception("Exception attempting to connect to Consul servers");
        e.initCause(finalError);
        throw e;
    }

    ///// UTILITY METHODS /////

    /**
     * Retrieves a Consul value from a key/value pair that may or may not be associated with a Consul service.
     *
     * TODO consider removing as this method is unused
     *
     * @param propertyName The property name or path. It should be available in a web browser at:
     *                         ${consulServer}/v1/kv/service/${propertyName}
     */
    @Deprecated
    private static String getProperty(String propertyName) throws Exception {
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

    private static List<String> consulServers = null;

    /**
     * Get list of consul servers from the consul server list system property
     *
     * @return           List of servers some of which may be repeated
     * @throws Exception if no servers were defined
     */
    private static List<String> getConsulServers() throws Exception {
        if (consulServers != null)
            return consulServers;

        final String m = "getConsulServers";

        String consulServerList = System.getProperty(PROP_CONSUL_SERVERLIST);
        if (consulServerList == null) {
            throw new Exception("There are no Consul hosts defined. Please ensure that the '" + PROP_CONSUL_SERVERLIST
                                + "' property contains a comma separated list of Consul hosts and is included in the "
                                + "user.build.properties file in your home directory. If not running on the IBM nework, "
                                + "this message can be ignored.");
        }

        // Add all the servers to the list twice, effectively giving us a retry so double the chance of working if consul is slow
        List<String> servers = Arrays.asList((consulServerList + "," + consulServerList).split(","));

        Log.info(c, m, "Initial consul server list (duplicates expected): " + servers);

        return consulServers = servers;
    }

    /**
     * Parses a json object from a consul http response to a service property object
     *
     * @param  json a json object
     * @return      a service property if the object can be parsed, null otherwise.
     */
    private static ServiceProperty parseServiceProperty(JsonObject json) {
        /*
         * Get the service property key in the form:
         * service/<service-name>/<service-instance-name>/<property-key-name>
         */
        String key = json.getString("Key");
        String[] keyParts = key.split("/", 4);

        if (keyParts.length < 3) {
            return null;
        }

        String instanceName = keyParts[2];
        String keyName = keyParts[3];

        /*
         * Get the service property value.
         * The data is base64 encoded. After decoding, the data may still be encrypted
         */
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

    /**
     * Iterates through a list of services and returns a collection of desired size of services that match various criteria
     * as described below:
     *
     * <ol>
     * <li>Network location filtering: Service must allow connections from the client network location</li>
     * <li>Property decryption: Property decrypter service must be able to decrypt service properties (if applicable)</li>
     * <li>Test Service Filter: Service must pass the supplied filter's test method</li>
     * </ol>
     *
     * If the service fails any of these criteria it will be reported as unhealthy and we will continue onto the next service
     * until we have collected the requested under.
     *
     * @param  count        number of services to locate
     * @param  testServices the list of all test services
     * @param  filter       a client supplied filter with additional criteria
     * @return              collection of test services that match all criteria, or null if no service could be located.
     * @throws Exception    if we have exhausted the list of services and we encountered an exception along the way.
     */
    private static Collection<ExternalTestService> getMatchedServices(int count, List<ExternalTestService> testServices, ExternalTestServiceFilter filter) throws Exception {
        String m = "getMatchedServices";
        Collections.shuffle(testServices, rand);
        Exception exception = null;
        Collection<ExternalTestService> matchedServices = new ArrayList<ExternalTestService>();

        for (ExternalTestService externalTestService : testServices) {
            String serviceName = externalTestService.serviceName;

            //Do Network Location Filtering
            try {
                String locationString = externalTestService.getProperties().get("allowed.networks");
                if (Objects.nonNull(locationString)) { // If null assume the service supports all networks
                    List<String> allowedNetworks = Arrays.asList(locationString.split(","));
                    if (!allowedNetworks.contains(CLIENT_NETWORK_LOCATION)) {
                        String reason = "Build Machine cannot use instance of " + serviceName + " as its networks location ("
                                        + CLIENT_NETWORK_LOCATION + ") is not in allowed.networks (" + locationString + ")";
                        ExternalTestServiceReporter.reportUnhealthy(externalTestService, reason);
                        continue;
                    }
                }

                Log.info(c, m, "Matched " + serviceName + " service based on network location (" + CLIENT_NETWORK_LOCATION + //
                               ") is in allowed.networks (" + (Objects.isNull(locationString) ? "ALL" : locationString) + //
                               ") continue to match service based on user defined filters.");

                // Do decryption - fail fast if no decryption service is available
                externalTestService.decryptProperties();

                //Do Filter
                if (filter.isMatched(externalTestService)) {
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

    /**
     * Determines the network location of the test system that is attempting to access the
     * external test service.
     *
     * @return The network location system property if it was set, otherwise a heuristically
     *         determined network location based on the server origin system property.
     */
    private static String getNetworkLocation() {
        // Priority 1: Use global.network.location system property
        String networkLocation = System.getProperty(PROP_NETWORK_LOCATION);
        if (networkLocation != null) {
            return networkLocation;
        }

        // Priority 2: Fail if we are not running locally, all build definitions should have the above property.
        if (!FATRunner.FAT_TEST_LOCALRUN) {
            throw new RuntimeException("This build definition lacked the " + PROP_NETWORK_LOCATION + " system property. "
                                       + "This property is required for our builds to correctly choose an external test resource. "
                                       + "Please contact a build monitor update this build definition with the " + PROP_NETWORK_LOCATION + " system property.");
        } else {
            Log.warning(c, "For better efficiency, please add global.network.location=IBM9UK or IBM9US to your user.build.properties.");
        }

        // Priority 3: Make a best guess as to where the closest service is based off of the client IP address.
        String serverOrigin = System.getProperty(PROP_SERVER_ORIGIN);
        // Attempt to guess where the closest services will be located
        if (serverOrigin.startsWith("9.20.")) {
            return "IBM9UK"; // Hursley
        } else if (serverOrigin.startsWith("9.42.") || serverOrigin.startsWith("9.46.")) {
            return "IBM9US"; // RTP
        } else if (serverOrigin.startsWith("9.30.")) {
            return "IBM9US"; // SVL
        } else if (serverOrigin.startsWith("9.57.")) {
            return "IBM9US"; // POK
        } else if (serverOrigin.startsWith("10.34.") || serverOrigin.startsWith("10.36.")) {
            return "HURPROD";
        } else if (serverOrigin.startsWith("10.51.")) {
            return "FYREHUR";
        } else if (serverOrigin.startsWith("10.11.") || serverOrigin.startsWith("10.15.") || serverOrigin.startsWith("10.17.")) {
            return "FYRESVL";
        } else if (serverOrigin.startsWith("10.21.") || serverOrigin.startsWith("10.22.") || serverOrigin.startsWith("10.23.") || serverOrigin.startsWith("10.26.")) {
            return "FYRERTP";
        } else {
            Log.warning(c, "Unable to determine closest service for the host/IP address: " + serverOrigin + ". "
                           + "If appropriate, please update fattest.simplicity/src/componenttest/topology/utils/ExternalTestService.getNetworkLocation()");
            return "UNKNOWN";
        }
    }

    ///// UTILITY CLASSES /////

    /**
     * POJO that represents a service property
     */
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

                stringValue = ExternalTestServiceDecrypter.decrypt(utf8Value);
            }
            return stringValue;
        }
    }

    ///// GETTERS /////

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

    ///// METHODS /////

    @Override
    public String toString() {
        return "ExternalTestService [serviceName=" + serviceName + ", hostname=" + hostname + ", port=" + port + "]";
    }

    /**
     * Returns a map of properties found about the service.
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

    /**
     * Iterates through the list of properties and calls getStringValue()
     * This is an eager decrypt so we can fail fast if the accessToken
     * is unset or rejected by the decrypter service.
     *
     * @throws Exception
     */
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
     * Write a service property value to a file.
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
     * This method releases any locks acquired for this service, to allow reuse on other machines.
     */
    public void release() {
        //Placeholder to implement later
    }

    /**
     * Test classes should use the {@link ExternalTestServiceReporter}
     * instead of this method.
     *
     * TODO remove when all tests have been updated to use the reporter
     *
     * @param reason
     */
    @Deprecated
    public void reportUnhealthy(String reason) {
        ExternalTestServiceReporter.reportUnhealthy(this, reason);
    }

    ///// MAIN /////

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
