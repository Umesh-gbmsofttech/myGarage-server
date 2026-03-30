package com.gbm.app.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mechanic_profiles", indexes = {
    @Index(name = "idx_mechanic_profiles_user_id", columnList = "user_id"),
    @Index(name = "idx_mechanic_profiles_profile_image_id", columnList = "profileImageId")
})
@Getter
@Setter
public class MechanicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String experience;

    private String speciality;

    private String city;

    private String certificate;

    @Column(nullable = false)
    private boolean available = true;

    private Double rating = 0.0;

    private Integer ratingCount = 0;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private boolean visible = true;

    private Long profileImageId;
    private Long certificationDocumentImageId;
    private Long shopActDocumentImageId;

    private String expertise;

    private String about;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MechanicRegistrationSource registrationSource = MechanicRegistrationSource.SELF;

    @ManyToOne
    @JoinColumn(name = "garage_owner_user_id")
    private User garageOwner;
}
