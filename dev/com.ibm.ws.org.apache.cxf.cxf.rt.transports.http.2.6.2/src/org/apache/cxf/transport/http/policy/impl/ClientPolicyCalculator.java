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
package org.apache.cxf.transport.http.policy.impl;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.policy.PolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ObjectFactory;

public class ClientPolicyCalculator implements PolicyCalculator<HTTPClientPolicy> {


    /**
     * Determines if two HTTPClientPolicy objects are equal. REVISIT: Check if
     * this can be replaced by a generated equals method.
     * 
     * @param p1 one client policy
     * @param p2 another client policy
     * @return true iff the two policies are equal
     */
    public boolean equals(HTTPClientPolicy p1, HTTPClientPolicy p2) {
        if (p1 == p2) {
            return true;
        }
        boolean result = true;
        result &= (p1.isAllowChunking() == p2.isAllowChunking())
                  && (p1.isAutoRedirect() == p2.isAutoRedirect())
                  && StringUtils.equals(p1.getAccept(), p2.getAccept())
                  && StringUtils.equals(p1.getAcceptEncoding(), p2.getAcceptEncoding())
                  && StringUtils.equals(p1.getAcceptLanguage(), p2.getAcceptLanguage())
                  && StringUtils.equals(p1.getBrowserType(), p2.getBrowserType());
        if (!result) {
            return false;
        }

        result &= (p1.getCacheControl() == null ? p2.getCacheControl() == null : p1.getCacheControl()
            .equals(p2.getCacheControl()) && p1.getConnection().value().equals(p2.getConnection().value()))
                  && (p1.getConnectionTimeout() == p2.getConnectionTimeout())
                  && StringUtils.equals(p1.getContentType(), p2.getContentType())
                  && StringUtils.equals(p1.getCookie(), p2.getCookie())
                  && StringUtils.equals(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint())
                  && StringUtils.equals(p1.getHost(), p2.getHost());
        if (!result) {
            return false;
        }

        result &= StringUtils.equals(p1.getProxyServer(), p2.getProxyServer())
                  && (p1.isSetProxyServerPort() ? p1.getProxyServerPort() == p2.getProxyServerPort() : !p2
                      .isSetProxyServerPort())
                  && p1.getProxyServerType().value().equals(p2.getProxyServerType().value())
                  && (p1.getReceiveTimeout() == p2.getReceiveTimeout())
                  && StringUtils.equals(p1.getReferer(), p2.getReferer());

        return result;
    }

    /**
     * Returns a new HTTPClientPolicy that is compatible with the two specified
     * policies or null if no compatible policy can be determined.
     * 
     * @param p1 one policy
     * @param p2 another policy
     * @return the compatible policy
     */
    public HTTPClientPolicy intersect(HTTPClientPolicy p1, HTTPClientPolicy p2) {

        // incompatibilities

        if (!compatible(p1, p2)) {
            return null;
        }

        // ok - compute compatible policy

        HTTPClientPolicy p = new HTTPClientPolicy();
        p.setAccept(StringUtils.combine(p1.getAccept(), p2.getAccept()));
        p.setAcceptEncoding(StringUtils.combine(p1.getAcceptEncoding(), p2.getAcceptEncoding()));
        p.setAcceptLanguage(StringUtils.combine(p1.getAcceptLanguage(), p2.getAcceptLanguage()));
        if (p1.isSetAllowChunking()) {
            p.setAllowChunking(p1.isAllowChunking());
        } else if (p2.isSetAllowChunking()) {
            p.setAllowChunking(p2.isAllowChunking());
        }
        if (p1.isSetAutoRedirect()) {
            p.setAutoRedirect(p1.isAutoRedirect());
        } else if (p2.isSetAutoRedirect()) {
            p.setAutoRedirect(p2.isAutoRedirect());
        }
        p.setBrowserType(StringUtils.combine(p1.getBrowserType(), p2.getBrowserType()));
        if (p1.isSetCacheControl()) {
            p.setCacheControl(p1.getCacheControl());
        } else if (p2.isSetCacheControl()) {
            p.setCacheControl(p2.getCacheControl());
        }
        if (p1.isSetConnection()) {
            p.setConnection(p1.getConnection());
        } else if (p2.isSetConnection()) {
            p.setConnection(p2.getConnection());
        }
        if (p1.isSetContentType()) {
            p.setContentType(p1.getContentType());
        } else if (p2.isSetContentType()) {
            p.setContentType(p2.getContentType());
        }
        p.setCookie(StringUtils.combine(p1.getCookie(), p2.getCookie()));
        p.setDecoupledEndpoint(StringUtils.combine(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint()));
        p.setHost(StringUtils.combine(p1.getHost(), p2.getHost()));
        p.setProxyServer(StringUtils.combine(p1.getProxyServer(), p2.getProxyServer()));
        if (p1.isSetProxyServerPort()) {
            p.setProxyServerPort(p1.getProxyServerPort());
        } else if (p2.isSetProxyServerPort()) {
            p.setProxyServerPort(p2.getProxyServerPort());
        }
        if (p1.isSetProxyServerType()) {
            p.setProxyServerType(p1.getProxyServerType());
        } else if (p2.isSetProxyServerType()) {
            p.setProxyServerType(p2.getProxyServerType());
        }
        p.setReferer(StringUtils.combine(p1.getReferer(), p2.getReferer()));
        if (p1.isSetConnectionTimeout()) {
            p.setConnectionTimeout(p1.getConnectionTimeout());
        } else if (p2.isSetConnectionTimeout()) {
            p.setConnectionTimeout(p2.getConnectionTimeout());
        }
        if (p1.isSetReceiveTimeout()) {
            p.setReceiveTimeout(p1.getReceiveTimeout());
        } else if (p2.isSetReceiveTimeout()) {
            p.setReceiveTimeout(p2.getReceiveTimeout());
        }

        return p;
    }

