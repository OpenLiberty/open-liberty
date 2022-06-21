/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning.packages;

import java.util.Iterator;

/**
 *
 */
public interface SharedPackageInspector {

    public static interface PackageType {
        /** IBM API type="api" */
        boolean isUserDefinedApi();

        /** IBM API type="ibm-api" */
        boolean isIbmApi();

        /** IBM API type="internal" */
        boolean isInternalApi();

        /** IBM-API type="spec" or type="spec:osgi" */
        boolean isSpecApi();

        /** IBM-API type="spec" only */
        boolean isStrictSpecApi();

        /** IBM-API type="third-party" only */
        boolean isThirdPartyApi();

        /** IBM API type="stable" */        
        boolean isStableApi();              

        /** IBM-API type="spec:osgi" */
        public boolean isSpecOsgiApi();

    }

    Iterator<String> listApiPackages();

    PackageType getExportedPackageType(String packageName);
}
