/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.jwt12.internal.tck;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                Mpjwt12TCKLauncher_aud_env.class,
                Mpjwt12TCKLauncher_aud_noenv.class,
                Mpjwt12TCKLauncher_noaud_env.class,
                Mpjwt12TCKLauncher_noaud_noenv.class,
                Mpjwt12TCKLauncher_aud_noenv2.class,
                DummyForQuarantine.class
})

public class FATSuite {
}