    /**
     * Checks if two HTTPClientPolicy objects are compatible.
     * 
     * @param p1 one client policy
     * @param p2 another client policy
     * @return true iff policies are compatible
     */
    public boolean compatible(HTTPClientPolicy p1, HTTPClientPolicy p2) {

        if (p1 == p2 || p1.equals(p2)) {
            return true;
        }

        boolean compatible = true;

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getAccept(), p2.getAccept());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getAcceptEncoding(), p2.getAcceptEncoding());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getAcceptLanguage(), p2.getAcceptLanguage());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getBrowserType(), p2.getBrowserType());
        }

        if (compatible) {
            compatible &= !p1.isSetCacheControl() || !p2.isSetCacheControl()
                          || p1.getCacheControl().equals(p2.getCacheControl());
        }

        if (compatible) {
            compatible = !p1.isSetConnection() || !p2.isSetConnection()
                         || p1.getConnection().value().equals(p2.getConnection().value());
        }

        if (compatible) {
            compatible = !p1.isSetContentType() || !p2.isSetContentType()
                         || p1.getContentType().equals(p2.getContentType());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getCookie(), p2.getCookie());
        }

        // REVISIT: Should compatibility require strict equality?

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getHost(), p2.getHost());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getProxyServer(), p2.getProxyServer());
        }

        if (compatible) {
            compatible &= !p1.isSetProxyServerPort() || !p2.isSetProxyServerPort()
                          || p1.getProxyServerPort() == p2.getProxyServerPort();
        }

        if (compatible) {
            compatible &= !p1.isSetProxyServerType() || !p2.isSetProxyServerType()
                          || p1.getProxyServerType().equals(p2.getProxyServerType());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getReferer(), p2.getReferer());
        }

        if (compatible) {
            compatible &= p1.isAllowChunking() == p2.isAllowChunking();
        }

        if (compatible) {
            compatible &= p1.isAutoRedirect() == p2.isAutoRedirect();
        }

        return compatible;
    }
    
    public boolean isAsserted(Message message, HTTPClientPolicy policy, HTTPClientPolicy refPolicy) {
        boolean outbound = MessageUtils.isOutbound(message);
        boolean compatible = compatible(policy, refPolicy);
        return !outbound || compatible;
    }

    public Class<HTTPClientPolicy> getDataClass() {
        return HTTPClientPolicy.class;
    }

    public QName getDataClassName() {
        return new ObjectFactory().createClient(null).getName();
    }
    
    public static String toString(HTTPClientPolicy p) {
        StringBuilder buf = new StringBuilder();
        buf.append(p);
        buf.append("[DecoupledEndpoint=\"");
        buf.append(p.getDecoupledEndpoint());
        buf.append("\", ReceiveTimeout=");
        buf.append(p.getReceiveTimeout());
        buf.append("])");
        return buf.toString();
    }
}
