/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.util.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_Exception;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class UtilImpl_Factory implements Util_Factory {
    private static final TraceComponent tc = Tr.register(UtilImpl_Factory.class);
    public static final String CLASS_NAME = UtilImpl_Factory.class.getName();

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_Factory() {
        super();

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Created", this.hashText));
        }
    }

    //

    public Util_Exception newUtilException(TraceComponent logger, String message) {
        Util_Exception exception = new Util_Exception(message);

        if (logger.isEventEnabled()) {
            Tr.event(logger, exception.getMessage(), exception);
        }

        return exception;
    }

    //

    @Override
    public Set<String> createIdentityStringSet() {
        return Collections.newSetFromMap(new IdentityHashMap<String, Boolean>());
    }

    @Override
    public UtilImpl_InternMap createInternMap(ValueType valueType, String name) {
        return new UtilImpl_InternMap(this, valueType, name);
    }

    @Override
    public UtilImpl_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                            ValueType heldType, String heldTag) {
        return createBidirectionalMap(holderType, holderTag,
                                      heldType, heldTag,
                                      Util_BidirectionalMap.IS_ENABLED);
    }

    @Override
    public UtilImpl_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                            ValueType heldType, String heldTag,
                                                            boolean isEnabled) {

        UtilImpl_InternMap heldInternMap = createInternMap(holderType, holderTag);
        UtilImpl_InternMap holderInternMap = createInternMap(heldType, heldTag);

        return createBidirectionalMap(heldTag, heldInternMap, holderTag, holderInternMap, isEnabled);
    }

    //

    public UtilImpl_BidirectionalMap createBidirectionalMap(String holderTag, UtilImpl_InternMap holderInternMap,
                                                            String heldTag, UtilImpl_InternMap heldInternMap) {
        return createBidirectionalMap(holderTag, holderInternMap,
                                      heldTag, heldInternMap,
                                      Util_BidirectionalMap.IS_ENABLED);
    }

    public UtilImpl_BidirectionalMap createBidirectionalMap(String holderTag, Util_InternMap holderInternMap,
                                                            String heldTag, Util_InternMap heldInternMap,
                                                            boolean isEnabled) {
        return new UtilImpl_BidirectionalMap(this, holderTag, heldTag, holderInternMap, heldInternMap, isEnabled);
    }

}