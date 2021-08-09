/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.model;

import java.util.List;

/**
 * Models a Java enum type.
 */
public class ModelEnumType extends ModelClassType {
    public static class Constant {
        public final String name;
        public final String xmiName;
        private boolean libertyNotInUse = false;

        public Constant(String name, String xmiName) {
            this.name = name;
            this.xmiName = xmiName;
        }

        public boolean isLibertyNotInUse() {
            return this.libertyNotInUse;
        }

        /**
         * @param tr
         */
        public void setLibertyNotInUse(boolean value) {
            this.libertyNotInUse = value;

        }
    }

    public final List<Constant> constants;

    public ModelEnumType(String className, List<Constant> constants) {
        super(className);
        this.constants = constants;
    }

    public boolean hasConstantName() {
        for (Constant constant : constants) {
            if (constant.name != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasXMIConstantName() {
        for (Constant constant : constants) {
            if (constant.xmiName != null) {
                return true;
            }
        }
        return false;
    }

    public String getParseXMIAttributeValueMethodName() {
        return "parseXMI" + className.substring(className.lastIndexOf('.') + 1) + "AttributeValue";
    }

    @Override
    public String getDefaultValue(String string) {
        if (string == null) {
            return null;
        }

        for (Constant constant : constants) {
            if (constant.name.equals(string)) {
                return getJavaImplTypeName() + '.' + string;
            }
        }

        throw new IllegalArgumentException("invalid default value \"" + string + "\" for type " + className);
    }
}
