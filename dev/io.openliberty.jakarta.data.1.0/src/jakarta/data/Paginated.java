/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a repository finder method to request pagination of results.
 * The method return type must be either:<p>
 *
 * <ul>
 * <li>{@link Page}, which represents a single page and provides the ability
 * to explicitly request subsequent pages.
 * <li>{@link java.util.Iterator Iterator}, which automatically advances
 * to subsequent pages as required by iteration.
 * </ul>
 *
 * For example,<p>
 *
 * <pre>
 * &#64;Paginated(50)
 * Iterator&#60;Item&#62; findByYearOrderById(int yearAdded);
 * </pre>
 *
 * Upon invocation of the method, the first page is read from the database.
 * Subsequent pages are read only as needed,
 * either as explicitly requested by {@link Page#next()}
 * or implicitly upon checking for a next item in the iterator
 * that reaches past the current page.<p>
 *
 * Expect the possibility of duplicates and missed entries across pages if
 * data is added, removed, or modified after the first page is read.
 * That possibility can be reduced if the ID attribute is always increasing (or
 * always decreasing) and the query is likewise ordered by the ID attribute alone.
 * TODO In this case, we could require that the Jakarta Data implementation append
 * an additional query condition upon the ID attribute to be greater than
 * (or less than if decreasing) the ID of the last element from the current page,
 * eliminating the possibility of duplicates. But is this worth it?
 * Lacking this, application that wants to avoid finding duplicates could do
 * something like the following if the primary key is always increasing:<p>
 *
 * <pre>
 * &#64;Paginated(200)
 * Page&#60;Item&#62; findByYearAndIdGreaterThanOrderById(int yearAdded, long idAfter);
 * </pre>
 *
 * <pre>
 * for (long idAfter = -1; idAfter < Long.MAX_VALUE; )
 *     Page&#60;Item&#62; page = items.findByYearAndIdGreaterThanOrderById(2020, idAfter);
 *     items = page.getContent(ArrayList::new);
 *     ...
 *     idAfter = items.size() == 200 ? items.get(items.size() - 1).id : Long.MAX_VALUE;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Paginated {
    /**
     * Maximum number of results per page.
     */
    int value() default 100;
}
