package com.freyr.readmynotify.common

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {
    private var companionDialog: AlertDialog? = null
    val TAG = this.javaClass.simpleName

    var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setMyFirebase()
    }

    private fun setMyFirebase(){
        db = FirebaseFirestore.getInstance()
    }
}