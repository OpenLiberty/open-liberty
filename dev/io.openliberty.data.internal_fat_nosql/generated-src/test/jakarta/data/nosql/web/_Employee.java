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
package test.jakarta.data.nosql.web;

import jakarta.data.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.NoSQLAttribute;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.StringAttribute;
import org.eclipse.jnosql.mapping.semistructured.metamodel.attributes.CriteriaAttribute;
//CHECKSTYLE:OFF
@StaticMetamodel(Employee.class)
@Generated(value = "The StaticMetamodel of the class Employee provider by Eclipse JNoSQL", date = "2025-05-30T11:32:09.226355")
public interface _Employee {

        String EMPNUM = "_id";
        String FIRSTNAME = "firstName";
        String LASTNAME = "lastName";
        String LOCATION = "location";
        String POSITION = "position";
        String WAGE = "wage";
        String YEARHIRED = "yearHired";
   
        CriteriaAttribute<Employee> empNum = new NoSQLAttribute<>(EMPNUM);
        StringAttribute<Employee> firstName = new NoSQLAttribute<>(FIRSTNAME);
        StringAttribute<Employee> lastName = new NoSQLAttribute<>(LASTNAME);
        StringAttribute<Employee> location = new NoSQLAttribute<>(LOCATION);
        StringAttribute<Employee> position = new NoSQLAttribute<>(POSITION);
        CriteriaAttribute<Employee> wage = new NoSQLAttribute<>(WAGE);
        CriteriaAttribute<Employee> yearHired = new NoSQLAttribute<>(YEARHIRED);

}
//CHECKSTYLE:ON