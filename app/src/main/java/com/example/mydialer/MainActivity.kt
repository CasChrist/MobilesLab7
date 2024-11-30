package com.example.mydialer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

data class Contact(
    val name: String,
    val phone: String,
    val type: String
)

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactsList: List<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Timber
        Timber.plant(Timber.DebugTree())

        // Initialize RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(emptyList())
        recyclerView.adapter = contactAdapter

        // Load contacts from URL
        loadContacts("https://drive.google.com/uc?export=download&id=1-KO-9GA3NzSgIc1dkAsNm8Dqw0fuPxcR")

        // Setup search EditText
        val searchEditText = findViewById<android.widget.EditText>(R.id.et_search)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterContacts(searchText: String) {
        Timber.d("Filtering contacts for query: $searchText")

        val filteredContacts = if (searchText.isEmpty()) {
            contactsList
        } else {
            contactsList.filter { it.name.contains(searchText, ignoreCase = true) }
        }

        contactAdapter.updateContacts(filteredContacts)
    }

    private fun loadContacts(url: String) {
        Thread {
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        val contacts = parseContacts(json)
                        runOnUiThread {
                            contactsList = contacts
                            contactAdapter.updateContacts(contactsList)
                        }
                    }
                } else {
                    Timber.e("Failed to access file: ${response.message}")
                }
            } catch (e: IOException) {
                Timber.e(e, "Error loading contacts")
            }
        }.start()
    }

    private fun parseContacts(json: String): List<Contact> {
        val listType = object : TypeToken<List<Contact>>() {}.type
        return Gson().fromJson(json, listType)
    }
}
