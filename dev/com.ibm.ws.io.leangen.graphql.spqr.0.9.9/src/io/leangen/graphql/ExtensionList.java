package io.leangen.graphql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("WeakerAccess")
public final class ExtensionList<E> extends ArrayList<E> {

    ExtensionList(Collection<? extends E> c) {
        super(c);
    }

    public E getFirstOfType(Class<? extends E> extensionType) {
        return get(firstIndexOfTypeStrict(extensionType));
    }

    @SafeVarargs
    public final ExtensionList<E> append(E... extensions) {
        Collections.addAll(this, extensions);
        return this;
    }

    public ExtensionList<E> append(Collection<E> extensions) {
        super.addAll(extensions);
        return this;
    }

    @SafeVarargs
    public final ExtensionList<E> prepend(E... extensions) {
        return insert(0, extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insert(int index, E... extensions) {
        for (int i = 0; i < extensions.length; i++) {
            add(index + i, extensions[i]);
        }
        return this;
    }

    @SafeVarargs
    public final ExtensionList<E> insertAfter(Class<? extends E> extensionType, E... extensions) {
        return insert(firstIndexOfTypeStrict(extensionType) + 1, extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertBefore(Class<? extends E> extensionType, E... extensions) {
        return insert(firstIndexOfTypeStrict(extensionType), extensions);
    }

    @SafeVarargs
    public final ExtensionList<E> insertAfterOrAppend(Class<? extends E> extensionType, E... extensions) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType >= 0) {
            return insert(firstIndexOfType + 1, extensions);
        } else {
            return append(extensions);
        }
    }

    @SafeVarargs
    public final ExtensionList<E> insertBeforeOrPrepend(Class<? extends E> extensionType, E... extensions) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        return insert(firstIndexOfType >= 0 ? firstIndexOfType : 0, extensions);
    }

    public ExtensionList<E> drop(int index) {
        super.remove(index);
        return this;
    }

    public ExtensionList<E> drop(Class<? extends E> extensionType) {
        return drop(firstIndexOfTypeStrict(extensionType));
    }

    public ExtensionList<E> dropAll(Predicate<? super E> filter) {
        super.removeIf(filter);
        return this;
    }

    public ExtensionList<E> replace(int index, E replacement) {
        super.set(index, replacement);
        return this;
    }

    public ExtensionList<E> replace(Class<? extends E> extensionType, E replacement) {
        return replace(firstIndexOfTypeStrict(extensionType), replacement);
    }

    public ExtensionList<E> replaceOrAppend(Class<? extends E> extensionType, E replacement) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType >= 0) {
            return replace(firstIndexOfType, replacement);
        } else {
            return append(replacement);
        }
    }

    public ExtensionList<E> modify(Class<? extends E> extensionType, Consumer<E> modifier) {
        modifier.accept(get(firstIndexOfTypeStrict(extensionType)));
        return this;
    }

    private int firstIndexOfTypeStrict(Class<? extends E> extensionType) {
        int firstIndexOfType = firstIndexOfType(extensionType);
        if (firstIndexOfType < 0) {
            throw new ConfigurationException("Extension of type " + extensionType.getName() + " not found");
        }
        return firstIndexOfType;
    }

    private int firstIndexOfType(Class<? extends E> extensionType) {
        for (int i = 0; i < size(); i++) {
            if (extensionType.isInstance(get(i))) {
                return i;
            }
        }
        return -1;
    }
}
