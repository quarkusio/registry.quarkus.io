package io.quarkus.registry.app.maven;

import java.io.IOException;

/**
 * Rendered artifact content together with its checksums.
 * <p>
 * Hashing a 250 KB catalog costs about 0.45 ms (SHA-1 + MD5), so the checksums are computed once, when the content
 * itself is generated, and travel with it into the cache. Every response for that artifact and for its
 * {@code .sha1}/{@code .md5} siblings is then served without touching the body again.
 */
public record ArtifactContent(String content, String sha1, String md5) {

    public static ArtifactContent of(String content) throws IOException {
        return new ArtifactContent(content, HashUtil.sha1(content), HashUtil.md5(content));
    }
}
