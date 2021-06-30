package com.example.babastagram.navigation.menu

import java.sql.Timestamp

data class AlarmDTO(
    var destinationUid : String? = null,
    var userID : String? = null,
    var uid : String? = null,
    var kind : Int? = null,
    var message : String? = null,
    var timestamp: Long? = null
)