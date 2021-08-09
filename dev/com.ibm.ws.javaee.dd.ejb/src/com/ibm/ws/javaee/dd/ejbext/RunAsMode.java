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
package com.ibm.ws.javaee.dd.ejbext;

import java.util.List;

import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;run-as-mode>.
 */
@LibertyNotInUse
public interface RunAsMode extends RunAsModeBase {

    /**
     * @return list of &lt;extended-method> objects, at least one must be provided.
     */
    @DDElement(name = "method", required = true)
    @DDXMIElement(name = "methodElements")
    List<ExtendedMethod> getMethods();

}
