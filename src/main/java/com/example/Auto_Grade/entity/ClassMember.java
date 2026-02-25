package com.example.Auto_Grade.entity;

import com.example.Auto_Grade.enums.MemberStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "class_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_class_account",
                        columnNames = {"class_id", "account_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @JsonIgnore
    private Class classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;
}