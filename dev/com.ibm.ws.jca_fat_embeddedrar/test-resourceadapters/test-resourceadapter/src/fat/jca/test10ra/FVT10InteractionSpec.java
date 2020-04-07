/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import java.beans.PropertyChangeSupport;

import javax.resource.cci.InteractionSpec;

public class FVT10InteractionSpec implements InteractionSpec {

    private static final long serialVersionUID = 3452345326456L;

    public static final String RETURN_DATE = "RET_DATE";
    private String functionName;
    protected transient PropertyChangeSupport propertyChange;

    public FVT10InteractionSpec() {
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        String oldFunctionName = functionName;
        this.functionName = functionName;
    }
}
