package org.example.gateway


const val USER_DETAILS_HEADER_KEY = "X-User-Details"
const val REQUEST_ID_HEADER = "J-Request-Id"
const val START_TIME_HEADER_KEY = "J-Start-Time"
const val CACHED_REQUEST_BODY_KEY = "cached-request-body"
const val CACHED_URL_KEY = "cached-request-url"


val IGNORED_HEADERS = setOf(USER_DETAILS_HEADER_KEY, REQUEST_ID_HEADER)