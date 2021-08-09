/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

/**
 *
 */
public class ExternalTestServiceFilterAlwaysMatched implements ExternalTestServiceFilter {

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.utils.ExternalTestServiceFilter#isMatched(componenttest.topology.utils.ExternalTestService)
     */
    @Override
    public boolean isMatched(ExternalTestService ets) {
        return true;
    }

}
