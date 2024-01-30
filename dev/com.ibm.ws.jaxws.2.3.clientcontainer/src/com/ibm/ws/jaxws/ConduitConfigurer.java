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
package com.ibm.ws.jaxws;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  Allows us to Apply our HTTPConduit configuration on a HTTPConduit via a ManagedService added to the Bus
 *  Mostly borrowed from
 *  org.apache.cxf.transport.http.asyncclient.Activator
 *  org.apache.cxf.transport.http.osgi.HttpConduitConfigApplier
 */
public class ConduitConfigurer extends ConduitConfigurerService  implements HTTPConduitConfigurer {
    
    //  Liberty Code change
    private static final TraceComponent tc = Tr.register(ConduitConfigurer.class);
    public static final String FACTORY_PID = "org.apache.cxf.http.conduits";
    public ConduitConfigurer(BundleContext context) {
        
        super(context);
        Bus[] buses = (Bus[])getServices();
        if (buses == null) {
            return;
        }
        for (Bus bus : buses) {
            bus.setExtension(this, HTTPConduitConfigurer.class );
        }

    }
    
    //Liberty Code Change
    public ConduitConfigurer() {
        super(JaxwsConduitConfigActivator.context);
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

        // Liberty Code Change
        JaxwsHttpConduitConfigApplier applier = new JaxwsHttpConduitConfigApplier();
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
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
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
    if (matcher != null) { // Liberty Change start
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
          Tr.debug(tc, "Matcher is not null, add to sorted list");
       }
       addToSortedInfos(info);
    } // Liberty Change end
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
}
