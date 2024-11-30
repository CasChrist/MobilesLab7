package com.example.mydialer

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)

        val searchEditText = findViewById<EditText>(R.id.et_search)
        val recyclerView = findViewById<RecyclerView>(R.id.rView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter()
        recyclerView.adapter = contactAdapter

        loadContacts("https://drive.google.com/uc?export=download&id=1-KO-9GA3NzSgIc1dkAsNm8Dqw0fuPxcR")

        val savedFilter = sharedPreferences.getString("SEARCH_FILTER", "") ?: ""
        searchEditText.setText(savedFilter)

        searchEditText.addTextChangedListener { text ->
            val searchText = text.toString()
            Timber.d("Search text changed: $searchText")

            sharedPreferences.edit().putString("SEARCH_FILTER", searchText).apply()

            if (contactsList.isNotEmpty()) {
                val filteredContacts = contactsList.filter { contact ->
                    contact.name.contains(searchText, ignoreCase = true)
                }
                contactAdapter.submitList(filteredContacts)
            }
        }
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
                            Timber.d("Contacts loaded: ${contactsList.size}")
                            contactAdapter.submitList(contactsList)

                            // Применяем сохранённый фильтр после загрузки контактов
                            val savedFilter = sharedPreferences.getString("SEARCH_FILTER", "") ?: ""
                            if (savedFilter.isNotEmpty()) {
                                val filteredContacts = contactsList.filter { contact ->
                                    contact.name.contains(savedFilter, ignoreCase = true)
                                }
                                contactAdapter.submitList(filteredContacts)
                            }
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