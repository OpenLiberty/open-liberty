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
package org.apache.cxf.transport.http.osgi;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Collects configuration information using a ManagedServiceFactory.
 *
 * Registers a HTTPConduitConfigurer that can configure conduits based on the above
 * configuration data.
 *
 * When used with felix config admin and fileinstall the configuration
 * is expected in files named org.apache.cxf.http.conduits-XYZ.cfg
 * that has a list of properties like:
 *
 * url: Regex url to match the configuration
 * order: Integer order in which to apply the regex's when multiple regex's match.
 * client.*
 * tlsClientParameters.*
 * proxyAuthorization.*
 * authorization.*
 *
 * Where each of those is a prefix for the attributes that would be on the elements
 * of the http:conduit configuration defined at:
 *
 * http://cxf.apache.org/schemas/configuration/http-conf.xsd
 *
 * For example:
 * client.ReceiveTimeout: 1000
 * authorization.Username: Foo
 * tlsClientParameters.keyManagers.keyStore.file: mykeys.jks
 * etc....
 *
 */
class ConfigAdminHttpConduitConfigurer implements ManagedServiceFactory, HTTPConduitConfigurer {
    public static final String FACTORY_PID = "org.apache.cxf.http.conduits";

    /**
     * Stores the configuration data index by matcher and sorted by order
     */
    private static class PidInfo implements Comparable<PidInfo> {
        final Dictionary<String, String> props;
        final Matcher matcher;
        final int order;

        PidInfo(Dictionary<String, String> p, Matcher m, int o) {
            matcher = m;
            props = p;
            order = o;
        }
        public Dictionary<String, String> getProps() {
            return props;
        }
        public Matcher getMatcher() {
            return matcher;
        }

        public int compareTo(PidInfo o) {
            if (order < o.order) {
                return -1;
            } else if (order > o.order) {
                return 1;
            }
            // priorities are equal
            if (matcher != null) {
                if (o.matcher == null) {
                    return -1;
                }
                return matcher.pattern().toString().compareTo(o.matcher.pattern().toString());
            }
            return 0;
        }
    }

    Map<String, PidInfo> props
        = new ConcurrentHashMap<>(4, 0.75f, 2);
    CopyOnWriteArrayList<PidInfo> sorted = new CopyOnWriteArrayList<>();

    public String getName() {
        return FACTORY_PID;
    }

    @SuppressWarnings("unchecked")
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties)
        throws ConfigurationException {
        if (pid == null) {
            return;
        }
        deleted(pid);

        String url = (String)properties.get("url");
        String name = (String)properties.get("name");
        Matcher matcher = url == null ? null : Pattern.compile(url).matcher("");
        String p = (String)properties.get("order");
        int order = 50;
        if (p != null) {
            order = Integer.parseInt(p);
        }

        PidInfo info = new PidInfo(properties, matcher, order);

        props.put(pid, info);
        if (url != null) {
            props.put(url, info);
        }
        if (name != null) {
            props.put(name, info);
        }
        addToSortedInfos(info);
    }

    private synchronized void addToSortedInfos(PidInfo pi) {
        int size = sorted.size();
        for (int x = 0; x < size; x++) {
            PidInfo p = sorted.get(x);
            if (pi.compareTo(p) < 0) {
                sorted.add(x, pi);
                return;
            }
        }
        sorted.add(pi);
    }
    private synchronized void removeFromSortedInfos(PidInfo pi) {
        sorted.remove(pi);
    }

    public void deleted(String pid) {
        PidInfo info = props.remove(pid);
        if (info == null) {
            return;
        }
        removeFromSortedInfos(info);
        Dictionary<String, String> d = info.getProps();
        if (d != null) {
            String url = d.get("url");
            String name = d.get("name");
            if (url != null) {
                props.remove(url);
            }
            if (name != null) {
                props.remove(name);
            }
        }
    }

    public void configure(String name, String address, HTTPConduit c) {
        PidInfo byName = null;
        PidInfo byAddress = null;
        if (name != null) {
            byName = props.get(name);
        }
        if (address != null) {
            byAddress = props.get(address);
            if (byAddress == byName) {
                byAddress = null;
            }
        }

        HttpConduitConfigApplier applier = new HttpConduitConfigApplier();
        for (PidInfo info : sorted) {
            if (info.getMatcher() != null
                && info != byName
                && info != byAddress) {
                Matcher m = info.getMatcher();
                synchronized (m) {
                    m.reset(address);
                    if (m.matches()) {
                        applier.apply(info.getProps(), c, address);
                    }
                }
            }
        }

        if (byAddress != null) {
            applier.apply(byAddress.getProps(), c, address);
        }
        if (byName != null) {
            applier.apply(byName.getProps(), c, address);
        }
    }
}
