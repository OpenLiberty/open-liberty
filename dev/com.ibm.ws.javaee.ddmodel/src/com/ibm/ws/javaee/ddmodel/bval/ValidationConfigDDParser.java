/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.bval;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class ValidationConfigDDParser extends DDParser {

    private static final TraceComponent tc = Tr.register(ValidationConfigDDParser.class);

    public ValidationConfigDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
        super(ddRootContainer, ddEntry);
    }

    @Override
    protected void failInvalidRootElement() throws ParseException {
        // provide warning message, but do not fail if the wrong root element is found
        if (rootParsable == null) {
            String moduleName = rootContainer.getName();
            NonPersistentCache cache = null;
            try {
                cache = rootContainer.adapt(NonPersistentCache.class);
            } catch (UnableToAdaptException e) {
                // EMPTY
            }

            if (cache != null) {
                ModuleInfo moduleInfo = (ModuleInfo) cache.getFromCache(ModuleInfo.class);
                if (moduleInfo != null) {
                    moduleName = moduleInfo.getName();
                }
            }

            Tr.warning(tc, "BVKEY_NOT_A_BEAN_VALIDATION_XML", moduleName);
        }

    }

    ValidationConfig parse() throws ParseException {
        super.parseRootElement();
        if (rootParsable == null) {
            return null;
        }
        return (ValidationConfig) rootParsable;
    }

    @Override
    protected ParsableElement createRootParsable() throws ParseException {
        if (!"validation-config".equals(rootElementLocalName)) {
            return null;
        }

        String vers = getAttributeValue("", "version");
        if (vers == null) {
            if ("http://jboss.org/xml/ns/javax/validation/configuration".equals(namespace)) {
                // javaee 6
                // a null version indicates that no version is specified per the 1.0 xsd
                version = ValidationConfig.VERSION_1_0;
                return new ValidationConfigType(getDeploymentDescriptorPath());
            }
        } else if ("1.0".equals(vers)) {
            // 1.0 is not allowed in either the 1.0 or 1.1 xsd's, so fail here
            throw new ParseException(invalidDeploymentDescriptorVersion("1.0"));
        } else if ("1.1".equals(vers)) {
            if ("http://jboss.org/xml/ns/javax/validation/configuration".equals(namespace)) {
                // javaee 7
                version = ValidationConfig.VERSION_1_1;
                return new ValidationConfigType(getDeploymentDescriptorPath());
            }
        }
        throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
    }

}
