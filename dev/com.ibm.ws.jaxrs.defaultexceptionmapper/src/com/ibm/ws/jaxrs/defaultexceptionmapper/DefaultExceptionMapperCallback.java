/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.defaultexceptionmapper;

import java.util.Map;
import javax.ws.rs.container.ResourceInfo;

public interface DefaultExceptionMapperCallback {
    /**
     * A callback method fired when the default exception mapper is invoked,
     * indicating that a request resulted in an unmapped exception. The JAX-RS
     * runtime will invoke this method for all registered listeners, passing in the
     * unmapped exception and the mapped status code. The returned map is
     * a map of headers to be added to the resulting Response. Null or an empty
     * map indicates no new headers should be added. **/
   
   Map<String, Object> onDefaultMappedException(Throwable paramThrowable, int paramInt, ResourceInfo paramResourceInfo);
}
