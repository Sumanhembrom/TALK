package com.example.talk

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.talk.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityMainBinding

    //if code sending fails, we wil use resend
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    private var mCallBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId: String? = null

    private lateinit var firebaseAuth: FirebaseAuth
    private val TAG = "MAIN_TAG"

    //progress dialog
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.phoneLo.visibility = View.VISIBLE
        binding.OTPLo.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

                Log.d(TAG, "onVerificationCompleted: ")
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Log.d(TAG, "onVerificationFailed: $(e.message)")
                Toast.makeText(this@MainActivity, "$(e.message)", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {

                Log.d(TAG, "onCodeSent: $verificationId")
                mVerificationId = verificationId
                forceResendingToken = token
                progressDialog.dismiss()

                Log.d(TAG, "onCodeSent: $verificationId")

                binding.phoneLo.visibility = View.GONE
                binding.OTPLo.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Verification Code Sent...", Toast.LENGTH_SHORT).show()
                binding.OTPSentDescription.text = "Please type the OTP we sent to ${binding.phonenumber.text.toString().trim()}"
            }
        }

        binding.getotp.setOnClickListener {
            //input phone number
            val phone = binding.phonenumber.text.toString().trim()
            //validate phone number
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this@MainActivity, "please enter phone number", Toast.LENGTH_SHORT).show()
            }
            else{
                startPhoneNumberVerification(phone)
            }
        }

        binding.resendCode.setOnClickListener {
            //input phone number
            val phone = binding.phonenumber.text.toString().trim()
            //validate phone number
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this@MainActivity, "please enter phone number", Toast.LENGTH_SHORT).show()
            }
            else{
                resendVerificationCode(phone, forceResendingToken)
            }
        }

        binding.Submit.setOnClickListener {
            //input verification code
            val code = binding.OTP.text.toString().trim()
            if(TextUtils.isEmpty(code)){
                Toast.makeText(this@MainActivity, "please enter the OTP sent", Toast.LENGTH_SHORT).show()
            }
            else{
                verifyPhoneNumberWithCode(mVerificationId, code)
            }
        }
    }

    private fun startPhoneNumberVerification(phone: String){
        Log.d(TAG, "startPhoneNumberVerification: $phone")
        progressDialog.setMessage("Verifying phone number..")
        progressDialog.show()

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun resendVerificationCode(phone: String, token: PhoneAuthProvider.ForceResendingToken?){
        progressDialog.setMessage("Resending Code..")
        progressDialog.show()

        Log.d(TAG, "resendVerificationCode: $phone")

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String){
        Log.d(TAG, "verifyPhoneNumberWithCode: $verificationId $code")
        progressDialog.setMessage("Verifying Code...")
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId , code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: ")
        progressDialog.setMessage("Logging in...")

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener{
                progressDialog.dismiss()
                val phone = firebaseAuth.currentUser.phoneNumber
                Toast.makeText(this, "Logged in as $phone", Toast.LENGTH_SHORT).show()

                //start profile activity
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this, "$(e.message)", Toast.LENGTH_SHORT).show()

            }
    }

}