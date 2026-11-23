package io.quarkus.registry.app.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import io.smallrye.common.version.VersionScheme;

public class Version {

    /**
     * Returns if the version is a valid match
     *
     * @param version
     * @return
     */
    public static void validateVersion(String version) {
        VersionScheme.MAVEN.validate(version);
    }

    /**
     * Order versions based on the qualifier. Pre-Final releases should always come last and ordered naturally
     * 2.5.0.Final > 2.6.0.CR1
     * 2.6.0.SP1 > 2.5.0.Final
     */
    public static final Comparator<String> RELEASE_IMPORTANCE_COMPARATOR = ((left, right) -> {
        DefaultArtifactVersion leftVersion = new DefaultArtifactVersion(left);
        DefaultArtifactVersion rightVersion = new DefaultArtifactVersion(right);
        String leftQualifier = leftVersion.getQualifier();
        String rightQualifier = rightVersion.getQualifier();
        int result;
        if (isQualifierPreFinal(leftQualifier)) {
            if (!isQualifierPreFinal(rightQualifier)) {
                result = 1;
            } else {
                result = VersionScheme.MAVEN.compare(left, right);
            }
        } else if (isQualifierPreFinal(rightQualifier)) {
            if (!isQualifierPreFinal(leftQualifier)) {
                result = -1;
            } else {
                result = VersionScheme.MAVEN.compare(right, left);
            }
        } else {
            result = VersionScheme.MAVEN.compare(right, left);
        }
        return result;
    });

    public static boolean isPreFinal(String version) {
        DefaultArtifactVersion v = new DefaultArtifactVersion(version);
        return isQualifierPreFinal(v.getQualifier());
    }

    /**
     * @param versionQualifier the version to
     * @return true if the qualifier is before final (Alpha, Beta, CR or RC or SNAPSHOTS)
     */
    private static boolean isQualifierPreFinal(String versionQualifier) {
        if (versionQualifier == null) {
            return false;
        }
        String upperQualifier = versionQualifier.toUpperCase(Locale.ROOT);
        return upperQualifier.startsWith("CR") ||
                upperQualifier.startsWith("RC") ||
                upperQualifier.startsWith("ALPHA") ||
                upperQualifier.startsWith("BETA") ||
                upperQualifier.startsWith("SNAPSHOT");
    }

    /**
     * Transforms the given version into a lexicographically sortable type
     * Eg. 1.2.3.Final -> 00001.00002.00003.00000.Final
     *
     * @param version the version to be formatted
     * @return a version lexicographically sortable
     */
    public static String toSortable(String version) {
        List<String> formattedParts = new ArrayList<>();
        String qualifier = "";
        DecimalFormat df = new DecimalFormat("00000");
        for (String part : version.split("\\.")) {
            if (StringUtils.isNumeric(part)) {
                formattedParts.add(df.format(Integer.parseInt(part)));
            } else {
                qualifier = part;
            }
        }
        for (int i = formattedParts.size(); i < 4; i++) {
            formattedParts.add(df.format(0));
        }
        if (!version.endsWith(qualifier)) {
            int idx = version.indexOf(qualifier);
            qualifier = version.substring(idx);
        }
        if (qualifier.isEmpty()) {
            qualifier = "Final";
        }
        if (qualifier.startsWith("RC")) {
            qualifier = "CR" + qualifier.substring(2);
        }
        formattedParts.add(qualifier);
        return String.join(".", formattedParts);
    }

    /**
     * Returns the sortable form of the given version, raising a pre-final qualifier to {@code Final}, for use as an
     * inclusive upper bound when looking for releases built against that version.
     * <p>
     * {@link #toSortable(String)} synthesises the literal {@code Final} when a version carries no qualifier, and
     * {@code Final} sorts above every pre-final qualifier ({@code A < Alpha1 < Beta1 < CR1 < Final}). A plain
     * {@code <=} against the sortable form of a pre-final core therefore rejects every extension built against the
     * GA core of the same version: while the Quarkus 4.0 stream still resolved to {@code 4.0.0.CR1}, all the
     * extensions already built against {@code 4.0.0} were hidden from the non-platform catalog.
     * <p>
     * Raising the ceiling to {@code Final} keeps those extensions visible without widening the bound any further: a
     * newer micro/minor ({@code 4.0.1}, {@code 4.1.0}) or a post-final qualifier ({@code 4.0.0.SP1}) still sorts
     * above the ceiling and stays excluded. For a version that is already final this returns exactly
     * {@link #toSortable(String)}.
     *
     * @param version the version to be formatted
     * @return a lexicographically sortable version, with a pre-final qualifier raised to {@code Final}
     */
    public static String toSortableReleaseCeiling(String version) {
        String sortable = toSortable(version);
        if (!isPreFinal(version)) {
            return sortable;
        }
        StringBuilder numericParts = new StringBuilder();
        for (String part : sortable.split("\\.")) {
            if (!StringUtils.isNumeric(part)) {
                // the qualifier is the first non-numeric segment, and it is always the last one
                break;
            }
            if (!numericParts.isEmpty()) {
                numericParts.append('.');
            }
            numericParts.append(part);
        }
        return numericParts + ".Final";
    }
}
