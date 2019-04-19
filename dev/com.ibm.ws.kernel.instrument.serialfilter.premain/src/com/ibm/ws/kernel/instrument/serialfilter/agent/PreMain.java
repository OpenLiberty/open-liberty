/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.agent;

import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.PreMainUtil;
import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.ObjectInputStreamClassInjector;
import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.ObjectInputStreamTransformer;
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigFacade;
import com.ibm.ws.kernel.instrument.serialfilter.store.Holder;
//import com.ibm.ws.kernel.instrument.serialfilter.store.Holder;
import com.ibm.ws.kernel.instrument.serialfilter.config.Config;
import com.ibm.ws.kernel.instrument.serialfilter.validators.ValidatorsFacade;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.jar.JarFile;

/**
 * This agent does the following:
 * <ol>
 *     <li>Ensure that ObjectInputStream has been adapted to perform validators when deserializing classes.</li>
 *     <li>Put the factory object in place (in a System property) for ObjectInputStream to initialize correctly.</li>
 *     <li>Force the initialisation of ObjectInputStream.</li>
 *     <li>Clean up the System property.</li>
 * </ol>
 */

public class PreMain {
    /** Note: this property name corresponds with code in some IBM JDKs. It must NEVER be changed. */
    public static void premain(String args, Instrumentation instrumentation) {
        boolean debugEnabled = PreMainUtil.isDebugEnabled();
        if (!PreMainUtil.isBeta() && !PreMainUtil.isEnableAgentPropertySet()) {
            // if it's not beta, do nothing.
            // this implementation should be changed when this will be in the production code.
            if (debugEnabled) {
                System.out.println("The serial filter is not loaded.");
            }
            return;
        }

        ClassFileTransformer transform = null;
        boolean useHolder = false;
        boolean alreadyLoaded = isObjectInputStreamLoaded(instrumentation);
        
        if (ObjectInputStreamClassInjector.injectionNeeded()) {
            // Install the transformer to modify ObjectInputStream.
            if (debugEnabled) {
                System.out.println("Using class file transformer to modify ObjectInputStream.");
            }
            if (alreadyLoaded) {
                // if ObjectInputStream class is already loaded, try to retransform.
                // if retransformation does not occur, the execution will fail later. therefore, no error check here.
                transform = new ObjectInputStreamTransformer(true);
                instrumentation.addTransformer(transform, true);
                if (debugEnabled) {
                    System.out.println("ObjectInputStreamTransformer for retransform is set : " + transform);
                }
                try {
                    instrumentation.retransformClasses(ObjectInputStream.class);
                } catch (UnmodifiableClassException e) {
                    if (debugEnabled) {
                        System.out.println("UnmodifiableClassException is caught.");
                    }
                }
                useHolder = true;
            } else {
                transform = new ObjectInputStreamTransformer(false);
                instrumentation.addTransformer(transform);
                if (debugEnabled) {
                    System.out.println("ObjectInputStreamTransformer is set : " + transform);
                }
            }
        }
        try {
            initialiseObjectInputStream(useHolder, instrumentation);    
        } finally {
            // Uninstall the class transformer.
            if (transform != null) {
                if (debugEnabled) {
                    System.out.println("Removing class file transformer.");
                }
                instrumentation.removeTransformer(transform);
            }
        }
        AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.setProperty(PreMainUtil.KEY_SERIALFILTER_AGENT_ACTIVE, "true");
            }
        });

        if (PreMainUtil.isMessageEnabled()) {
            System.out.println(MessageFormat.format(ResourceBundle.getBundle("com.ibm.ws.kernel.instrument.serialfilter.agent.internal.resources.SerialFilterAgentMessages").getString("SFA_INFO_AGENT_LOADED"), ""));
        }
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        if (PreMainUtil.isDebugEnabled()) {
            System.out.println("agentmain was called");
        }
        premain(args, instrumentation);
    }

    private static void initialiseObjectInputStream(boolean useHolder, Instrumentation instrumentation) {
        boolean debugEnabled = PreMainUtil.isDebugEnabled();
        if (debugEnabled) {
            System.out.println("Entering initializeObjectInputStream. useHolder : " + useHolder);
        }
        // Ensure the factory instance needed by the modified ObjectInputStream is in place.
        final Config config = ConfigFacade.createConfig();
        final Map<?,?> validatorFactory = ValidatorsFacade.createFactory(config);
        if (debugEnabled) {
            System.out.println("Inserting validator factory instance into system property: " + validatorFactory);
        }
        final Object oldVal = System.getProperties().put(PreMainUtil.FACTORY_INIT_PROPERTY, validatorFactory);
        if (debugEnabled) {
            System.out.println("Saving old value stored in system property: " + PreMainUtil.FACTORY_INIT_PROPERTY + "=" + oldVal );
        }
        try {
            if (useHolder) {
                addJarToBootStrapClassLoader("ws-serialfilterstore.jar", instrumentation);
                if (debugEnabled) {
                    System.out.println("ws-serialfilterstore.jar is loaded to the bootstrap classloader.");
                }
                Class<?> holder = Holder.class;
                Field factoryField = holder.getDeclaredField(Holder.FACTORY_FIELD);
                factoryField.setAccessible(true);
                factoryField.set(null, validatorFactory);
                if (debugEnabled) {
                    System.out.println("The factory class is set to Holder class");
                }
            } else {
                Field factory = ObjectInputStream.class.getDeclaredField("serializationValidatorFactory");
                factory.setAccessible(true);
                int modifiers = factory.getModifiers();
                if (Modifier.isFinal(modifiers)) {
                    try {
                        Field modifierField = factory.getClass().getDeclaredField("modifiers");
                        modifierField.setAccessible(true);
                        modifierField.setInt(factory, modifiers &~ Modifier.FINAL);
                    } catch (NoSuchFieldException expectedForNonIbmJava) {
                        if (debugEnabled) {
                            System.out.println("Caught NoSuchFieldException while accessing ObjectInputStream modifiers field from agent which is expected for non IBM JVM");
                        }
                    }
                } else {
                    if (debugEnabled) {
                        System.out.println("Modifiers does not set as final.");
                    }                
                }
                factory.set(null, validatorFactory);
                if (debugEnabled) {
                    System.out.println("Forcing ObjectInputStream initialisation.");
                }
            }
            // Force ObjectInputStream.<clinit>() to run!
            try {
                new ObjectInputStream(null);
            } catch (NullPointerException ignored) {
            } catch (IOException ignored) {
            }
        } catch (IllegalAccessException unexpected) {
            if (debugEnabled) {
                System.out.println("Caught unexpected IllegalAccessException while accessing ObjectInputStream fields from agent" + unexpected);
            }
        } catch (Exception unexpected) {
            if (debugEnabled) {
                System.out.println("Caught unexpected exception while accessing ObjectInputStream fields from agent" + unexpected);
            }
        } finally {
            if (oldVal == null) {
                if (debugEnabled) {
                    System.out.println("Removing validator factory from system property: " + PreMainUtil.FACTORY_INIT_PROPERTY);
                }
                System.getProperties().remove(PreMainUtil.FACTORY_INIT_PROPERTY);
            } else {
                if (debugEnabled) {
                    System.out.println("Restoring previous value of system property: " + PreMainUtil.FACTORY_INIT_PROPERTY + "=" + oldVal);
                }
                System.getProperties().put(PreMainUtil.FACTORY_INIT_PROPERTY, oldVal);
            }
        }
        if (!useHolder) {
            assert ObjectInputStreamClassInjector.hasModified(ObjectInputStream.class);
        }
    }

    private static boolean isObjectInputStreamLoaded(Instrumentation instrumentation) {
        boolean debugEnabled = PreMainUtil.isDebugEnabled();
        boolean isLoaded = false;
        if (debugEnabled) {
            System.out.println("Entering isObjectInputStreamLoaded.");
        }
        Class<?> loadedClasses[] = instrumentation.getAllLoadedClasses();
        if (loadedClasses != null) {
            for(Class<?> clazz : loadedClasses) {
                if ("java.io.ObjectInputStream".equals(clazz.getName())) {
                    isLoaded = true;
                    break;
                }
            }
        }
        if (debugEnabled) {
            System.out.println("Exiting isObjectInputStreamLoaded : " + isLoaded);
        }
        return isLoaded;
    }

    private static void addJarToBootStrapClassLoader(String jarName, Instrumentation instrumentation) throws Exception {
        // The code assumes the jar file exists in the same location as this class is loaded.
        CodeSource bootstrapCodeSource = PreMain.class.getProtectionDomain().getCodeSource();
        URI bootstrapLocationURI = bootstrapCodeSource.getLocation().toURI();
        assert ("file".equals(bootstrapLocationURI.getScheme()));

        // Build target URI relative to our own
        URI uri = bootstrapLocationURI.resolve(jarName);
        JarFile jarFile = new JarFile(new File(uri));
        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
    }
}
