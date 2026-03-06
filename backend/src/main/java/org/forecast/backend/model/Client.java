package org.forecast.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
public class Client {
    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 255, message = "Client name must be between 2 and 255 characters")
    @Column(nullable = false)
    private String name;

    @Email(message = "Email should be valid")
    private String email;

    private String address;
}
