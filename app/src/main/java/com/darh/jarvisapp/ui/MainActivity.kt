package com.darh.jarvisapp.ui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.darh.jarvisapp.R
import com.darh.jarvisapp.ui.viewmodel.AssistantParams
import dagger.hilt.android.AndroidEntryPoint

const val CHAT_FRAGMENT_TAG = "chatFragmentTag"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, ChatFragment.newInstance(AssistantParams(0)), CHAT_FRAGMENT_TAG)
                .commit()
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (Intent.ACTION_SEARCH == intent?.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val chatFragment =
                supportFragmentManager.findFragmentByTag(CHAT_FRAGMENT_TAG) as? ChatFragment
            chatFragment?.updateInputBar(query)
        }
    }
}