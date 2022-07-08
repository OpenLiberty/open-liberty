/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.websphere.servlet60;

import java.util.HashMap;

import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.wsspi.http.HttpRequest;

/**
 * Since: Servlet 6.0      
 */
public interface IRequest60 extends IRequest40 {
    
    default void dummy60() {
    }

}
