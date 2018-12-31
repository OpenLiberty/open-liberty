/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

//<OCD id="com.ibm.ws.mongo.mongo" ibm:alias="mongo" name="%mongo" description="%mongo.desc">
//<AD id="library.target"                 type="String"  default="(|(id=${libraryRef})(pid=${libraryRef}))" ibm:final="true" name="internal" description="internal use only"/>
//<AD id="onError"                        type="String"  default="WARN" ibm:type="onError" ibm:variable="onError" name="%onError" description="%onError.desc">
// <Option value="FAIL"   label="%onError.FAIL"/>
// <Option value="IGNORE" label="%onError.IGNORE"/>
// <Option value="WARN"   label="%onError.WARN"/>
//</AD>
//<!-- MongoOptions -->
//<AD id="alwaysUseMBeans"                ibmui:group="MongoOptions" type="Boolean" required="false" name="%alwaysUseMBeans" description="%alwaysUseMBeans.desc"/>
//<AD id="autoConnectRetry"               ibmui:group="MongoOptions" type="Boolean" required="false" name="%autoConnectRetry" description="%autoConnectRetry.desc"/>
//<AD id="connectionsPerHost"             ibmui:group="MongoOptions" type="Integer" required="false" min="0" name="%connectionsPerHost" description="%connectionsPerHost.desc"/>
//<AD id="connectTimeout"                 ibmui:group="MongoOptions" type="String"  required="false" ibm:type="duration(ms)" min="0" name="%connectTimeout" description="%connectTimeout.desc"/>
//<AD id="cursorFinalizerEnabled"         ibmui:group="MongoOptions" type="Boolean" required="false" name="%cursorFinalizerEnabled" description="%cursorFinalizerEnabled.desc"/>
//<AD id="description"                    ibmui:group="MongoOptions" type="String"  required="false" name="%description" description="%description.desc"/>
////<AD id="maxAutoConnectRetryTime"        ibmui:group="MongoOptions" type="String"  required="false" ibm:type="duration(ms)" min="0" name="%maxAutoConnectRetryTime" description="%maxAutoConnectRetryTime.desc"/>
//<AD id="maxWaitTime"                    ibmui:group="MongoOptions" type="String"  required="false" ibm:type="duration(ms)" name="%maxWaitTime" description="%maxWaitTime.desc"/>
//<AD id="readPreference"                 ibmui:group="MongoOptions" type="String"  required="false" name="%readPreference" description="%readPreference.desc">
// <Option value="nearest"            label="nearest"/>
// <Option value="primary"            label="primary"/>
// <Option value="primaryPreferred"   label="primaryPreferred"/>
// <Option value="secondary"          label="secondary"/>
// <Option value="secondaryPreferred" label="secondaryPreferred"/>
//</AD>
//<AD id="safe"                           ibmui:group="MongoOptions" type="Boolean" required="false" name="%safe" description="%safe.desc"/>
//<AD id="socketKeepAlive"                ibmui:group="MongoOptions" type="Boolean" required="false" name="%socketKeepAlive" description="%socketKeepAlive.desc"/>
//<AD id="threadsAllowedToBlockForConnectionMultiplier" ibmui:group="MongoOptions" type="Integer" required="false" min="0" name="%threadsAllowedToBlockForConnectionMultiplier" description="%threadsAllowedToBlockForConnectionMultiplier.desc"/>
//<AD id="w"                              ibmui:group="MongoOptions" type="Integer" required="false" min="0" name="%w" description="%w.desc"/>
//</OCD>
public class MongoElement extends ConfigElement {
    private String hostNames, libraryRef, password, user, description, readPreference, writeConcern, sslRef, certificateSubject;
    private Boolean alwaysUserMBeans, autoConnectRetry, cursorFinalizerEnabled, socketKeepAlive, sslEnabled;
    private Integer connectionsPerHost, threadsAllowedToBlockForConnectionMultiplier, connectTimeout, maxWaitTime, socketTimeout;
    private Long maxAutoConnectRetryTime;

