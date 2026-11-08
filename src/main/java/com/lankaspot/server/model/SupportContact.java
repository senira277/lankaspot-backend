package com.lankaspot.server.model;

import com.lankaspot.server.enums.SupportContactStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_contacts")
@Data
@NoArgsConstructor
public class SupportContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number",  length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SupportContactStatus supportContactStatus;
}
