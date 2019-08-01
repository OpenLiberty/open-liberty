package com.ibm.ws.crypto.util;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.security.crypto.CustomPasswordEncryption;

public class CryptoComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        if (instance instanceof PasswordCipherUtil) {
            ((PasswordCipherUtil) instance).activate(componentContext);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        if (instance instanceof PasswordCipherUtil) {
            ((PasswordCipherUtil) instance).deactivate(componentContext);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setCustomPasswordEncryption".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((PasswordCipherUtil) instance).setCustomPasswordEncryption((ServiceReference<CustomPasswordEncryption>) params[0]);
        } else if ("setVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((VariableResolver) instance).setVariableRegistry((VariableRegistry) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetCustomPasswordEncryption".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((PasswordCipherUtil) instance).unsetCustomPasswordEncryption((ServiceReference<CustomPasswordEncryption>) params[0]);
        } else if ("unsetVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((VariableResolver) instance).unsetVariableRegistry((VariableRegistry) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (PasswordCipherUtil.class.getName().equals(componentName) || VariableResolver.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
