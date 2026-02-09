package org.example.auth.entities

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import org.example.auth.enums.Role
import org.example.auth.enums.UserStatus

//
/*
@Entity(name = "users")
class User(
    var fullName: String,
    @Column(nullable = false, unique = true)
    var phoneNumber: String,
    @Column(length = 32, unique = true)
    var username: String?,
    @Column(length = 255)
    var password: String,
    var role: Role,
    var currentOrgId: Long,
    @Enumerated(STRING)
    @Column(length = 32)
    var status: UserStatus = UserStatus.ACTIVE
) : BaseEntity()

 */