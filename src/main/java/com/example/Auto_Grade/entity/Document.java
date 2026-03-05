package com.example.Auto_Grade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @JsonIgnore
    private Class classEntity;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;
}
