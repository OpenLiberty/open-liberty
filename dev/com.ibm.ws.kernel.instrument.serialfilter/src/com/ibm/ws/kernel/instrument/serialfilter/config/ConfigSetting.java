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

import java.util.Properties;

interface ConfigSetting<I, O> extends ConfigurationSetting<SimpleConfig, I, O> {
    enum Resetter implements ConfigSetting<Void, Void> {
        RESET;
        public Class<Void> getInputType() {
            return Void.class;
        }
        public Class<Void> getOutputType() {
            return Void.class;
        }
        @Override
        public Void apply(SimpleConfig config, Void param) {
            config.reset();
            return null;
        }
    }

    enum PropertiesSetter implements ConfigSetting<Properties, Void> {
        LOAD;

        @Override
        public Class<Properties> getInputType() {
            return Properties.class;
        }

        @Override
        public Class<Void> getOutputType() {
            return Void.class;
        }

        @Override
        public Void apply(SimpleConfig target, Properties props) {
            target.load(props);
            return null;
        }
    }

    enum DefaultValidationModeGetter implements ConfigSetting<Void, ValidationMode> {
        GET_DEFAULT_VALIDATION_MODE;
        public Class<Void> getInputType() {return Void.class;}
        public Class<ValidationMode> getOutputType() {return ValidationMode.class;}
        public ValidationMode apply(SimpleConfig config, Void param) {return config.getDefaultMode();}
    }

    enum ValidationModeGetter implements ConfigSetting<String, ValidationMode> {
        GET_VALIDATION_MODE;
        public Class<String> getInputType() {return String.class;}
        public Class<ValidationMode> getOutputType() {return ValidationMode.class;}
        public ValidationMode apply(SimpleConfig config, String param) {return config.getValidationMode(param);}
    }
}
