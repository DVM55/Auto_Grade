package com.example.Auto_Grade.entity;

import com.example.Auto_Grade.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "object_key", length = 255)
    private String object_key;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Builder.Default
    @Column(name = "locked", nullable = false)
    private boolean locked = false;
}
