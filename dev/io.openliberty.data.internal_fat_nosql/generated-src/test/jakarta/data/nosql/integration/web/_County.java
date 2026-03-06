/*
*  Copyright (c) 2023 Otávio Santana and others
*   All rights reserved. This program and the accompanying materials
*   are made available under the terms of the Eclipse Public License v1.0
*   and Apache License v2.0 which accompanies this distribution.
*   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
*   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
*
*   You may elect to redistribute this code under either of these licenses.
*
*   Contributors:
*
*   Otavio Santana
*/
package test.jakarta.data.nosql.integration.web;

import jakarta.data.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.NoSQLAttribute;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.StringAttribute;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.CriteriaAttribute;
//CHECKSTYLE:OFF
@StaticMetamodel(County.class)
@Generated(value = "The StaticMetamodel of the class County provider by Eclipse JNoSQL", date = "2026-01-05T12:00:00.000000")
public interface _County {

        String NAME = "_id";
        String POPULATION = "population";
        String ZIPCODES = "zipcodes";
        String COUNTYSEAT = "countySeat";
   
        StringAttribute<County> name = new NoSQLAttribute<>(NAME);
        CriteriaAttribute<County> population = new NoSQLAttribute<>(POPULATION);
        CriteriaAttribute<County> zipcodes = new NoSQLAttribute<>(ZIPCODES);
        StringAttribute<County> countySeat = new NoSQLAttribute<>(COUNTYSEAT);

}
//CHECKSTYLE:ON