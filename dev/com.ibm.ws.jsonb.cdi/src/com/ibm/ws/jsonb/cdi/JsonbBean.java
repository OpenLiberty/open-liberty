/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class JsonbBean implements Bean<Jsonb>, PassivationCapable {

    private static final TraceComponent tc = Tr.register(JsonbBean.class);

    static final Set<Type> types = new HashSet<Type>(Arrays.asList(Object.class, Jsonb.class));
    static final Set<Annotation> qualifiers = new HashSet<Annotation>(Arrays.asList(Default.Literal.INSTANCE, Any.Literal.INSTANCE));

    private final JsonbProvider jsonbProvider;
    private final JsonProvider jsonpProvider;

    public JsonbBean(JsonbProvider jsonbProivder, JsonProvider jsonpProvider) {
        this.jsonbProvider = jsonbProivder;
        this.jsonpProvider = jsonpProvider;
    }

    @Override
    public Jsonb create(CreationalContext<Jsonb> ctx) {
        return jsonbProvider.create().withProvider(jsonpProvider).build();
    }

    @Override
    public void destroy(Jsonb jsonb, CreationalContext<Jsonb> ctx) {
        try {
            jsonb.close();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Error closing Jsonb object", jsonb, e.getMessage());
            // auto-ffdc
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return Jsonb.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public String getId() {
        return getClass().getCanonicalName() + '-' + hashCode();
    }

}
