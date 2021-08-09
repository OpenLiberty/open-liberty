/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.passivator;

import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.ejs.container.passivator.PassivatorSerializable;
import com.ibm.ejs.container.passivator.PassivatorSerializableHandle;
import com.ibm.ejs.container.util.StatefulBeanOReplacement;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationObjectReplacer;

/**
 * This class implements both {@link SerializationObjectReplacer} and {@link DeserializationObjectResolver},
 * used for serialization and deserialization of stateful session beans that need special handling.
 */
public class StatefulPassivatorSerializationHandler implements SerializationObjectReplacer, DeserializationObjectResolver {

    /**
     * Represents the bean that is to be activated.
     * 
     * This is used for deserialization of a bean, so will be null if being
     * used only for serialization.
     */
    private final BeanO activatedBeanO;

    /**
     * Used for serialization of a stateful session bean.
     */
    public StatefulPassivatorSerializationHandler() {
        this(null);
    }

    /**
     * Used for deserialization of a stateful session bean.
     * 
     * @param beanO bean that is to be deserialized
     */
    public StatefulPassivatorSerializationHandler(BeanO beanO) {
        activatedBeanO = beanO;
    }

    @Override
    public Object replaceObject(@Sensitive Object object) {

        // -----------------------------------------------------------------------
        // StatefulBeanO (the SessionContext) does not need to be serialized
        // since activation of SFSB will create a new StatefulBeanO to use as
        // the SessionContext.  So replace it with a dummy StatefulBeanOReplacement
        // object. It will be converted back on input.
        // -----------------------------------------------------------------------
        if (object instanceof StatefulBeanO) {
            return new StatefulBeanOReplacement();
        }

        // -----------------------------------------------------------------------
        // Some objects are not directly serializable, but passivating a SFSB that
        // contains that object must be supported, so replace it with a
        // serializable object. It will be converted back on input.
        // -----------------------------------------------------------------------
        if (object instanceof PassivatorSerializable) {
            return ((PassivatorSerializable) object).getSerializableObject();
        }

        return null;
    }

    @Override
    public Object resolveObject(@Sensitive Object object) {

        // -----------------------------------------------------------------------
        // If the object contained a reference to the SessionContext (BeanO),
        // then the deserialized BeanO needs to be replaced with the BeanO that
        // is being used to activate the Stateful EJB.
        // -----------------------------------------------------------------------
        if (object instanceof StatefulBeanOReplacement) {
            return activatedBeanO;
        }

        // -----------------------------------------------------------------------
        // If the object contained a reference to PassivatorSerializable, then it
        // was written out as a PassivatorSerializableHandle. Convert it back.
        // Note: the PassivatorSerializableHandle might return itself if the
        // object really contains a reference to that object.                 
        // -----------------------------------------------------------------------
        if (object instanceof PassivatorSerializableHandle) {
            return ((PassivatorSerializableHandle) object).getSerializedObject();
        }

        return null;
    }
}
