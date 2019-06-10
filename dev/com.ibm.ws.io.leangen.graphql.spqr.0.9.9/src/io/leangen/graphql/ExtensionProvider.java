package io.leangen.graphql;

import java.util.List;

@FunctionalInterface
public interface ExtensionProvider<C, D> {
    List<D> getExtensions(C config, ExtensionList<D> defaults);
}