    /*
     * PortsAdapter is required for JAXB to unmarshal a comma-separated list of ports into an
     * integer array.
     */
    private static class PortsAdapter extends XmlAdapter<String, Integer[]> {
        @Override
        public Integer[] unmarshal(String v) throws Exception {
            if (v == null)
                return null;

            String[] portStrings = v.split(",");
            Integer[] ports = new Integer[portStrings.length];

            for (int i = 0; i < portStrings.length; ++i) {
                ports[i] = Integer.parseInt(portStrings[i]);
            }

            return ports;
        }

        @Override
        public String marshal(Integer[] v) throws Exception {
            if (v == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < v.length - 1; ++i) {
                sb.append(v[i]);
                sb.append(",");
            }

            // Append the last element without the ','
            sb.append(v[v.length - 1]);

            return sb.toString();
        }
    }

    @XmlAttribute
    @XmlJavaTypeAdapter(MongoElement.PortsAdapter.class)
    private Integer[] ports;

    @XmlElement(name = "hostNames")
    private List<String> hostNamesElements;

    @XmlElement(name = "ports")
    private List<Integer> portsElements;

    /**
     * @return the hostNames
     */
    public String getHostNames() {
        return hostNames;
    }

    /**
     * @param hostNames the hostNames to set
     */
    @XmlAttribute
    public void setHostNames(String hostNames) {
        this.hostNames = hostNames;
    }

    /**
     * @return the libraryRef
     */
    public String getLibraryRef() {
        return libraryRef;
    }

    /**
     * @param libraryRef the libraryRef to set
     */
    @XmlAttribute
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    @XmlAttribute
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    @XmlAttribute
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the connectTimeout
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @param connectTimeout the connectTimeout to set
     */
    @XmlAttribute
    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    @XmlAttribute
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the maxAutoConnectRetryTime
     */
    public Long getMaxAutoConnectRetryTime() {
        return maxAutoConnectRetryTime;
    }

    /**
     * @param maxAutoConnectRetryTime the maxAutoConnectRetryTime to set
     */
    @XmlAttribute
    public void setMaxAutoConnectRetryTime(Long maxAutoConnectRetryTime) {
        this.maxAutoConnectRetryTime = maxAutoConnectRetryTime;
    }

