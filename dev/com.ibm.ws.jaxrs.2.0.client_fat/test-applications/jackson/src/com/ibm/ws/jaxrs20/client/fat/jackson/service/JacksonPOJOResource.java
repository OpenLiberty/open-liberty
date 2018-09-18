/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.jackson.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * <code>JacksonPOJOResource</code> is a simple POJO which is annotated with
 * JAX-RS annotations to turn it into a JAX-RS resource.
 * <p/>
 * This class has a {@link Path} annotation with the value "/person" which
 * means the resource will be available at:
 * <code>http://&lt;hostname&gt;:&lt;port&gt/&lt;context root&gt;/&lt;servlet path&gt;/person</code>
 * <p/>
 * Remember to add this resource class to the {@link JacksonPOJOApplication#getClasses()} method.
 */
@Path("/person")
public class JacksonPOJOResource {

    @GET
    @Produces("application/json")
    @Path("person")
    public Person getPerson() {
        Person p = new Person();
        p.setFirst("first");
        p.setLast("last");
        return p;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("person")
    public Person postPerson(Person p) {
        return p;
    }

    @GET
    @Produces("application/json")
    @Path("string")
    public List<String> getCollection() {
        List<String> list = new ArrayList<String>();
        list.add("string1");
        list.add("");
        list.add("string3");
        return list;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("string")
    public List<String> postCollection(List<String> list) {
        return list;
    }

    @GET
    @Produces("application/json")
    @Path("personcollect")
    public List<Person> getPersonCollection() {
        List<Person> people = new ArrayList<Person>();
        Person p = new Person();
        p.setFirst("first1");
        p.setLast("last1");
        people.add(p);
        p = new Person();
        p.setFirst("first2");
        p.setLast("last2");
        people.add(p);
        p = new Person();
        p.setFirst("first3");
        p.setLast("last3");
        people.add(p);
        return people;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("personcollect")
    public List<Person> postPeopleCollection(List<Person> people) {
        return people;
    }

    @GET
    @Produces("application/json")
    @Path("stringarray")
    public String[] getArray() {
        String[] list = new String[4];
        list[0] = "string1";
        list[1] = "";
        list[2] = null;
        list[3] = "string4";
        return list;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("stringarray")
    public String[] postArray(String[] list) {
        return list;
    }

    @GET
    @Produces("application/json")
    @Path("personarray")
    public Person[] getPeopleArray() {
        Person[] people = new Person[3];
        Person p = new Person();
        p.setFirst("first1");
        p.setLast("last1");
        people[0] = p;
        p = new Person();
        p.setFirst("first2");
        p.setLast("last2");
        people[1] = p;
        p = new Person();
        p.setFirst("first3");
        p.setLast("last3");
        people[2] = p;
        return people;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("personarray")
    public Person[] postPeopleArray(Person[] people) {
        return people;
    }

    @GET
    @Produces("application/json")
    @Path("collectionofcollection")
    public List<List<Person>> getCollectionofCollection() {
        List<List<Person>> peopleCollection = new ArrayList<List<Person>>();

        List<Person> people = new ArrayList<Person>();
        Person p = new Person();
        p.setFirst("first1");
        p.setLast("last1");
        people.add(p);
        p = new Person();
        p.setFirst("first2");
        p.setLast("last2");
        people.add(p);
        p = new Person();
        p.setFirst("first3");
        p.setLast("last3");
        people.add(p);
        peopleCollection.add(people);

        people = new ArrayList<Person>();
        p = new Person();
        p.setFirst("first4");
        p.setLast("last4");
        people.add(p);
        people.add(null);
        p = new Person();
        p.setFirst("first6");
        p.setLast("last6");
        people.add(p);
        peopleCollection.add(people);

        return peopleCollection;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("collectionofcollection")
    public List<List<Person>> postCollectionofCollection(List<List<Person>> peopleCollection) {
        return peopleCollection;
    }

    @GET
    @Produces("application/json")
    @Path("collectionofarray")
    public List<Person[]> getCollectionofArray() {
        List<Person[]> peopleCollection = new ArrayList<Person[]>();

        List<Person> people = new ArrayList<Person>();
        Person p = new Person();
        p.setFirst("first1");
        p.setLast("last1");
        people.add(p);
        p = new Person();
        p.setFirst("first2");
        p.setLast("last2");
        people.add(p);
        p = new Person();
        p.setFirst("first3");
        p.setLast("last3");
        people.add(p);
        peopleCollection.add(people.toArray(new Person[] {}));

        people = new ArrayList<Person>();
        p = new Person();
        p.setFirst("first4");
        p.setLast("last4");
        people.add(p);
        people.add(null);
        p = new Person();
        p.setFirst("first6");
        p.setLast("last6");
        people.add(p);
        peopleCollection.add(people.toArray(new Person[] {}));

        return peopleCollection;
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("collectionofarray")
    public List<Person[]> postCollectionofArray(List<Person[]> peopleCollection) {
        return peopleCollection;
    }

    public static class Person {
        String first;
        String last;

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person))
                return false;
            Person other = (Person) o;
            return this.first.equals(other.first) && this.last.equals(other.last);
        }
    }
}
