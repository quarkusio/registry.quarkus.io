package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

@Entity
@Cacheable
public class Platform extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String platformKey;

    @Column
    public String name;

    @Column
    public boolean isDefault;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
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
}
