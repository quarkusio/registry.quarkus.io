package io.quarkus.registry.app.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jboss.logging.Logger;

/**
 * Generates a "best-effort" Semantic version based on a Maven version.
 */
public class Semver {

    private static final Logger log = Logger.getLogger(Semver.class);

    /**
     * Copied from https://semver.org/
     */
    private static final Predicate<String> SEMVER_PATTERN = Pattern
            .compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$").asPredicate();

    public static final String FINAL_QUALIFIER = "Final";

    private static final List<Pattern> QUALIFIERS = Arrays.asList(
            Pattern.compile("(Alpha)([0-9]+)"),
            Pattern.compile("(Beta)([0-9]+)"),
            Pattern.compile("(CR)([0-9]+)")
    );

    public static String toSemver(String version) {
        if (version == null || isSemver(version)) {
            return version;
        }
        StringBuilder semver = new StringBuilder();
        // 1.0.0.Final  -> 1.0.0
        // 1.0.0.Alpha1 -> 1.0.0-alpha-1
        // 1.0.0.Final-redhat-0001  -> 1.0.0+redhat-0001
        DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
        semver.append(artifactVersion.getMajorVersion())
                .append('.')
                .append(artifactVersion.getMinorVersion())
                .append('.')
                .append(artifactVersion.getIncrementalVersion());
        if (artifactVersion.getQualifier() != null) {
            String qualifier = artifactVersion.getQualifier();
            if (FINAL_QUALIFIER.equals(qualifier)) {
                int idx = version.lastIndexOf(qualifier);
                if (idx != version.length() - FINAL_QUALIFIER.length()) {
                    // There is something else after "Final"
                    qualifier = version.substring(idx + FINAL_QUALIFIER.length());
                } else {
                    qualifier = "";
                }
            } else {
                semver.append('-').append(normalizeQualifier(qualifier));
                int idx = version.lastIndexOf(qualifier);
                qualifier = version.substring(idx + qualifier.length());
            }
            if (!qualifier.isEmpty()) {
                semver.append(qualifier);
            }
        }
        String result = semver.toString();
        if (isSemver(result)) {
            return result;
        } else {
            if (log.isDebugEnabled()) {
                log.debugf("Could not convert {} to a semver format. Result was: {}", version, result);
            }
            return null;
        }
    }


    private static String normalizeQualifier(String qualifier) {
        // Alpha1 -> Alpha+1, Beta23 -> Beta+23, CR1 -> CR+1
        for (Pattern p : QUALIFIERS) {
            Matcher matcher = p.matcher(qualifier);
            if (matcher.find()) {
                return matcher.group(1) + "." + matcher.group(2);
            }
        }
        return qualifier;
    }

    public static boolean isSemver(String version) {
        return version != null && SEMVER_PATTERN.test(version);
    }
}
