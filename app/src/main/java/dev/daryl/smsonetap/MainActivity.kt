package dev.daryl.smsonetap

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import dev.daryl.smsonetap.databinding.ActivityMainBinding
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initSmsRetriever()
    }

    private fun initSmsRetriever() {
        /**
         * If you know the sender's phone number, pass that as the parameter for startSmsUserConsent
         * If not, pass null
         */
        SmsRetriever.getClient(this).startSmsUserConsent(null)
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(
            smsVerificationReceiver,
            intentFilter
        ) // Use requireContext.registerReceiver(), if you're calling from a fragment
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsVerificationReceiver)
    }

    /**
     * Initializing runtime broadcast receiver, this doesn't need to be handled in the manifest
     * Make sure you unregister in your onDestroy or onDestroyView
     */
    private val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                val extras = intent.extras
                val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                when (smsRetrieverStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // Get consent intent
                        val consentIntent =
                            extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            // Start activity to show consent dialog to user, activity must be started in
                            // 5 minutes, otherwise you'll receive another TIMEOUT intent
                            smsReceiverIntent.launch(consentIntent)
                        } catch (e: ActivityNotFoundException) {
                            // Handle the exception ...
                            Log.e(TAG, e.message!!)
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        Log.e(TAG, "Timed out")
                    }
                }
            }
        }
    }

    val smsReceiverIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    val message = it.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    retrieveOtp(message)
                }
                Activity.RESULT_CANCELED -> Log.e(TAG, "SMS auto fetch is cancelled")
            }
        }

    /**
     * Uses a regex to filter out a 4 digit OTP from the received SMS. Change the regex and SMS according to your needs
     * Example SMS: Your OTP is 1232
     */
    private fun retrieveOtp(message: String?) {
        val pattern = Pattern.compile("\\b\\d{4}\\b")
        val matcher = pattern.matcher(message.toString())
        if (matcher.find()) {
            val otp = matcher.group(0)
            binding.textOtp.text = otp
        }
    }
}
