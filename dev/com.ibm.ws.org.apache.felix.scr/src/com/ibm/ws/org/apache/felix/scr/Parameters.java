package com.ibm.ws.org.apache.felix.scr;

import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.ValueUtils;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.osgi.framework.ServiceReference;

public class Parameters {

    BindParameters bp;
    public Parameters (BindParameters bp) {
        this.bp = bp;
    }

    public Object[] getParameters(Class<?>... objTypes) {
        ComponentContextImpl context = bp.getComponentContext();
        RefPair refPair = bp.getRefPair();
        Object[] objects = new Object[objTypes.length];
        for (int i = 0; i < objTypes.length; ++i) {
            ValueType valType = ValueType.ref_serviceType;
            if (objTypes[i] == ServiceReference.class) {
                valType = ValueType.ref_serviceReference;
            }
            //ValueType valType = ValueUtils.getValueType(objTypes[i]);
            objects[i] = ValueUtils.getValue(null, valType, null, context, refPair);
        }
        return objects;
    }
    
}
