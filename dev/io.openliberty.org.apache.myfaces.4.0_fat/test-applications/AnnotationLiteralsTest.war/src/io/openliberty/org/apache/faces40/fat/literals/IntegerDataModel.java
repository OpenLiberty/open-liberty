/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import jakarta.faces.model.DataModel;
import jakarta.faces.model.FacesDataModel;

/**
 * This class forces CDI to create a @FacesDataModel bean for testing
 */
@FacesDataModel(forClass = Integer.class)
public class IntegerDataModel extends DataModel<Integer> {

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public Integer getRowData() {
        return Integer.valueOf(32);
    }

    @Override
    public int getRowIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getWrappedData() {
        return null;
    }

    @Override
    public boolean isRowAvailable() {
        return true;
    }

    @Override
    public void setRowIndex(int arg0) {
        //DO NOTHING
    }

    @Override
    public void setWrappedData(Object arg0) {
        //DO NOTHING
    }
}
