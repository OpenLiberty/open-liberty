/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

public class BookSubresourceImpl implements BookSubresource {

    private final Long id;

    public BookSubresourceImpl() {
        id = 123L;
    }

    public BookSubresourceImpl(Long id) {
        this.id = id;
    }

    @Override
    public Book getTheBook() throws BookNotFoundFault {

        if (id == 0) {
            return null;
        }

        Book b = new Book();
        b.setId(id);
        b.setName("CXF in Action");
        return b;
    }

    @Override
    public Book getTheBook2(String n1, String n2, String n3, String n33,
                            String n4, String n5, String n6)
                    throws BookNotFoundFault {

        Book b = new Book();
        b.setId(id);
        b.setName(n1 + n2 + n3 + n33 + n4 + n5 + n6);
        return b;
    }

    @Override
    public Book getTheBook3(String sid, List<String> nameParts) throws BookNotFoundFault {
        if (nameParts.size() != 2) {
            throw new RuntimeException("Wrong number of name parts");
        }

        Book b = new Book();

        b.setId(Long.valueOf(sid));
        b.setName(nameParts.get(0) + nameParts.get(1));
        return b;
    }

    @Override
    public Book getTheBook4(Book bookPath, Book bookQuery,
                            Book bookMatrix, Book formBook) throws BookNotFoundFault {
        if (bookPath == null || bookQuery == null
            || bookMatrix == null || formBook == null) {
            throw new RuntimeException();
        }
        long id1 = bookPath.getId();
        long id2 = bookQuery.getId();
        long id3 = bookMatrix.getId();
        long id4 = formBook.getId();
        if (id1 != 139L || id1 != id2 || id1 != id3 || id1 != id4 || id1 != id.longValue()) {
            throw new RuntimeException();
        }
        String name1 = bookPath.getName();
        String name2 = bookQuery.getName();
        String name3 = bookMatrix.getName();
        String name4 = formBook.getName();
        if (!"CXF Rocks".equals(name1) || !name1.equals(name2)
            || !name1.equals(name3) || !name1.equals(name4)) {
            throw new RuntimeException();
        }
        return bookPath;
    }

    @Override
    public Book getTheBookNoProduces() throws BookNotFoundFault {
        return getTheBook();
    }

    @Override
    public OrderBean addOrder(OrderBean order) {
        return order;
    }

    @Override
    public Book getTheBookWithContext(UriInfo ui) throws BookNotFoundFault {
        return getTheBook();
    }

    @Override
    public Book getTheBook5(String name, long bookid) throws BookNotFoundFault {
        return new Book(name, bookid);
    }

    @Override
    public BookBean getTheBookQueryBean(BookBean book) throws BookNotFoundFault {
        Map<Long, String> comments = book.getComments();
        String comment1 = comments.get(1L);
        String comment2 = comments.get(2L);
        if ("Good".equals(comment1) && "Good".equals(comment2)) {
            return book;
        } else {
            return null;
        }
    }

}
