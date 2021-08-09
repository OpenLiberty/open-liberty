/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.udpchannel.UDPConfigConstants;

/**
 * @author mjohnson
 */
public class UDPChannelConfiguration {

    private static final TraceComponent tc = Tr.register(UDPChannelConfiguration.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private static int DEFAULT_READ_BUFFER_SIZE = 1024000;

    private ChannelData channelData;
    private String hostname = null;
    private int port = 0;
    private int sendBufferSize = 0;
    private int receiveBufferSize = 0;
    private int channelReceiveBufferSize = UDPConfigConstants.MAX_UDP_PACKET_SIZE;
    private String addressExcludeList[] = null;
    private String addressIncludeList[] = null;

    /**
     * Constructor.
     */
    public UDPChannelConfiguration() {
        this.sendBufferSize = DEFAULT_READ_BUFFER_SIZE;
        this.receiveBufferSize = DEFAULT_READ_BUFFER_SIZE;
    }

    private void setValues(Map<Object, Object> props) throws ChannelException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "setValues");
        }

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            try {
                if (key.equals("id") ||
                    key.equals("type") ||
                    key.startsWith("service.") ||
                    key.startsWith("component.") ||
                    key.startsWith("config.") ||
                    key.startsWith("objectClass") ||
                    key.startsWith("parentPid")) {
                    // skip osgi standard properties
                    continue;
                }

                if (isInboundChannel()) {
                    if (key.equalsIgnoreCase(UDPConfigConstants.HOST_NAME)) {
                        setHostname((String) value);
                        continue;
                    }
                    if (key.equalsIgnoreCase(UDPConfigConstants.PORT)) {
                        setPort(Integer.parseInt((String) value));
                        continue;
                    }
                    if (key.equalsIgnoreCase(UDPConfigConstants.ENDPOINT_NAME)) {
                        // This parameter only has to be present if WAS is running,
                        // It is used by the Channel Framework later.
                        // Don't throw a warning and move on.
                        continue;
                    }

                }
                if (key.equalsIgnoreCase(UDPConfigConstants.SEND_BUFF_SIZE)) {
                    setSendBufferSize(Integer.parseInt((String) value));
                    continue;
                }
                if (key.equalsIgnoreCase(UDPConfigConstants.RCV_BUFF_SIZE)) {
                    setReceiveBufferSize(Integer.parseInt((String) value));
                    continue;
                }
                if (key.equalsIgnoreCase(UDPConfigConstants.CHANNEL_RCV_BUFF_SIZE)) {
                    setChannelReceiveBufferSize(Integer.parseInt((String) value));
                    continue;
                }
                if (key.equalsIgnoreCase(UDPConfigConstants.ADDR_EXC_LIST)) {
                    if (value instanceof String) {
                        this.addressExcludeList = convertToArray((String) value);
                    } else {
                        this.addressExcludeList = (String[]) value;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(UDPConfigConstants.ADDR_INC_LIST)) {
                    if (value instanceof String) {
                        this.addressIncludeList = convertToArray((String) value);
                    } else {
                        this.addressIncludeList = (String[]) value;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(ChannelFrameworkConstants.CHAIN_DATA_KEY)
                    || key.equalsIgnoreCase(ChannelFrameworkConstants.CHAIN_NAME_KEY)) {
                    // these parameters are made available by the channel framework
                    // for any channel that needs it. This channel does not need
                    // it so ignore and move on.
                    continue;
                }

                if (value instanceof String) {
                    Tr.warning(tc, "CWUDP0003W", new Object[] { getExternalName(), key, value });
                } else {
                    Tr.warning(tc, "CWUDP0003W", new Object[] { getExternalName(), key, "" });
                }
            } catch (NumberFormatException x) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Incorrect number in config; " + key + "=" + value);
                }
                throw new ChannelException(getExternalName() + ": Incorrect number in " + key + "=" + value, x);
            } catch (Throwable t) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Unexpected failure handling new config; " + t);
                }
                throw new ChannelException(getExternalName() + ": Unexpected error in new config", t);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "setValues");
        }
    }

    private String[] convertToArray(String allEntries) {
        // convert String allEntries (which has comma seperators)
        // into an array of strings.

        int start = 0;
        int end = 0;
        String newAddress = null;
        if (allEntries == null) {
            return null;
        }

        int length = allEntries.length();

        List<String> entryList = new ArrayList<String>();

        while (start != length) {
            end = allEntries.indexOf(",", start);
            if (end > start) {
                newAddress = allEntries.substring(start, end);
                newAddress = newAddress.trim();
                entryList.add(newAddress);
            } else {
                // if end == start, then the substring is only a comma, so
                // ignore otherwise end is -1, so just go to the end
                if (end != start) {
                    newAddress = allEntries.substring(start);
                    newAddress = newAddress.trim();
                    entryList.add(newAddress);
                }
            }

            if (end == -1) {
                break; // no more data in string
            }
            start = end + 1;
        }

        if (entryList.isEmpty()) {
            return null;
        }
        return entryList.toArray(new String[entryList.size()]);
    }

    protected void outputConfigToTrace() {
        Tr.debug(tc, "Config parameters for UDP Channel: " + getExternalName());
        Tr.debug(tc, "Inbound: " + isInboundChannel());
        if (isInboundChannel()) {
            Tr.debug(tc, UDPConfigConstants.HOST_NAME + ": " + getHostname());
            Tr.debug(tc, UDPConfigConstants.PORT + ": " + getPort());
        }

        Tr.debug(tc, UDPConfigConstants.RCV_BUFF_SIZE + ": " + getReceiveBufferSize());
        Tr.debug(tc, UDPConfigConstants.CHANNEL_RCV_BUFF_SIZE + ": " + getChannelReceiveBufferSize());
        Tr.debug(tc, UDPConfigConstants.SEND_BUFF_SIZE + ": " + getSendBufferSize());
    }

    /**
     * Set the hostname to the input value.
     *
     * @param name
     */
    protected void setHostname(String name) {
        if (null != name) {
            this.hostname = name.trim();
        }
    }

    /**
     * Query the external name of this channel.
     *
     * @return String
     */
    public String getExternalName() {
        return this.channelData.getExternalName();
    }

    /**
     * Query the hostname set in the configuration.
     *
     * @return String
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Query whether this is an inbound UDP channel or not.
     *
     * @return boolean
     */
    public boolean isInboundChannel() {
        return this.channelData.isInbound();
    }

    /**
     * Access the channel framework configuration object for this channel.
     *
     * @return ChannelData
     */
    public ChannelData getChannelData() {
        return this.channelData;
    }

    /**
     * Update the configuration with a new channel framework configuration object.
     *
     * @param data
     * @throws ChannelException
     */
    public void setChannelData(ChannelData data) throws ChannelException {
        this.channelData = data;
        setValues(data.getPropertyBag());
        if (tc.isDebugEnabled())
            outputConfigToTrace();
    }

    /**
     * Query the port value set in the configuration, only valid for an inbound
     * channel.
     *
     * @return int
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Set the port value for the configuration, only valid for an inbound
     * channel.
     *
     * @param newPort
     */
    public void setPort(int newPort) {
        this.port = newPort;
    }

    /**
     * Query the socket receive buffer size setting value.
     *
     * @return int
     */
    public int getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    /**
     * Query the socket send buffer size value.
     *
     * @return int
     */
    public int getSendBufferSize() {
        return this.sendBufferSize;
    }

    /**
     * Set the socket receive buffer size value.
     *
     * @param size
     */
    public void setReceiveBufferSize(int size) {
        this.receiveBufferSize = size;
    }

    /**
     * Set the socket send buffer size value.
     *
     * @param size
     */
    public void setSendBufferSize(int size) {
        this.sendBufferSize = size;
    }

    /**
     * Query the bytebuffer size configured for receiving data.
     *
     * @return int
     */
    public int getChannelReceiveBufferSize() {
        return this.channelReceiveBufferSize;
    }

    /**
     * Set the size of the bytebuffer to allocate when receiving data.
     *
     * @param size
     */
    public void setChannelReceiveBufferSize(int size) {
        this.channelReceiveBufferSize = size;
        if (size < 0 || size > UDPConfigConstants.MAX_UDP_PACKET_SIZE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel Receive buffer size not within Limits: " + size + " setting to default: " + UDPConfigConstants.MAX_UDP_PACKET_SIZE);
            }
            this.channelReceiveBufferSize = UDPConfigConstants.MAX_UDP_PACKET_SIZE;
        }
    }

    /**
     * Query the address exclude list.
     *
     * @return String[] - null if not configured
     */
    protected String[] getAddressExcludeList() {
        return this.addressExcludeList;
    }

    /**
     * Query the address include list.
     *
     * @return String[] - null if not configured
     */
    protected String[] getAddressIncludeList() {
        return this.addressIncludeList;
    }

}
