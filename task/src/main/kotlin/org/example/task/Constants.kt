package org.example.task

import java.security.Principal
import kotlin.jvm.java

const val SYSTEM_NAME = "OPEN_BANKING"
const val BASE_API_VERSION = "/api/v1"
const val HUMO_CHECK_CARD_OWNERSHIP_RESULT = "Yes"
const val HUMO_CHECK_SMS_STATE = "on"
const val USER_ID_KEY = "uid"
const val JWT_CURRENT_ORG_ID_KEY = "coid"
const val USERNAME_KEY = "sub"
const val ROLE_KEY = "rol"
const val ROLE_LEVEL_KEY = "roll"
const val DEV_ROLE = "DEV"
const val USER_DETAILS_HEADER_KEY = "X-User-Details"
const val ACTIVE = "ACTIVE"
val PRINCIPAL_KEY: String = Principal::class.java.name