/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restProducer.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author anupag
 *
 */
public class DeleteAllRestResponse {

    @Schema(required = true, example = "Apps deleted")
    private String deleteResult = "";

    public String getDeleteResult() {
        return deleteResult;
    }

    public void setDeleteResult(String deleteResult) {
        this.deleteResult = deleteResult;
    }

    public DeleteAllRestResponse addDeleteResults(String deleteResult) {
        this.deleteResult += deleteResult;
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteAllRestResponse deleteAllRestResponse = (DeleteAllRestResponse) o;
        return Objects.equals(this.deleteResult, deleteAllRestResponse.deleteResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deleteResult);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DeleteAllRestResponse {\n");

        sb.append("    deleteResult: ").append(toIndentedString(deleteResult)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
