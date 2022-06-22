/**
 *
 */
package test.jakarta.data.web;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used to test if a user-defined collection class can be used
 * for a repository method return type.
 */
public class UserDefinedCollection<T> extends AbstractCollection<T> {
    private final List<T> list = new ArrayList<T>();

    @Override
    public boolean add(T element) {
        return list.add(element);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public int size() {
        return list.size();
    }
}
