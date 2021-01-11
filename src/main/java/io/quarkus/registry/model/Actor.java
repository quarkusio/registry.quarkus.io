package io.quarkus.registry.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Actor {

    @Id @GeneratedValue
    private Long id;
    private String name;

    @Relationship(type = "ACTS_IN", direction = Relationship.OUTGOING)
    private Set<Movie> movies = new HashSet<>();

    public Actor() {
    }

    public Actor(String name) {
        this.name = name;
    }

    public void actsIn(Movie movie) {
        movies.add(movie);
        movie.getActors().add(this);
    }
}
