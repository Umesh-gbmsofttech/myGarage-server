package com.gbm.app.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicle_owner_profiles", indexes = {
    @Index(name = "idx_vehicle_owner_profiles_user_id", columnList = "user_id"),
    @Index(name = "idx_owner_profiles_avatar_image_id", columnList = "avatarImageId")
})
@Getter
@Setter
public class VehicleOwnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String city;

    private String addressLine;

    private Long avatarImageId;
}
