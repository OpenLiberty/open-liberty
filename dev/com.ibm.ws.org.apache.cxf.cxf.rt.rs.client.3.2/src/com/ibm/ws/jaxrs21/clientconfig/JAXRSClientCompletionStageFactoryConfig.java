/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.clientconfig;

import com.ibm.ws.threading.CompletionStageFactory;

public class JAXRSClientCompletionStageFactoryConfig {

    private static volatile CompletionStageFactory completionStageFactory = null;

    protected void setCompletionStageFactory(CompletionStageFactory completionStageFactory) {        
        JAXRSClientCompletionStageFactoryConfig.completionStageFactory = completionStageFactory;
    }
    
    protected void unsetCompletionStageFactory(CompletionStageFactory completionStageFactory) {        
        JAXRSClientCompletionStageFactoryConfig.completionStageFactory = null;
    }

    public static CompletionStageFactory getCompletionStageFactory() {
        return completionStageFactory;
    }
}
