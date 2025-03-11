package com.example.speechtocall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import android.provider.ContactsContract
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var micBtn: ImageView
    private lateinit var contactsTextView: TextView
    private lateinit var wantedNameView: TextView

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val CALL_PERMISSION_CODE = 3

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.texts)
        micBtn = findViewById(R.id.buttons)
        contactsTextView = findViewById(R.id.contacts)
        wantedNameView = findViewById(R.id.wantedNameView)

        checkAudioPermission()
        checkContactsPermission()
        checkCallPhonePermission()

        micBtn.setOnClickListener {
            startSpeechToText()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RecordAudioRequestCode
            )
        }
    }

    private fun checkCallPhonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_CODE
            )
        }
    }

    private fun findClosestName(sentence: String, contactData: List<List<String>>): MutableList<Triple<String, Double, String>> {
        val word = sentence.lowercase()
        var bestMatchIndex = -1
        var highestScore = 0.0

        val bestMatches = mutableListOf<Triple<String, Double, String>>()
        val similarity = JaroWinklerSimilarity()

        for ((name, phoneNumber) in contactData) {
            val score = similarity.apply(word, name.lowercase())
            if(score > 0.8){
                bestMatches.add(Triple(name, score, phoneNumber))
            }
        }

        return bestMatches
    }

    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PERMISSION_CODE)
            // Show a message to the user
            Toast.makeText(this, "Phone call permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                ContactsRequestCode
            )
        }
    }

    private fun startSpeechToText() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)

                micBtn.setImageResource(R.drawable.ic_mic_on)
                editText.hint = "Listening..."

            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Permission required for speech recognition", Toast.LENGTH_SHORT).show()
            checkAudioPermission()
        }
    }

    private fun getContactsData(): MutableList<List<String>> {
        val contactsList = mutableListOf<List<String>>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val resolver: ContentResolver = contentResolver
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (nameIndex != -1 && phoneIndex != -1) {
                        val name = it.getString(nameIndex)
                        val phoneNumber = it.getString(phoneIndex)
                        val contact = listOf(name, phoneNumber)
                        contactsList.add(contact)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Contacts permission required", Toast.LENGTH_SHORT).show()
            checkContactsPermission()
        }

        return contactsList
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        micBtn.setImageResource(R.drawable.ic_mic_off)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (result != null && result.isNotEmpty()) {
                    val text = result[0]

                    var searchedName = text.split(" ", limit = 2)
                    editText.setText(searchedName[1])
                    val contactsData = getContactsData()
                    val matchedContact = findClosestName(searchedName[1], contactsData)
                    val highestScoreMatch = matchedContact.maxByOrNull { it.second }

                    if (matchedContact.isNotEmpty()) {
                        wantedNameView.text = matchedContact.toString()
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            if (highestScoreMatch != null) {
                                makePhoneCall(highestScoreMatch.third)
                            }
                        } else {
                            Toast.makeText(this, "Call permission required to dial number", Toast.LENGTH_SHORT).show()
                            checkCallPhonePermission()
                        }

                        val allContactsText = StringBuilder()
                        for (contact in contactsData) {
                            allContactsText.append("Name: ${contact[0]}\nPhone: ${contact[1]}\n\n")
                        }
                        contactsTextView.text = allContactsText.toString()
                    } else {
                        wantedNameView.text = "No matching contact found"
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RecordAudioRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Audio Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Audio Permission Denied - Cannot use speech recognition", Toast.LENGTH_SHORT).show()
                }
            }
            ContactsRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Contacts Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Contacts Permission Denied - Cannot access contacts", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Call Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Call Permission Denied - Cannot make phone calls", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val RecordAudioRequestCode = 1
        const val ContactsRequestCode = 2
    }
}