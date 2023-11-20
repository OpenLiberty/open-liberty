/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testContext")
@ApplicationScoped // Make this a bean so that there's one bean in the archive, otherwise CDI gets disabled and @Inject doesn't work
public class ContextAPIServlet extends FATServlet {

    @Inject Span injectedSpan;
    
    /**
     * Very simple test that we can use a Context
     * {@link Context}
     * {@link ContextKey}
     */
    @Test
    public void testContext() {
        
        Context context = Context.current();
        ContextKey<Object> key = ContextKey.named("MyKey");
        Object value = new Object();
        context = context.with(key, value);
        Object result = context.get(key);
        assertEquals(value, result);
    }
    
    @Test
    public void testStoreInContextOnSpanProxy() {
        Context context = Context.current();
        Context stuffInContext = injectedSpan.storeInContext(context);
        Context contextWithStuff = context.with(injectedSpan);
        assertTrue(contextWithStuff.toString().contains(injectedSpan.toString()));
        assertTrue(stuffInContext.toString().contains(injectedSpan.toString()));
    }

    /**
     * Very simple test that we can use a ImplicitContextKeyed
     * {@link ImplicitContextKeyed}
     */
    @Test
    public void testImplicitContextKeyed() {
        Context context = Context.current();
        MyImplicitContextKeyed value = new MyImplicitContextKeyed();
        context = context.with(value);
        MyImplicitContextKeyed result = MyImplicitContextKeyed.get(context);
        assertEquals(value, result);
    }

    private static class MyImplicitContextKeyed implements ImplicitContextKeyed {
        static ContextKey<MyImplicitContextKeyed> KEY = ContextKey.named("MyKey");

        @Override
        public Context storeInContext(Context context) {
            return context.with(KEY, this);
        }

        public static MyImplicitContextKeyed get(Context context) {
            return context.get(KEY);
        }
    };

    /**
     * Very simple test that we can use a ContextStorage
     * {@link ContextStorage}
     * {@link Scope}
     */
    @Test
    public void testContextStorage() {
        ContextStorage storage = ContextStorage.get();
        Context context = Context.current();
        try (Scope scope = storage.attach(context)) {
            assertNotNull(scope);
            assertNotSame(Scope.noop(), scope);
            Context stored = storage.current();
            assertEquals(context, stored);
        }
    }

    /**
     * Very simple test that we can use a ContextStorageProvider
     * {@link ContextStorageProvider}
     */
    @Test
    public void testContextStorageProvider() {
        ContextStorageProvider provider = new MyContextStorageProvider();
        ContextStorage storage = provider.get();
        Context context = Context.current();
        try (Scope scope = storage.attach(context)) {
            assertNotNull(scope);
            assertEquals(Scope.noop(), scope);
            Context stored = storage.current();
            assertEquals(context, stored);
        }
    }

    private static class MyContextStorageProvider implements ContextStorageProvider {

        /** {@inheritDoc} */
        @Override
        public ContextStorage get() {
            return new ContextStorage() {

                private Context current;

                @Override
                public Scope attach(Context context) {
                    this.current = context;
                    return Scope.noop();
                }

                @Override
                public Context current() {
                    return this.current;
                }

            };
        }

    }

    /**
     * Very simple test that we can use ContextPropagators
     * {@link ContextPropagators}
     * {@link TextMapPropagator}
     * {@link TextMapSetter}
     * {@link TextMapGetter}
     */
    @Test
    public void testContextPropagators() {
        //create initial context and add a value
        Context contextA = Context.current();
        contextA = contextA.with(MY_KEY, MY_VALUE);

        //create a temporary carrier and a TextMapPropagator
        Map<String, String> carrier = new HashMap<>();
        TextMapPropagator textPropagator = new MyTextMapPropagator();

        //inject the carrier with the value from the context
        textPropagator.inject(contextA, carrier, new MyTextMapSetter());
        assertTrue(carrier.containsKey(MY_KEY_STRING));
        assertEquals(MY_VALUE, carrier.get(MY_KEY_STRING));

        //create a ContextPropagators, get the TextMapPropagator back out and check that it contains the field we expect
        ContextPropagators contextPropagators = ContextPropagators.create(textPropagator);
        textPropagator = contextPropagators.getTextMapPropagator();
        assertTrue(textPropagator.fields().contains(MY_KEY_STRING));

        //create a second context, extract the value from the carrier and put it in the context
        Context contextB = Context.current();
        contextB = textPropagator.extract(contextB, carrier, new MyTextMapGetter());
        String value = contextB.get(MY_KEY);

        //check that it still matches
        assertEquals(MY_VALUE, value);
    }

    private static final String MY_VALUE = "myValue";
    private static final String MY_KEY_STRING = "myKey";
    private static final ContextKey<String> MY_KEY = ContextKey.named(MY_KEY_STRING);

    private static class MyTextMapPropagator implements TextMapPropagator {
        @Override
        public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
            String value = context.get(MY_KEY);
            setter.set(carrier, MY_KEY_STRING, value);
        }

        @Override
        public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
            String value = getter.get(carrier, MY_KEY_STRING);
            return context.with(MY_KEY, value);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<String> fields() {
            return Collections.singleton(MY_KEY_STRING);
        }
    }

    private static class MyTextMapSetter implements TextMapSetter<Map<String, String>> {
        /** {@inheritDoc} */
        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }
    }

    private static class MyTextMapGetter implements TextMapGetter<Map<String, String>> {

        /** {@inheritDoc} */
        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }

        /** {@inheritDoc} */
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

    }
}
