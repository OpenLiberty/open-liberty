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

package org.apache.cxf.jaxrs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * The CXF Service implementation which is used
 * to register the JAX-RS endpoint with the runtime.
 */
public class JAXRSServiceImpl extends AbstractAttributedInterceptorProvider implements Service, Configurable {
    private static final long serialVersionUID = 6765400202555126993L;
    private List<ClassResourceInfo> classResourceInfos;
    private DataBinding dataBinding;
    private Executor executor;
    private Invoker invoker;
    private Map<QName, Endpoint> endpoints = new HashMap<QName, Endpoint>();
    private String address;
    private boolean createServiceModel;
    private final QName serviceName;

    public JAXRSServiceImpl(String address, QName qname) {
        this.address = address;
        this.serviceName = qname;
    }

    public JAXRSServiceImpl(List<ClassResourceInfo> cri, QName qname) {
        this.classResourceInfos = cri;
        this.serviceName = qname;
    }

    public JAXRSServiceImpl(List<ClassResourceInfo> cri) {
        this(cri, null);
    }

    public JAXRSServiceImpl(List<ClassResourceInfo> cri, boolean create) {
        this(cri, null);
        createServiceModel = true;
    }

    public void setCreateServiceModel(boolean create) {
        createServiceModel = create;
    }

    @Override
    public String getBeanName() {
        return getName().toString();
    }

    @Override
    public QName getName() {
        if (serviceName != null) {
            return serviceName;
        }
        if (address == null && !classResourceInfos.isEmpty()) { //Liberty check for isEmpty
            Class<?> primaryClass = classResourceInfos.get(0).getServiceClass();
            String ns = PackageUtils.getNamespace(PackageUtils.getPackageName(primaryClass));
            return new QName(ns, primaryClass.getSimpleName());
        } else {
            return new QName(address, "WebClient");
        }
    }

    public List<ClassResourceInfo> getClassResourceInfos() {
        return classResourceInfos;
    }

    @Override
    public List<ServiceInfo> getServiceInfos() {
        if (!createServiceModel) {
            return Collections.emptyList();
        }
        // try to convert to WSDL-centric model so that CXF DataBindings can get initialized
        // might become useful too if we support wsdl2

        // make databindings to use databinding-specific information
        // like @XmlRootElement for ex to select a namespace
        this.put("org.apache.cxf.databinding.namespace", "true");

        List<ServiceInfo> infos = new ArrayList<ServiceInfo>();
        for (ClassResourceInfo cri : classResourceInfos) {
            ServiceInfo si = new ServiceInfo();
            infos.add(si);
            QName qname = JAXRSUtils.getClassQName(cri.getServiceClass());
            si.setName(qname);
            InterfaceInfo inf = new InterfaceInfo(si, qname);
            si.setInterface(inf);
            for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
                Method m = ori.getMethodToInvoke();
                QName oname = new QName(qname.getNamespaceURI(), m.getName());
                OperationInfo oi = inf.addOperation(oname);
                createMessagePartInfo(oi, m.getReturnType(), qname, m, false);
                for (Parameter pm : ori.getParameters()) {

                    if (pm.getType() == ParameterType.REQUEST_BODY) {
                        createMessagePartInfo(oi,
                                              ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()],
                                              qname, m, true);
                    }
                }
            }

        }
        return infos;
    }

    private void createMessagePartInfo(OperationInfo oi, Class<?> type, QName qname, Method m,
                                       boolean input) {
        if (type == void.class || Source.class.isAssignableFrom(type)) {
            return;
        }
        if (InjectionUtils.isPrimitive(type) || Response.class == type) {
            return;
        }
        QName mName = new QName(qname.getNamespaceURI(),
                                (input ? "in" : "out") + m.getName());
        MessageInfo ms = oi.createMessage(mName,
                                          input ? MessageInfo.Type.INPUT : MessageInfo.Type.OUTPUT);
        if (input) {
            oi.setInput("in", ms);
        } else {
            oi.setOutput("out", ms);
        }
        QName mpQName = JAXRSUtils.getClassQName(type);
        MessagePartInfo mpi = ms.addMessagePart(mpQName);
        mpi.setConcreteName(mpQName);
        mpi.setTypeQName(mpQName);
        mpi.setTypeClass(type);
    }

    @Override
    public EndpointInfo getEndpointInfo(QName endpoint) {
        // For WSDL-based services, this is to construct an EndpointInfo
        // (transport, binding, address etc) from WSDL's physical part.
        // not applicable to JAX-RS services.
        return null;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Invoker getInvoker() {
        return invoker;
    }

    @Override
    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public DataBinding getDataBinding() {
        return dataBinding;
    }

    @Override
    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    @Override
    public Map<QName, Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<QName, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void setProperties(Map<String, Object> properties) {
        this.putAll(properties);
    }

    // Liberty change - add for tracing
    @Override
    public String toString() {
        return getName().toString();
    }
}
