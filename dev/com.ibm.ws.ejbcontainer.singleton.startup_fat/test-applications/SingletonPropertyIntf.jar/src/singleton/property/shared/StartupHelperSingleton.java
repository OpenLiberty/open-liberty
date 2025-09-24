/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.shared;

public interface StartupHelperSingleton {
    void setPostConstructRun(boolean value);

    boolean isPostConstructRun();

    void setWarPostConstructRun(boolean value);

    boolean isWarPostConstructRun();

    void setListenerStarted(boolean value);

    boolean isListenerStarted();
}
