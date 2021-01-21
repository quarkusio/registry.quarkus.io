package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

@Entity
@Table(indexes = { @Index(columnList = "groupId,artifactId", unique = true) })
@NamedQuery(name = "Extension.findByGroupIdAndArtifactId", query = "select e from Extension e where e.groupId = ?1 and e.artifactId = ?2")
public class Extension extends BaseEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String groupId;
    public String artifactId;

    @Column(nullable = false)
    public String name;

    public String description;

    @ManyToOne
    public Category category;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @OneToMany(cascade = CascadeType.PERSIST)
    public List<ExtensionRelease> releases = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) o;
        return Objects.equals(groupId, extension.groupId) && Objects.equals(artifactId, extension.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    public static Uni<Extension> findByGroupIdAndArtifactId(String groupId, String artifactId) {
        return Extension.find("#Extension.findByGroupIdAndArtifactId", groupId, artifactId).firstResult();
    }

}
