/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.org.apache.faces40.fat.inputfile.beans;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

@Named
@RequestScoped
public class AcceptFilesBean {

    private Part singleSelection;
    private List<Part> multipleSelection;

    /**
     * @return the singleSelection
     */
    public void submitSingleSelection() {
        addAsMessage("singleSelection", singleSelection);
    }

    /**
     * @return the multipleSelection
     */
    public void submitMultipleSelection() {
        for (Part part : multipleSelection)
            addAsMessage("multipleSelection", part);
    }

    /**
     * @return the singleSelection
     */
    public Part getSingleSelection() {
        return singleSelection;
    }

    /**
     * @param singleSelection the singleSelection to set
     */
    public void setSingleSelection(Part singleSelection) {
        this.singleSelection = singleSelection;
    }

    /**
     * @return the multipleSelection
     */
    public List<Part> getMultipleSelection() {
        return multipleSelection;
    }

    /**
     * @param multipleSelection the multipleSelection to set
     */
    public void setMultipleSelection(List<Part> multipleSelection) {
        this.multipleSelection = multipleSelection;
    }

    private static void addAsMessage(String field, Part part) {
        String name = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        Optional<String> extension = Optional.of(name).filter(f -> f.contains(".")).map(f -> f.substring(name.lastIndexOf(".")));
        long size = part.getSize();

        FacesContext.getCurrentInstance().addMessage(null,
                                                     new FacesMessage("field: " + field + ", name: " + name + ", size: " + size + ", extension: " + extension.orElse("NONE")));
    }

}