    /**
     * @return the maxWaitTime
     */
    public Integer getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * @param maxWaitTime the maxWaitTime to set
     */
    @XmlAttribute
    public void setMaxWaitTime(Integer maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * @return the readPreference
     */
    public String getReadPreference() {
        return readPreference;
    }

    /**
     * @param readPreference the readPreference to set
     */
    @XmlAttribute
    public void setReadPreference(String readPreference) {
        this.readPreference = readPreference;
    }

    /**
     * @return the socketTimeout
     */
    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @param socketTimeout the socketTimeout to set
     */
    @XmlAttribute
    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * @return the writeConcern
     */
    public String getWriteConcern() {
        return writeConcern;
    }

    /**
     * @param wtimeout the writeConcern to set
     */
    @XmlAttribute
    public void setWriteConcern(String writeConcern) {
        this.writeConcern = writeConcern;
    }

    /**
     * @return the alwaysUserMBeans
     */
    public Boolean getAlwaysUserMBeans() {
        return alwaysUserMBeans;
    }

    /**
     * @param alwaysUserMBeans the alwaysUserMBeans to set
     */
    @XmlAttribute
    public void setAlwaysUserMBeans(Boolean alwaysUserMBeans) {
        this.alwaysUserMBeans = alwaysUserMBeans;
    }

    /**
     * @return the autoConnectRetry
     */
    public Boolean getAutoConnectRetry() {
        return autoConnectRetry;
    }

    /**
     * @param autoConnectRetry the autoConnectRetry to set
     */
    @XmlAttribute
    public void setAutoConnectRetry(Boolean autoConnectRetry) {
        this.autoConnectRetry = autoConnectRetry;
    }

    /**
     * @return the cursorFinalizerEnabled
     */
    public Boolean getCursorFinalizerEnabled() {
        return cursorFinalizerEnabled;
    }

    /**
     * @param cursorFinalizerEnabled the cursorFinalizerEnabled to set
     */
    @XmlAttribute
    public void setCursorFinalizerEnabled(Boolean cursorFinalizerEnabled) {
        this.cursorFinalizerEnabled = cursorFinalizerEnabled;
    }

    /**
     * @return the socketKeepAlive
     */
    public Boolean getSocketKeepAlive() {
        return socketKeepAlive;
    }

    /**
     * @param socketKeepAlive the socketKeepAlive to set
     */
    @XmlAttribute
    public void setSocketKeepAlive(Boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }

    /**
     * @return the connectionsPerHost
     */
    public Integer getConnectionsPerHost() {
        return connectionsPerHost;
    }

    /**
     * @param connectionsPerHost the connectionsPerHost to set
     */
    @XmlAttribute
    public void setConnectionsPerHost(Integer connectionsPerHost) {
        this.connectionsPerHost = connectionsPerHost;
    }

    /**
     * @return the threadsAllowedToBlockForConnectionMultiplier
     */
    public Integer getThreadsAllowedToBlockForConnectionMultiplier() {
        return threadsAllowedToBlockForConnectionMultiplier;
    }

    /**
     * @param threadsAllowedToBlockForConnectionMultiplier the threadsAllowedToBlockForConnectionMultiplier to set
     */
    @XmlAttribute
    public void setThreadsAllowedToBlockForConnectionMultiplier(Integer threadsAllowedToBlockForConnectionMultiplier) {
        this.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
    }

    /**
     * @return the sslEnabled
     */
    public Boolean getSslEnabled() {
        return sslEnabled;
    }

    /**
     * @param sslEnabled the sslEnabled to set
     */
    @XmlAttribute
    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute
    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @return the certificateSubject
     */
    public String getCertificateSubject() {
        return certificateSubject;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute
    public void setCertificateSubject(String certificateSubject) {
        this.certificateSubject = certificateSubject;
    }

    /**
     * @param ports the ports to set
     */
    public void setPorts(Integer[] ports) {
        this.ports = ports;
    }

    /**
     * @param ports the ports to set
     */
    public Integer[] getPortList() {
        return this.ports;
    }

    /**
     * Get the list of values from &lt;hostNames> sub-elements
     * 
     * @return the hostnames
     */
    public List<String> getHostNamesElements() {
        if (hostNamesElements == null) {
            hostNamesElements = new ArrayList<String>();
        }
        return hostNamesElements;
    }

    /**
     * Get the list of values from &lt;ports> sub-elements
     * 
     * @return the ports
     */
    public List<Integer> getPortsElements() {
        if (portsElements == null) {
            portsElements = new ArrayList<Integer>();
        }
        return portsElements;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MongoElement [hostNames=" + hostNames + ", libraryRef=" + libraryRef + ", password=" + password + ", user=" + user + ", description=" + description
               + ", readPreference=" + readPreference + ", writeConcern=" + writeConcern + ", alwaysUserMBeans=" + alwaysUserMBeans + ", autoConnectRetry=" + autoConnectRetry
               + ", cursorFinalizerEnabled=" + cursorFinalizerEnabled + ", socketKeepAlive=" + socketKeepAlive + ", connectionsPerHost=" + connectionsPerHost
               + ", threadsAllowedToBlockForConnectionMultiplier=" + threadsAllowedToBlockForConnectionMultiplier + ", connectTimeout=" + connectTimeout + ", maxWaitTime="
               + maxWaitTime + ", socketTimeout=" + socketTimeout + ", maxAutoConnectRetryTime=" + maxAutoConnectRetryTime + ", sslEnabled=" + sslEnabled + ", sslRef=" + sslRef
               + ", certificateSubject=" + certificateSubject + ", ports=" + Arrays.toString(ports) + "]";
    }
}
