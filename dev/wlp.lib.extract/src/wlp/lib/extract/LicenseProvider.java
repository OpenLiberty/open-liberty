/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.InputStream;

/**
 * Implementations of this interface provide information and text for the license of a particular product.
 */
public interface LicenseProvider {

    /**
     * The name of the program this license is for.
     *
     * @return
     */
    public String getProgramName();

    /**
     * The name of the license for this program.
     *
     * @return
     */
    public String getLicenseName();

    /**
     * Returns an input stream to the license agreement.
     *
     * @return
     */
    public InputStream getLicenseAgreement();

    /**
     * Returns an input stream to the license information.
     *
     * @return
     */
    public InputStream getLicenseInformation();
}
