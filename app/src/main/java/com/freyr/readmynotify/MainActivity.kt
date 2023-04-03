package com.freyr.readmynotify

import android.os.Bundle
import com.freyr.readmynotify.common.BaseActivity
import com.freyr.readmynotify.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        navView.setupWithNavController(navController)
    }
}