/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.ejbcontainer.util.EJBSerializer;
import com.ibm.ws.ejbcontainer.util.EJBSerializer.ObjectType;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationObjectReplacer;

/**
 * Provides a global implementation of {@link SerializationObjectReplacer} and {@link DeserializationObjectResolver} so
 * that references to EJB's can be serialized/deserialized.
 */
// Note we override the trace options so that the right message bundle for CNTR0005W is used
@Component(service = { SerializationObjectReplacer.class, DeserializationObjectResolver.class })
@TraceOptions(traceGroup = "EJBContainer", messageBundle = "com.ibm.ejs.container.container")
public class SerializedEJBRefHandler implements SerializationObjectReplacer, DeserializationObjectResolver {

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    private static final TraceComponent tc = Tr.register(SerializedEJBRefHandler.class);

    private static final EJBSerializer serializer = EJBSerializer.instance();

    @Override
    public Object resolveObject(@Sensitive Object object) {

        if (object instanceof SerializedEJBRef) {
            try {
                SerializedEJBRef ejbRef = (SerializedEJBRef) object;

                return serializer.deserialize(ejbRef.getSerializedEJB());
            } catch (Exception e) {
                FFDCFilter.processException(e, SerializedEJBRefHandler.class.getName() + ".resolveObject",
                                            "191", this);
                Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W", object, this, e);
            }
        }

        return null;
    }

    @Override
    public Object replaceObject(@Sensitive Object object) {

        ObjectType objectType = serializer.getObjectType(object);
        if (objectType != ObjectType.NOT_AN_EJB &&
            objectType != ObjectType.CORBA_STUB &&
            objectType != ObjectType.EJB_HOME &&
            objectType != ObjectType.EJB_OBJECT) {
            return new SerializedEJBRef(serializer.serialize(object));
        }

        return null;
    }

}
