package com.ibm.ws.org.apache.felix.scr;

import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethods;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

import com.ibm.ws.org.apache.felix.scr.NoReflectionBindMethod.BindMethodType;

public class NoReflectionBindMethods implements ReferenceMethods {

    private final ReferenceMethod bind;
    private final ReferenceMethod updated;
    private final ReferenceMethod unbind;
    private final InitReferenceMethod init;

    NoReflectionBindMethods(StaticComponentManager componentManager, ReferenceMetadata m_dependencyMetadata) {
        bind = new NoReflectionBindMethod(componentManager, m_dependencyMetadata.getBind(), BindMethodType.BIND);
        updated = new NoReflectionBindMethod(componentManager, m_dependencyMetadata.getUpdated(), BindMethodType.UPDATED);
        unbind = new NoReflectionBindMethod(componentManager, m_dependencyMetadata.getUnbind(), BindMethodType.UNBIND);
        init = m_dependencyMetadata.getField() == null ? null : new NoReflectionBindMethod(componentManager, m_dependencyMetadata.getField(), BindMethodType.INIT);
    }
    
    @Override
    public ReferenceMethod getBind() {
        return bind;
    }

    @Override
    public InitReferenceMethod getInit() {
        return init;
    }

    @Override
    public ReferenceMethod getUnbind() {
        return unbind;
    }

    @Override
    public ReferenceMethod getUpdated() {
        return updated;
    }
}
