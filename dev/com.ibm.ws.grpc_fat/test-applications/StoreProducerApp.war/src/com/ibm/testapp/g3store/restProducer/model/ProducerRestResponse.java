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
public class ProducerRestResponse {

    @Schema(required = true, example = "1bxxfxxx-956e-4exx-8c7e-666fc6xxxaac")
    private String createResult = "";

    public String getCreateResult() {
        return createResult;
    }

    public void setCreateResult(String createResult) {
        this.createResult = createResult;
    }

    public ProducerRestResponse concatProducerResults(String createResult) {
        this.createResult += createResult;
        return this;
    }

    public ProducerRestResponse appID(String createResult) {
        this.createResult = createResult;
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
        ProducerRestResponse producerRestResponse = (ProducerRestResponse) o;
        return Objects.equals(this.createResult, producerRestResponse.createResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createResult);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProducerRestResponse {\n");

        sb.append("    appID: ").append(toIndentedString(createResult)).append("\n");
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
