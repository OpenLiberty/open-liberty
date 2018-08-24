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

package org.apache.cxf.service.model;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

/**
 * The EndpointInfo contains the information for a web service 'port' inside of a service.
 */
public class EndpointInfo extends AbstractDescriptionElement implements NamedItem {
    String transportId;
    ServiceInfo service;
    BindingInfo binding;
    QName name;
    private volatile EndpointReferenceType lastAddressSet; //Liberty: Maintain last address set for when threadlocal value may be null.
    
    // Liberty #3669:  Store address in a theadLocal to avoid issue where redirected URL is mismatched when accessed 
    // from both IP address and machine name.
    private static ThreadLocal<EndpointReferenceType> threadLocal = new ThreadLocal<EndpointReferenceType>();

    public EndpointInfo() {
    }
    
    public EndpointInfo(ServiceInfo serv, String ns) {
        transportId = ns;
        service = serv;
    }
    
    public DescriptionInfo getDescription() {
        if (service == null) {
            return null;
        }
        return service.getDescription();
    }

    
    public String getTransportId() {
        return transportId;
    }    
    
    public void setTransportId(String tid) {
        transportId = tid;
    }
    
    public InterfaceInfo getInterface() {
        if (service == null) {
            return null;
        }
        return service.getInterface();
    }
    
    public void setService(ServiceInfo s) {
        service = s;
    }
    public ServiceInfo getService() {
        return service;
    }
    
    public QName getName() {
        return name;
    }
    
    public void setName(QName n) {
        name = n;
    }

    public BindingInfo getBinding() {
        return binding;
    }
    
    public void setBinding(BindingInfo b) {
        binding = b;
    }    
    
    public String getAddress() {
        EndpointReferenceType address = threadLocal.get(); //Liberty #3669
        if (address == null) {
            address = lastAddressSet; //Liberty
        }
        return (null != address && null != address.getAddress()) ? address.getAddress().getValue() : null;
    }
    
    public void setAddress(String addr) {
        EndpointReferenceType address = threadLocal.get();  //Liberty #3669
        if (null == address) {
            address = EndpointReferenceUtils.getEndpointReference(addr);
            threadLocal.set(address); //Liberty #3669
        } else {
            EndpointReferenceUtils.setAddress(address, addr);
        }
        lastAddressSet = address; //Liberty 
    }
    
    public void setAddress(EndpointReferenceType endpointReference) {
        threadLocal.set(endpointReference);  //Liberty #3669
        lastAddressSet = endpointReference; //Liberty
    }
    
    //When finished the threadlocal must be cleared. //Liberty #3669
    public void resetAddress() {
        threadLocal.set(null); //Liberty #3669
    }
    
    @Override
    public <T> T getTraversedExtensor(T defaultValue, Class<T> type) {
        T value = getExtensor(type);
        
        if (value == null) {
            if (binding != null) {
                value = binding.getExtensor(type);
            }
            
            if (service != null && value == null) {
                value = service.getExtensor(type);
            }
            
            if (value == null) {
                value = defaultValue;
            }
        }
        
        return value;
    }

    public EndpointReferenceType getTarget() {
        EndpointReferenceType address = threadLocal.get();   //Liberty #3669
        if (address == null) {
            address = lastAddressSet;   //Liberty
        }
        return address;
    }

    public boolean isSameAs(EndpointInfo epInfo) {
        if (this == epInfo) {
            return true;
        }
        if (epInfo == null) {
            return false;
        }
        return binding.getName().equals(epInfo.binding.getName()) 
            && service.getName().equals(epInfo.service.getName()) 
            && name.equals(epInfo.name);
    }

    public String toString() {
        return "BindingQName=" + (binding == null ? "" : (binding.getName()
                + ", ServiceQName=" + (binding.getService() == null ? "" : binding.getService().getName())))
                + ", QName=" + name;
    }
}
