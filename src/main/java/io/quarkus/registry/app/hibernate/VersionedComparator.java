package io.quarkus.registry.app.hibernate;

import java.util.Comparator;

import io.quarkus.registry.app.model.Versioned;
import io.smallrye.common.version.VersionScheme;

public class VersionedComparator implements Comparator<Versioned> {

    private static final Comparator<String> COMPARATOR = VersionScheme.BASIC;

    @Override
    public int compare(Versioned o1, Versioned o2) {
        return -COMPARATOR.compare(o1.getVersion(), o2.getVersion());
    }
}
