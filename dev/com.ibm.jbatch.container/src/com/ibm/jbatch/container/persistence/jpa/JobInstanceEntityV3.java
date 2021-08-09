/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
@Entity
public class JobInstanceEntityV3 extends JobInstanceEntityV2 {

    @ElementCollection
    @CollectionTable(name = "GROUPASSOCIATION", joinColumns = @JoinColumn(name = "FK_JOBINSTANCEID"))
    @Column(name = "GROUPNAMES")
    private Set<String> groupNames;

    // For JPA
    @Trivial
    public JobInstanceEntityV3() {
        super();
    }

    // For in-memory persistence
    public JobInstanceEntityV3(long jobInstanceId) {
        super(jobInstanceId);
    }

    public void setGroupNames(Set<String> opGroupNames) {

        groupNames = opGroupNames;
    }

    @Override
    public Set<String> getGroupNames() {
        return groupNames;
    }

}
