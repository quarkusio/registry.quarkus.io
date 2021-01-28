package io.quarkus.registry.app.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;

@Entity
@NamedQueries({
        @NamedQuery(name = "Extension.findByGA",
                query = "select e from Extension e where e.groupId = ?1 and e.artifactId = ?2")
})
public class Extension extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String groupId;

    @NaturalId
    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String name;

    @Column(length = 4096)
    public String description;

    @OneToMany(mappedBy = "extension")
    public List<ExtensionRelease> releases;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) o;
        return Objects.equals(groupId, extension.groupId) &&
                Objects.equals(artifactId, extension.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    public static Optional<Extension> findByGA(String groupId, String artifactId) {
        return Extension.find("#Extension.findByGA", groupId, artifactId).firstResultOptional();
    }

}
