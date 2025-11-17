/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.resolver.beans;

import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class OptionalValues {

    private Optional<PersonBean> optionalEmpty;
    private Optional<PersonBean> optionalBean;

    public OptionalValues() {
        optionalBean = Optional.of(new PersonBean());
        optionalEmpty = Optional.empty();
    }

    public Optional<PersonBean> getOptionalEmpty() {
        return optionalEmpty;
    }

    public Optional<PersonBean> getOptionalBean() {
        return optionalBean;
    }
}
