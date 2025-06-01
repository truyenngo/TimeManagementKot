package com.example.timemanagementkot.data.model

import com.google.firebase.auth.FirebaseUser

data class User(
    val userId: String = "",
    val email: String = "",
    val displayName: String? = null
) {
    constructor(firebaseUser: FirebaseUser) : this(
        userId = firebaseUser.uid,
        email = firebaseUser.email ?: "",
        displayName = firebaseUser.displayName
    )
}
