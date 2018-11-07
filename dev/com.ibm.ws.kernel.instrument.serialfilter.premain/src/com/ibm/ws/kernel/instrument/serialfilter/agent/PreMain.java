/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.ws.kernel.instrument.serialfilter.config.Config;
import com.ibm.ws.kernel.instrument.serialfilter.validators.ValidatorsFacade;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ResourceBundle;

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
        if (!PreMainUtil.isBeta() && !PreMainUtil.isEnableAgentPropertySet()) {
            // if it's not beta, do nothing.
            // this implementation should be changed when this will be in the production code.
            return;
        }
        ObjectInputStreamTransformer transform = null;
        if (ObjectInputStreamClassInjector.injectionNeeded()) {
            // Install the transformer to modify ObjectInputStream.
            if (PreMainUtil.isDebugEnabled()) {
                System.out.println("Using class file transformer to modify ObjectInputStream.");
            }
            transform = new ObjectInputStreamTransformer();
            instrumentation.addTransformer(transform);
        }
        try {
            initialiseObjectInputStream();    
        } finally {
            // Uninstall the class transformer.
            if (transform != null) {
                if (PreMainUtil.isDebugEnabled()) {
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
        System.out.println(MessageFormat.format(ResourceBundle.getBundle("com.ibm.ws.kernel.instrument.serialfilter.agent.internal.resources.SerialFilterAgentMessages").getString("SFA_INFO_AGENT_LOADED"), ""));

    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        if (PreMainUtil.isDebugEnabled()) {
            System.out.println("agentmain was called");
        }
        premain(args, instrumentation);
    }

    private static void initialiseObjectInputStream() {
        // Ensure the factory instance needed by the modified ObjectInputStream is in place.
        final Config config = ConfigFacade.createConfig();
        final Object validatorFactory = ValidatorsFacade.createFactory(config);
        boolean debugEnabled = PreMainUtil.isDebugEnabled();
        if (debugEnabled) {
            System.out.println("Inserting validator factory instance into system property: " + PreMainUtil.FACTORY_INIT_PROPERTY);
        }
        final Object oldVal = System.getProperties().put(PreMainUtil.FACTORY_INIT_PROPERTY, validatorFactory);
        if (debugEnabled) {
            System.out.println("Saving old value stored in system property: " + PreMainUtil.FACTORY_INIT_PROPERTY + "=" + oldVal );
        }
        try {
            Class<?> oisc = ObjectInputStream.class;
            Field factory = ObjectInputStream.class.getDeclaredField("serializationValidatorFactory");
            factory.setAccessible(true);
            int modifiers = factory.getModifiers();
            Field modifierField = factory.getClass().getDeclaredField("modifiers");
            modifierField.setAccessible(true);
            modifierField.setInt(factory, modifiers	&~ Modifier.FINAL);
            factory.set(null, validatorFactory);
            if (debugEnabled) {
                System.out.println("Forcing ObjectInputStream initialisation.");
            }
            // Force ObjectInputStream.<clinit>() to run!
            try {
                new ObjectInputStream(null);
            } catch (NullPointerException ignored) {
            } catch (IOException ignored) {
            }

        } catch (NoSuchFieldException expectedForNonIbmJava) {
        } catch (IllegalAccessException unexpected) {
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
        assert ObjectInputStreamClassInjector.hasModified(ObjectInputStream.class);
    }
}
