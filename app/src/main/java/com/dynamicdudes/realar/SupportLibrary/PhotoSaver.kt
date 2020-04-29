package com.dynamicdudes.realar.SupportLibrary

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import com.dynamicdudes.realar.R
import com.google.ar.sceneform.ArSceneView
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class PhotoSaver(private val activity : Activity) {

    private var mediaPlayer : MediaPlayer? = MediaPlayer.create(activity,
        R.raw.shutter
    )

    private fun generateFileName() : String?{

        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath + "/RealAR/${date}_screenshot.jpg"

    }

    private fun saveBitmapImage(bmp : Bitmap,filename : String){
        val out = File(filename)
        if(!out.parentFile.exists()){
            out.parentFile.mkdirs()
        }
        try{
            val outputStream = FileOutputStream(filename)
            saveDataToGallery(bmp,outputStream)
        }catch (e : IOException){
            Toast.makeText(activity,"Failed to save image $e",Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveDataToGallery(bmp:Bitmap,outputStream: OutputStream){
        val outputData = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
        outputData.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
    }

    //above api 29

    private fun saveBitmapImage(bmp: Bitmap){
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,"${date}_screenshot.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"DCIM/RealAR")
        }
        val uri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
        activity.contentResolver.openOutputStream(uri?: return).use {outputStream->
            outputStream?.let {
                try {
                    saveDataToGallery(bmp,outputStream)
                    mediaPlayer?.start()
                }catch (e : IOException){
                    Toast.makeText(activity,"Failed to save image",Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun takePhoto(arSceneView: ArSceneView){

        val bmp = Bitmap.createBitmap(arSceneView.width,arSceneView.height,Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopyThread")
        handlerThread.start()
        PixelCopy.request(arSceneView,bmp,{
            result->
            if(result == PixelCopy.SUCCESS){
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                    val filename = generateFileName()
                    saveBitmapImage(bmp, filename?: return@request)
                }else{
                    //API About 29
                    saveBitmapImage(bmp)
                }
                activity.runOnUiThread {
                    mediaPlayer?.start()
                    //Toast.makeText(activity,"Image saved successfully",Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(activity,"Image failed to save",Toast.LENGTH_SHORT).show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))

    }


}