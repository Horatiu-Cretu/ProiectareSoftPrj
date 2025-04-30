package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "hashtags")
public class Hashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100) // Add length limit
    private String name;

    @ManyToMany(mappedBy = "hashtags", fetch = FetchType.LAZY) // Fetch lazily
    @ToString.Exclude // Avoid recursion in toString
    @EqualsAndHashCode.Exclude // Avoid recursion in equals/hashCode
    private Set<Post> posts = new HashSet<>();

    // Constructor for convenience
    public Hashtag(String name) {
        this.name = name;
    }

    // Override equals and hashCode based on name for proper Set behavior
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hashtag hashtag = (Hashtag) o;
        return name != null ? name.equalsIgnoreCase(hashtag.name) : hashtag.name == null; // Case-insensitive comparison
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0; // Case-insensitive hashcode
    }
}