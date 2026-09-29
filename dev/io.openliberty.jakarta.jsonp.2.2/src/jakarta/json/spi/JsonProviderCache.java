/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json.spi;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonException;

/**
 * Provider cache used by {@link JsonProvider#provider()}.

 * <p>{@code CACHE} maps each thread context class loader (TCCL) to a weak reference to the
 * canonical {@link JsonProvider} instance for that loader. Weak values prevent {@code CACHE}
 * from being the sole reason an instance stays alive.
 *
 * <p>{@code ANCHOR} is a {@link ClassValue} that associates each provider {@code Class} with a
 * {@code WeakHashMap<ClassLoader, JsonProvider>}. The map is stored inside the provider's own
 * {@code Class} via {@code Class.classValueMap}, so it is owned by the provider's defining loader,
 * not by a static field. This prevents the JsonProvider from being gc'd while the TCCL still exists.

 */
final class JsonProviderCache {

    private static final Logger LOG = Logger.getLogger(JsonProviderCache.class.getName());

    private static final Map<ClassLoader, WeakReference<JsonProvider>> CACHE = new WeakHashMap<>();

    private static final ClassValue<Map<ClassLoader, JsonProvider>> ANCHOR = new ClassValue<>() {
        @Override
        protected Map<ClassLoader, JsonProvider> computeValue(Class<?> type) {
            return Collections.synchronizedMap(new WeakHashMap<>());
        }
    };

    private JsonProviderCache() {
    }


    static JsonProvider get(ClassLoader cl) {
        synchronized (CACHE) {
            WeakReference<JsonProvider> ref = CACHE.get(cl);
            return (ref != null) ? ref.get() : null;
        }
    }

    /**
     * Stores the discovered instance as the canonical provider for the given class loader and
     * returns it.
     *
     * <p>If a concurrent first caller already anchored an instance for this TCCL, that canonical
     * instance is returned instead of {@code discovered}.
     */
    static JsonProvider put(ClassLoader cl, JsonProvider discovered) {
        Map<ClassLoader, JsonProvider> anchor = ANCHOR.get(discovered.getClass());
        JsonProvider instance;
        synchronized (CACHE) {
            WeakReference<JsonProvider> ref = CACHE.get(cl);
            instance = (ref != null) ? ref.get() : null;
            if (instance == null) {
                instance = anchor.putIfAbsent(cl, discovered);
                if (instance == null) {
                    instance = discovered;
                }
                CACHE.put(cl, new WeakReference<>(instance));
            }
        }
        return instance;
    }

    /**
     * Resolves {@code className} and returns a fresh instance. Used for the system-property
     * override and the platform default fallback paths.
     */
    static JsonProvider newInstance(String className, ClassLoader cl) {
        Class<? extends JsonProvider> type = resolve(className, cl);
        try {
            return type.getConstructor().newInstance();
        } catch (Exception x) {
            throw new JsonException("Provider " + className + " could not be instantiated: " + x, x);
        }
    }

    /**
     * Resolves a provider class name to a {@code Class}, trying {@code cl} first and falling back
     * to the API's own class loader for compatibility with pre-2.2 behavior.
     */
    private static Class<? extends JsonProvider> resolve(String className, ClassLoader cl) {
        Class<?> raw;
        try {
            raw = Class.forName(className, false, cl);
        } catch (ClassNotFoundException x) {
            try {
                // Compatibility: before 2.2 the class was resolved by the API's own loader.
                raw = Class.forName(className, false, JsonProvider.class.getClassLoader());
            } catch (ClassNotFoundException y) {
                JsonException je = new JsonException("Provider " + className + " not found", x);
                je.addSuppressed(y);
                throw je;
            }
        }
        try {
            return raw.asSubclass(JsonProvider.class);
        } catch (ClassCastException x) {
            throw new JsonException("Provider " + className + " is not a " + JsonProvider.class.getName(), x);
        }
    }
}
