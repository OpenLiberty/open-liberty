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
package com.ibm.ws.kernel.instrument.serialfilter.config;

public enum ValidationMode implements ConfigSetting<String, Boolean>{
    INACTIVE, DISCOVER, ENFORCE, REJECT;
    public Class<String> getInputType() {return String.class;}
    public Class<Boolean> getOutputType() {return Boolean.class;}
    public Boolean apply(SimpleConfig config, String param) {return config.setValidationMode(this, param);}
}