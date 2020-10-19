@file:Suppress("DEPRECATION")

package com.example.socialmedia

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class Login : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var database=FirebaseDatabase.getInstance()
    private var myRef=database.reference
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        ivPerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })
    }


    fun LoginToFirebase(email: String, password: String){
        mAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){ task ->
                if (task.isSuccessful){
                    Toast.makeText(applicationContext, "success login", Toast.LENGTH_SHORT).show()
                    //save in db
                    var  currentUser= mAuth!!.currentUser
                    SaveImageFirebase(currentUser!!)
                }else{
                    Toast.makeText(applicationContext, "failure login", Toast.LENGTH_SHORT).show()
                }

            }
    }


    fun SaveImageFirebase(currentUser:FirebaseUser){
        val storage=FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://socialmedia-ea616.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmss")
        val dataobj=Date()
        val imagePath= Splitstring(currentUser.email.toString())+ df.format(dataobj)+".jpg"
        val imgRef=storageRef.child("images/$imagePath")
        ivPerson.isDrawingCacheEnabled=true
        ivPerson.buildDrawingCache()
        val drawable=ivPerson.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val baos=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data =baos.toByteArray()
        val UploadTask=imgRef.putBytes(data)
        UploadTask.addOnFailureListener{
            Toast.makeText(applicationContext, "failed to upload", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {  taskSnapshot->
            var DownloadURL=taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            myRef.child("user").child(currentUser.uid).child("email").setValue(currentUser.email)
            myRef.child("user").child(currentUser.uid).child("profileImage").setValue(DownloadURL)
            LoadTweets()
        }
    }

    override fun onStart() {
        super.onStart()
    LoadTweets()
    }

    fun LoadTweets(){
        var currentUser=mAuth!!.currentUser
        if (currentUser!=null){
            var intent=Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentUser.email)
            intent.putExtra("uid",currentUser.uid)
            startActivity(intent)
        }

    }

    fun Splitstring(email:String):String{
        val split= email.split("@")
return split[0]
    }


    private fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                return
            }
        }
        loadImage()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            0 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                } else {
                    Toast.makeText(this, "cannot access image", Toast.LENGTH_LONG).show()
                }

            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }
val PICKIMG=12
    private fun loadImage() {
        //TODO("Not yet implemented")
        var intent=Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).apply {
            type="image/*"
            startActivityForResult(this, PICKIMG)
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode==PICKIMG && resultCode== RESULT_OK && data!=null){
            val selectedImage=data.data
            val source=ImageDecoder.createSource(contentResolver, selectedImage!!)
            val bit=ImageDecoder.decodeBitmap(source)
            ivPerson.setImageBitmap(bit)

        /*    val filePathColumn= arrayOf(MediaStore.Images.Media.DATA)
            val cursor= contentResolver.query(selectedImage!!,filePathColumn,null,null,null)
            cursor?.moveToFirst()
            val columnIndex=cursor!!.getColumnIndex(filePathColumn[0])
            val picPath=cursor!!.getString(columnIndex)
            cursor!!.close()
            ivPerson.setImageBitmap(BitmapFactory.decodeFile(picPath)) */
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun buLogin(view: View) {
        LoginToFirebase(etEmail.text.toString(), etPassword.text.toString())
    }


}