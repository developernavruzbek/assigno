package org.example.auth.constants

import java.security.Principal

const val DOT = "."
const val HASH = "#"
const val JWT_USER_ID_KEY = "uid"
const val JWT_USERNAME_KEY = "sub"
const val JWT_ROLE_KEY = "rol"
const val JWT_ROLE_LEVEL_KEY = "roll"
const val JWT_CURRENT_ORG_ID_KEY = "coid"
const val ROLE_PREFIX = "ROLE_"
const val PERMISSION_GROUP_PREFIX = "PG_"
const val PERMISSION_PREFIX = "PERMISSION_"
const val DEV_ROLE = "DEV"
const val USER_DETAILS_HEADER_KEY = "X-User-Details"
val PRINCIPAL_KEY: String = Principal::class.java.name
const val USERNAME_KEY = "username"
const val PASSWORD_KEY = "password"
const val PARTNER_ROLE = "PARTNER"