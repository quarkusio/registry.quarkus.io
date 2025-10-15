package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
@Cacheable
public class Platform extends BaseEntity {

    private static final Pattern PLATFORM_KEY_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\-]+:quarkus-([a-zA-Z0-9\\-]+)-quarkus-platform-descriptor");

    @NaturalId
    @Column(nullable = false)
    public String platformKey;

    @Column
    public String name;

    @Column
    public String groupId;

    @Column
    public String artifactId;

    @Column
    public boolean isDefault;

    @Column
    @Enumerated(EnumType.STRING)
    public Type platformType = Type.C;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platform", orphanRemoval = true)
    @OrderBy("streamKeySortable DESC")
    public List<PlatformStream> streams = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Platform)) {
            return false;
        }
        Platform platform = (Platform) o;
        return Objects.equals(platformKey, platform.platformKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformKey);
    }

    public static Optional<Platform> findByKey(String platformKey) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Platform.class)
                .using("platformKey", platformKey)
                .loadOptional();
    }

    public static List<Platform> findMembersByPlatform(Platform platform) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.createQuery("FROM Platform p WHERE p.platformKey like :platformKey AND p.platformType = :type", Platform.class)
                .setParameter("platformKey", platform.platformKey +":%")
                .setParameter("type", Type.M)
                .getResultList();
    }


    /**
     * Returns a friendly platform name based on the platform key
     *
     * @param platformKey the platform key, in the format of groupId:artifactId
     * @return the platform name
     */
    public static String toPlatformName(String platformKey) {
        //io.quarkus.platform:quarkus-qpid-jms-bom-quarkus-platform-descriptor
        if (platformKey == null) {
            return null;
        }
        Matcher matcher = PLATFORM_KEY_PATTERN.matcher(platformKey);
        if (matcher.find()) {
            String content = Strings.CS.removeEnd(matcher.group(1).replace('-', ' '), " bom");
            return StringUtils.capitalize(content);
        }
        return platformKey;
    }

    /**
     * Is this a root or a member platform?
     */
    public enum Type {
        /**
         * Core member
         */
        C,
        /**
         * Member
         */
        M
    }
}
