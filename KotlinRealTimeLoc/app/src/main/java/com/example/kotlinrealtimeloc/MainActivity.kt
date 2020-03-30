package com.example.kotlinrealtimeloc

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.kotlinrealtimeloc.Utils.Common
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import java.util.*
import com.firebase.ui.auth.data.model.User as User
import java.util.jar.Manifest as Manifest1

class MainActivity : AppCompatActivity() {

    companion object
    {
        private val MY_REQUEST_CODE = 7117
    }

    lateinit var user_information:DatabaseReference
    lateinit var providers:List<AuthUI.IdpConfig>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Paper.init(this)

        user_information =FirebaseDatabase.getInstance().getReference(Common.USER_INFORMATION)

        providers = Arrays.asList<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object :PermissionListener{
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    showSignInOptions()

                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@MainActivity,"You must accept this permission",Toast.LENGTH_SHORT).show()
                }


            }).check()

    }

    private fun showSignInOptions() {
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build(), MY_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == MY_REQUEST_CODE)
        {
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            user_information.orderByKey()
                .equalTo(firebaseUser!!.uid)
                .addListenerForSingleValueEvent(object:ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if(dataSnapshot.value == null)
                        {
                            if(!dataSnapshot.child(firebaseUser!!.uid).exists())
                            {
                                Common.loggedUser = com.example.kotlinrealtimeloc.Model.User(firebaseUser.uid,firebaseUser.email!!)

                                user_information.child(Common.loggedUser.uid!!)
                                    .setValue(Common.loggedUser)
                            }

                        }
                        else
                        {
                            Common.loggedUser = dataSnapshot.child(firebaseUser.uid)
                                .getValue(com.example.kotlinrealtimeloc.Model.User::class.java)!!
                        }
                        Paper.book().write(Common.USER_UID_SAVE_KEY,Common.loggedUser.uid)
                        updateToken(firebaseUser)
                        setupUI()

                    }


                })
        }
    }

    private fun setupUI() {
        startActivity(Intent (this@MainActivity,HomeActivity::class.java))
        finish()
    }

    private fun updateToken(firebaseUser: FirebaseUser) {
        val tokens = FirebaseDatabase.getInstance()
            .getReference(Common.TOKENS);


            FirebaseInstanceId.getInstance().instanceId
                .addOnSuccessListener { instanceIdResult ->
                    tokens.child(firebaseUser!!.uid)
                        .setValue(instanceIdResult.token)
                }.addOnFailureListener { e->
                    Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_SHORT).show()

                }
    }


}
