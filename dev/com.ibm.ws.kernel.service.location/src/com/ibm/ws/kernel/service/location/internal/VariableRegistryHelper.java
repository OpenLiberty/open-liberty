/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import com.ibm.ws.kernal.service.location.SymbolResolver;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 *
 */
public class VariableRegistryHelper implements VariableRegistry {

    private final SymbolRegistry registry;

    public VariableRegistryHelper() {
        this(SymbolRegistry.getRegistry());
    }

    public VariableRegistryHelper(SymbolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean addVariable(String variable, String value) {
        return registry.addStringSymbol(variable, value);
    }

    @Override
    public void replaceVariable(String variable, String value) {
        registry.replaceStringSymbol(variable, value);
    }

    /** {@inheritDoc} */
    @Override
    public String resolveString(String string) {
        return registry.resolveSymbolicString(string);
    }

    /** {@inheritDoc} */
    @Override
    public String resolveRawString(String string) {
        return registry.resolveRawSymbolicString(string);
    }

    /** {@inheritDoc} */
    @Override
    public void removeVariable(String symbol) {
        registry.removeSymbol(symbol);
    }
    
    /** 
     * Helper method to add SymbolResolver to SymbolRegistry from Activator 
     * @param resolver
     */
    protected void addSymbolResolver(SymbolResolver resolver) {
    	if (resolver != null) {
    		registry.addSymbolResolver(resolver);
    	}
    }
    
    /** 
     * Helper method to remover specific SymbolResolver from SymbolRegistry.
     * Used in Activator class. 
     * @param resolver
     */
    protected void removeSymbolResolver(SymbolResolver resolver) {
    	if (resolver != null) {
    		registry.removeSymbolResolver(resolver);
    	}
    }
    
    /**
     * Method to remove all SymbolResolver objects from SymbolRegistry. Called from 
     * Activator class when SymbolResolverServiceTracker is closed.
     */
    protected void removeAllSymbolResolvers() {
    	registry.clearAllResolverList();
    }
    	

}