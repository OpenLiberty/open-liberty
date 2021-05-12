/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.internal.globalhandler;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Traceable;
import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class RESTfulGlobalHandlerMessageContext implements GlobalHandlerMessageContext, Traceable {

    private final boolean isServerSide;
    private final boolean isOutboundFlow;
    private final Supplier<Collection<String>> propertyNamesGetter;
    private final Function<String, Object> propertyGetter;
    private final BiConsumer<String, Object> propertySetter;
    private final Consumer<String> propertyRemover;
    private final HttpServletRequest servletRequest;
    private final HttpServletResponse servletResponse;

    RESTfulGlobalHandlerMessageContext(boolean isServerSide,
                                       boolean isOutboundFlow, 
                                       Supplier<Collection<String>> propertyNamesGetter, 
                                       Function<String, Object> propertyGetter,
                                       BiConsumer<String, Object> propertySetter, 
                                       Consumer<String> propertyRemover, 
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        this.isServerSide = isServerSide;
        this.isOutboundFlow = isOutboundFlow;
        this.propertyNamesGetter = propertyNamesGetter;
        this.propertyGetter = propertyGetter;
        this.propertySetter = propertySetter;
        this.propertyRemover = propertyRemover;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }
    
    @Override
    public boolean isServerSide() {
        return isServerSide;
    }

    @Override
    public boolean isClientSide() {
        return !isServerSide;
    }

    @Override
    public String getEngineType() {
        return HandlerConstants.ENGINE_TYPE_JAXRS;
    }

    @Override
    public String getFlowType() {
        return isOutboundFlow ? HandlerConstants.FLOW_TYPE_OUT : HandlerConstants.FLOW_TYPE_IN;
    }

    @Override
    public Object getProperty(String name) {
        return propertyGetter.apply(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        propertySetter.accept(name, value);
    }

    @Override
    public Iterator<String> getPropertyNames() {
        return propertyNamesGetter.get().iterator();
    }

    @Override
    public void removeProperty(String name) {
        propertyRemover.accept(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return propertyNamesGetter.get().contains(name);
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return servletRequest;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return servletResponse;
    }

    @Override
    public <T> T adapt(Class<T> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public String toTraceString() {
        return toString(true);
    }

    private String toString(boolean includeProperties) {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("[");
        sb.append("isOutboundFlow:").append(isOutboundFlow).append(" ");
        sb.append("isServerSide:").append(isServerSide).append(" ");
        if (includeProperties) {
            String LS = System.lineSeparator();
            Iterator<String> iter = getPropertyNames();
            while (iter.hasNext()) {
                String name = iter.next();
                sb.append(LS).append(name).append("=").append(getProperty(name));
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
