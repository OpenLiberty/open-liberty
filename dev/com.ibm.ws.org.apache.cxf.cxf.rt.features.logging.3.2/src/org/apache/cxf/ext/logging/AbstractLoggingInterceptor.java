/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.ext.logging;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrettyLoggingFilter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

public abstract class AbstractLoggingInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final int DEFAULT_LIMIT = 48 * 1024;
    public static final int DEFAULT_THRESHOLD = -1;
    public static final String CONTENT_SUPPRESSED = "--- Content suppressed ---";
    protected static final String  LIVE_LOGGING_PROP = "org.apache.cxf.logging.enable";
    private static final Pattern BOUNDARY_PATTERN =
        Pattern.compile("^--(\\S*)$", Pattern.MULTILINE);
    private static final Pattern CONTENT_TYPE_PATTERN =
        Pattern.compile("Content-Type:.*?$", 
                        Pattern.DOTALL | Pattern.MULTILINE);
    protected int limit = DEFAULT_LIMIT;
    protected long threshold = DEFAULT_THRESHOLD;
    protected boolean logBinary;
    protected boolean logMultipart = true;

    protected LogEventSender sender;
    protected final DefaultLogEventMapper eventMapper = new DefaultLogEventMapper();

    protected final MaskSensitiveHelper maskSensitiveHelper = new MaskSensitiveHelper();

    protected Set<String> sensitiveProtocolHeaderNames = new HashSet();

    // Liberty change begin
    // from LibertyApplicationBusFactory disableLogging is set to false always
    // from LibertyServiceImpl disableLogging toggles true and false depending on
    // trace enablement and EnableLoggingInOutInterceptor configuration settings 
    private static Boolean disableLogging = false;

    /**
     * @return the disableLogging
     */
    public static Boolean getDisableLogging() {
        return disableLogging;
    } 
    
    /**
     * @return the enableLoggingInOutInterceptor property value from endpoint info 
     */
    private static boolean isEndpointEnableLoggingInOutInterceptorTrue(Message message) {
        if (null != message.getExchange().getEndpoint()) {
            if (null != message.getExchange().getEndpoint().getEndpointInfo()) {
                Object prop = message.getExchange().getEndpoint().getEndpointInfo().getProperties().get("enableLoggingInOutInterceptor");
                return PropertyUtils.isTrue(prop);
            }
        }
        return false;
    }

    /**
     * @param disableLogging the disableLogging to set
     */
    public static void setDisableLogging(Boolean disable) {
        disableLogging = disable;
    } // Liberty change end
    
    public AbstractLoggingInterceptor(String phase, LogEventSender sender) {
        super(phase);
        this.sender = sender;
    }

    protected static boolean isLoggingDisabledNow(Message message) throws Fault {
        Object liveLoggingProp = message.getContextualProperty(LIVE_LOGGING_PROP);
        // Liberty change begin 
        boolean isLiveLoggingFalse = liveLoggingProp != null && PropertyUtils.isFalse(liveLoggingProp);
        if(isLiveLoggingFalse || (getDisableLogging() && !isEndpointEnableLoggingInOutInterceptorTrue(message)))    {
            return true;
        }
        return false;
     // Liberty change end
    }

    public void addBinaryContentMediaTypes(String mediaTypes) {
        eventMapper.addBinaryContentMediaTypes(mediaTypes);
    }
    
    public void setLimit(int lim) {
        this.limit = lim;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setInMemThreshold(long t) {
        this.threshold = t;
    }

    public long getInMemThreshold() {
        return threshold;
    }

    public void setSensitiveElementNames(final Set<String> sensitiveElementNames) {
        maskSensitiveHelper.setSensitiveElementNames(sensitiveElementNames);
    }
    
    public void addSensitiveElementNames(final Set<String> sensitiveElementNames) {
        maskSensitiveHelper.addSensitiveElementNames(sensitiveElementNames);
    }

    public void setSensitiveProtocolHeaderNames(final Set<String> protocolHeaderNames) {
        this.sensitiveProtocolHeaderNames.clear();
        addSensitiveProtocolHeaderNames(protocolHeaderNames);
    }
    
    public void addSensitiveProtocolHeaderNames(final Set<String> protocolHeaderNames) {
        this.sensitiveProtocolHeaderNames.addAll(protocolHeaderNames);
    }

    public void setPrettyLogging(boolean prettyLogging) {
        if (sender instanceof PrettyLoggingFilter) {
            ((PrettyLoggingFilter)this.sender).setPrettyLogging(prettyLogging);
        }
    }

    protected boolean shouldLogContent(LogEvent event) {
        return event.isBinaryContent() && logBinary
            || event.isMultipartContent() && logMultipart
            || !event.isBinaryContent() && !event.isMultipartContent();
    }

    public void setLogBinary(boolean logBinary) {
        this.logBinary = logBinary;
    }

    public void setLogMultipart(boolean logMultipart) {
        this.logMultipart = logMultipart;
    }

    public void createExchangeId(Message message) {
        Exchange exchange = message.getExchange();
        String exchangeId = (String)exchange.get(LogEvent.KEY_EXCHANGE_ID);
        if (exchangeId == null) {
            exchangeId = UUID.randomUUID().toString();
            exchange.put(LogEvent.KEY_EXCHANGE_ID, exchangeId);
        }
    }

    protected String transform(final Message message, final String originalLogString) {
        return originalLogString;
    }

    protected String maskSensitiveElements(final Message message, String originalLogString) {
        return maskSensitiveHelper.maskSensitiveElements(message, originalLogString);
    }
    
    protected String stripBinaryParts(LogEvent event, String originalLogString) {
        try {
            if (!this.logBinary && findBoundary(originalLogString) != null) {
                String boundary = findBoundary(originalLogString);
                String[] parts = originalLogString.split(Pattern.quote(boundary));
                String payload = "";
                for (String str : parts) {
                    if (findContentType(str) != null
                        && eventMapper.isBinaryContent(
                           findContentType(str).substring("Content-Type:".length()).trim())) {
                        payload = payload + "\r\n" + CONTENT_SUPPRESSED + "\r\n";
                    } else {
                        payload = payload + str;
                        payload = payload + boundary;
                    }
                }
            
                originalLogString = payload;
            }
        } catch (Exception ex) {
            //
        } 
        return originalLogString;
        
    }
    
    private String findContentType(String payload) {
        // Use regex to get the Content-Type and return null if it's not found
        Matcher m = CONTENT_TYPE_PATTERN.matcher(payload);
        return m.find() ? m.group(0) : null;
    }
    
    private String findBoundary(String payload) {
        
        // Use regex to get the boundary and return null if it's not found
        Matcher m = BOUNDARY_PATTERN.matcher(payload);
        return m.find() ? "--" + m.group(1) : null;
    }
}
