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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonException;

/**
 * Provider cache used by {@link JsonProvider#provider()}.
 *
 * <p>{@code CLASSLOADER_CACHE} maps a thread context classloader to a weak reference to the
 * resolved provider {@code Class}. Weak keys allow entries to be evicted when the classloader is
 * collected; weak values refer to the provider {@code Class}, which is strongly owned by its
 * defining classloader ({@code ClassLoader.classes}) and therefore lives exactly as long as
 * that loader.
 *
 * <p>The provider instance is stored by {@code INSTANCES}, a {@code ClassValue} that anchors
 * exactly one {@link JsonProvider} instance inside each provider {@code Class}'s own internal map
 * ({@code Class.classValueMap}). The result is that the entire graph (loader + class + instance) is
 * collected as a unit when the container drops the loader.
 *
 * <p>All accesses to {@code CLASSLOADER_CACHE} are guarded by {@code synchronized} blocks.
 * {@code ClassValue.get} is lock-free after the first computation.
 */
final class JsonProviderCache {

    private static final Logger LOG = Logger.getLogger(JsonProviderCache.class.getName());

    /** TCCL → weak reference to the resolved provider Class. */
    private static final Map<ClassLoader, WeakReference<Class<? extends JsonProvider>>> CLASSLOADER_CACHE =
            new WeakHashMap<>();

    /** provider Class → singleton instance, stored inside the Class itself via {@code Class.classValueMap}. */
    private static final ClassValue<JsonProvider> INSTANCES = new ClassValue<>() {
        @Override
        protected JsonProvider computeValue(Class<?> type) {
            return instantiate(type.asSubclass(JsonProvider.class));
        }
    };

    private JsonProviderCache() {
    }

    /**
     * Returns the cached provider instance for the given classloader, or {@code null} on a miss.
     */
    static JsonProvider get(ClassLoader cl) {
        Class<? extends JsonProvider> type;
        synchronized (CLASSLOADER_CACHE) {
            WeakReference<Class<? extends JsonProvider>> ref = CLASSLOADER_CACHE.get(cl);
            type = (ref != null) ? ref.get() : null;
        }
        return (type != null) ? INSTANCES.get(type) : null;
    }

    /**
     * Stores the provider class for the given classloader and returns the canonical shared instance.
     * If the provider is not re-instantiable by the cache rules, returns {@code null} and leaves
     * the entry uncached (the caller should return the already-discovered instance as-is).
     */
    static JsonProvider put(ClassLoader cl, JsonProvider discovered) {
        Class<? extends JsonProvider> type = discovered.getClass();
        JsonProvider shared;
        try {
            shared = INSTANCES.get(type);
        } catch (JsonException e) {
            LOG.log(Level.FINE, "Provider " + type.getName() + " is not cacheable", e);
            return null;
        }
        synchronized (CLASSLOADER_CACHE) {
            WeakReference<Class<? extends JsonProvider>> existing = CLASSLOADER_CACHE.get(cl);
            if (existing == null || existing.get() == null) {
                CLASSLOADER_CACHE.put(cl, new WeakReference<>(type));
            }
        }
        return shared;
    }

    /**
     * Resolves a provider class name and instantiates it directly, bypassing {@code INSTANCES}.
     * Used for the system-property override path in {@link JsonProvider#provider()}.
     */
    static JsonProvider getForClassName(String className, ClassLoader cl) {
        return instantiate(resolve(className, cl));
    }

    /**
     * Resolves the platform default provider class and returns its canonical shared instance
     * from {@code INSTANCES}. Convenience for the final fallback in discovery; keeps
     * {@link #resolve} and {@link #instantiate} private.
     */
    static JsonProvider instantiateDefault(String className, ClassLoader cl) {
        return INSTANCES.get(resolve(className, cl));
    }

    /**
     * Resolves a provider class name to a {@code Class}, trying {@code cl} first and falling back
     * to the API's own classloader for compatibility with pre-2.2 behaviour.
     */
    private static Class<? extends JsonProvider> resolve(String className, ClassLoader cl) {
        try {
            Class<?> raw = Class.forName(className, false, cl);
            Class<? extends JsonProvider> typed = raw.asSubclass(JsonProvider.class);
            Class.forName(className, true, cl);
            return typed;
        } catch (ClassNotFoundException x) {
            try {
                // Compatibility: before 2.2 the class was resolved by the API's own loader.
                Class<?> raw = Class.forName(className, false, JsonProvider.class.getClassLoader());
                Class<? extends JsonProvider> typed = raw.asSubclass(JsonProvider.class);
                Class.forName(className, true, JsonProvider.class.getClassLoader());
                return typed;
            } catch (ClassNotFoundException y) {
                JsonException je = new JsonException("Provider " + className + " not found", x);
                je.addSuppressed(y);
                throw je;
            }
        } catch (ClassCastException x) {
            throw new JsonException("Provider " + className + " is not a " + JsonProvider.class.getName(), x);
        }
    }

    /**
     * Instantiates the given provider class. A {@code public static provider()} factory method
     * declared directly on the class takes precedence over the public no-arg constructor.
     * Uses {@code getDeclaredMethod} (not {@code getMethod}) to avoid finding the inherited
     * static {@link JsonProvider#provider()} and recursing.
     */
    private static JsonProvider instantiate(Class<? extends JsonProvider> type) {
        try {
            try {
                Method factory = type.getDeclaredMethod("provider");
                int mods = factory.getModifiers();
                if (Modifier.isPublic(mods) && Modifier.isStatic(mods)
                        && JsonProvider.class.isAssignableFrom(factory.getReturnType())) {
                    return (JsonProvider) factory.invoke(null);
                }
            } catch (NoSuchMethodException ignored) {
            }
            return type.getConstructor().newInstance();
        } catch (Exception x) {
            throw new JsonException("Provider " + type.getName() + " could not be instantiated: " + x, x);
        }
    }
}
