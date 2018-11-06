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
package com.ibm.ws.kernel.instrument.serialization.agent;

import com.ibm.ws.kernel.instrument.serialization.config.ConfigFacade;
import com.ibm.ws.kernel.instrument.serialization.config.Config;
import com.ibm.ws.kernel.instrument.serialization.validators.ValidatorsFacade;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static java.util.logging.Level.FINE;

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
    private static final Logger log = Logger.getLogger(PreMain.class.getName());
    /** Note: this property name corresponds with code in some IBM JDKs. It must NEVER be changed. */
    public static final String FACTORY_INIT_PROPERTY = "com.ibm.serialization.validators.factory.instance";
    public static void premain(String args, Instrumentation instrumentation) {
        ObjectInputStreamTransformer transform = null;
        if (ObjectInputStreamClassInjector.injectionNeeded()) {
            // Install the transformer to modify ObjectInputStream.
            log.config("Using class file transformer to modify ObjectInputStream.");
            transform = new ObjectInputStreamTransformer();
            instrumentation.addTransformer(transform);
        }
        try {
            initialiseObjectInputStream();    
        } finally {
            // Uninstall the class transformer.
            if (transform != null) {
                log.finer("Removing class file transformer.");
                instrumentation.removeTransformer(transform);
            }
        }
    }

    private static void initialiseObjectInputStream() {
        // Ensure the factory instance needed by the modified ObjectInputStream is in place.
        final Config config = ConfigFacade.createConfig();
        final Object validatorFactory = ValidatorsFacade.createFactory(config);
        if (log.isLoggable(FINE)) log.fine("Inserting validator factory instance into system property: " + FACTORY_INIT_PROPERTY);
        final Object oldVal = System.getProperties().put(FACTORY_INIT_PROPERTY, validatorFactory);
        if (log.isLoggable(FINE)) log.fine("Saving old value stored in system property: " + FACTORY_INIT_PROPERTY + "=" + oldVal );
        try {
            Class<?> oisc = ObjectInputStream.class;
            Field factory = ObjectInputStream.class.getDeclaredField("serializationValidatorFactory");
            factory.setAccessible(true);
            int modifiers = factory.getModifiers();
            Field modifierField = factory.getClass().getDeclaredField("modifiers");
            modifierField.setAccessible(true);
            modifierField.setInt(factory, modifiers	&~ Modifier.FINAL);
            factory.set(null, validatorFactory);
            log.fine("Forcing ObjectInputStream initialisation.");
            // Force ObjectInputStream.<clinit>() to run!
            try {
                new ObjectInputStream(null);
            } catch (NullPointerException ignored) {
            } catch (IOException ignored) {
            }

        } catch (NoSuchFieldException expectedForNonIbmJava) {
        } catch (IllegalAccessException unexpected) {
            log.info("Caught unexpected exception while accessing ObjectInputStream fields from agent" + unexpected);
        } finally {
            if (oldVal == null) {
                if (log.isLoggable(FINE))log.fine("Removing validator factory from system property: " + FACTORY_INIT_PROPERTY);
                System.getProperties().remove(FACTORY_INIT_PROPERTY);
            } else {
                if (log.isLoggable(FINE))log.fine("Restoring previous value of system property: " + FACTORY_INIT_PROPERTY + "=" + oldVal);
                System.getProperties().put(FACTORY_INIT_PROPERTY, oldVal);
            }
        }
        assert ObjectInputStreamClassInjector.hasModified(ObjectInputStream.class);
    }

}
