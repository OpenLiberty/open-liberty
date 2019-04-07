package org.aguibert.liberty;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Book {

    @Id
    public int id;

    @Basic
    public String author;

    @Basic
    public String title;

    @Basic
    public int pages;

}
