/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config30.test;

import java.util.EnumSet;
import java.util.Set;

import com.ibm.ws.fat.util.tck.AbstractArquillianLoadableExtension;
import com.ibm.ws.fat.util.tck.TCKArchiveModifications;

public class ArquillianLoadableExtension extends AbstractArquillianLoadableExtension {
    @Override
    public Set<TCKArchiveModifications> getModifications() {
        return EnumSet.of(TCKArchiveModifications.TEST_LOGGER, TCKArchiveModifications.HAMCREST);
    }
}
