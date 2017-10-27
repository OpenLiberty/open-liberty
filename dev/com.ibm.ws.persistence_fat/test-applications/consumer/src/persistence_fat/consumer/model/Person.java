/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package persistence_fat.consumer.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
// This gets overridden in-memory orm.xml
@Table(name = "PERSON_ORM")
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    long id;

    @Version
    int version;

    // These columns are overridden in static-orm.xml
    @Column(name = "FIRST_NAME")
    private String firstName;
    @Column(name = "LAST_NAME")
    private String lastName;

    private String data;

    public Person() {

    }

    public Person(long i, String f, String l) {
        id = i;
        firstName = f;
        lastName = l;
    }

    public long getId() {
        return id;
    }

    /**
     * @return the first
     */
    public String getFirst() {
        return firstName;
    }

    /**
     * @param first
     *            the first to set
     */
    public void setFirst(String first) {
        this.firstName = first;
    }

    /**
     * @return the last
     */
    public String getLast() {
        return lastName;
    }

    /**
     * @param last
     *            the last to set
     */
    public void setLast(String last) {
        this.lastName = last;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + version;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (id != other.id)
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (version != other.version)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + ", version=" + version + ", firstName=" + firstName + ", lastName=" + lastName
               + ", data=" + data + "]";
    }

}
