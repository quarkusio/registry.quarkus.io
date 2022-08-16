package io.quarkus.registry.app.util;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class Version {

    /**
     * Returns if the version is a valid match
     *
     * @param version
     * @return
     */
    public static void validateVersion(String version) {
        DefaultArtifactVersion dav = new DefaultArtifactVersion(version);
        if (dav.getMajorVersion() == 0) {
            throw new IllegalArgumentException("Invalid Version: " + version);
        }
    }

    /**
     * Order versions based on the qualifier. Pre-Final releases should always come last
     * 2.5.0.Final > 2.6.0.CR1
     * 2.6.0.SP1 > 2.5.0.Final
     */
    public static final Comparator<String> RELEASE_IMPORTANCE_COMPARATOR = ((o1, o2) -> {
        DefaultArtifactVersion v1 = new DefaultArtifactVersion(o1);
        DefaultArtifactVersion v2 = new DefaultArtifactVersion(o2);
        String o1Qualifier = v1.getQualifier();
        String o2Qualifier = v2.getQualifier();
        if (isQualifierPreFinal(o1Qualifier)) {
            if (!isQualifierPreFinal(o2Qualifier)) {
                return 1;
            }
        } else if (isQualifierPreFinal(o2Qualifier)) {
            if (!isQualifierPreFinal(o1Qualifier)) {
                return -1;
            }
        }
        return -v1.compareTo(v2);
    });

    public static boolean isPreFinal(String version) {
        DefaultArtifactVersion v = new DefaultArtifactVersion(version);
        return isQualifierPreFinal(v.getQualifier());
    }

    /**
     * @param versionQualifier the version to
     * @return true if the qualifier is before final (Alpha, Beta, CR or RC)
     */
    private static boolean isQualifierPreFinal(String versionQualifier) {
        if (versionQualifier == null) {
            return false;
        }
        String upperQualifier = versionQualifier.toUpperCase(Locale.ROOT);
        return upperQualifier.startsWith("CR") ||
                upperQualifier.startsWith("RC") ||
                upperQualifier.startsWith("ALPHA") ||
                upperQualifier.startsWith("BETA");
    }

    /**
     * Transforms the given version into a lexicographically sortable type
     * Eg. 1.2.3.Final -> 00001.00002.00003.Final
     *
     * @param version the version to be formatted
     * @return a version lexicographically sortable
     */
    public static String toSortable(String version) {
        DefaultArtifactVersion dav = new DefaultArtifactVersion(version);
        String qualifier = Objects.toString(dav.getQualifier(), "");
        // getQualifier does not work in some cases. Eg. 1.2.3.Final-redhat-00001
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
        return String.format("%05d.%05d.%05d.%s",
                dav.getMajorVersion(),
                dav.getMinorVersion(),
                dav.getIncrementalVersion(),
                qualifier);
    }
}
