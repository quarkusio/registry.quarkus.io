package io.quarkus.registry.app.maven;

import java.io.IOException;

import io.quarkus.maven.dependency.ArtifactCoords;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface ArtifactContentProvider {

    /**
     * Header carrying the SHA-1 of the response body, as used by Maven Central.
     */
    String HEADER_CHECKSUM_SHA1 = "x-checksum-sha1";

    /**
     * Header carrying the MD5 of the response body, as used by Maven Central.
     */
    String HEADER_CHECKSUM_MD5 = "x-checksum-md5";

    boolean supports(ArtifactCoords artifact, UriInfo uriInfo);

    Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception;

    /**
     * Builds the response for an artifact request, serving either the content itself or one of its checksums,
     * depending on the suffix the client asked for.
     * <p>
     * The content of this registry is generated on the fly from the current database state, so the body and its
     * {@code .sha1}/{@code .md5} siblings are two independent requests over two potentially different states. A
     * client that downloads the body and then asks for the checksum can therefore receive a checksum that describes
     * different bytes, and reports it as {@code Checksum validation failed ... (REMOTE_EXTERNAL)}.
     * <p>
     * To avoid that, the checksums of the body travel <em>with</em> the body in the {@code x-checksum-sha1} and
     * {@code x-checksum-md5} headers. Maven Resolver evaluates such "Remote Included" checksums before falling back
     * to a separate {@code .sha1} request, so the checksum can never describe anything other than the bytes the
     * client received. It also saves one HTTP round trip per artifact.
     *
     * @see <a href="https://maven.apache.org/resolver/expected-checksums.html">Maven Resolver: Expected Checksums</a>
     */
    default Response toResponse(ArtifactCoords artifact, UriInfo uriInfo, String content, String mediaType)
            throws IOException {
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        if (ArtifactParser.SUFFIX_SHA1.equals(checksumSuffix)) {
            return toChecksumResponse(HashUtil.sha1(content));
        }
        if (ArtifactParser.SUFFIX_MD5.equals(checksumSuffix)) {
            return toChecksumResponse(HashUtil.md5(content));
        }
        return Response.ok(content)
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .header(HEADER_CHECKSUM_SHA1, HashUtil.sha1(content))
                .header(HEADER_CHECKSUM_MD5, HashUtil.md5(content))
                .build();
    }

    private static Response toChecksumResponse(String checksum) {
        return Response.ok(checksum)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .build();
    }
}
