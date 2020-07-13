package com.dynamicdudes.realar

import android.graphics.Color
import android.media.CamcorderProfile
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.dynamicdudes.realar.Adapter.ModelAdapter
import com.dynamicdudes.realar.Data.Model
import com.dynamicdudes.realar.SupportLibrary.PhotoSaver
import com.dynamicdudes.realar.SupportLibrary.VideoRecorder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_TIME = 1000L

class MainActivity : AppCompatActivity() {

    private val models = mutableListOf(
        Model(R.drawable.jet, "Jet", R.raw.jet),
        Model(
            R.drawable.plant,
            "Plant",
            R.raw.plant
        ),
        Model(
            R.drawable.spiderman,
            "Spider-Man",
            R.raw.spiderman
        ),
        Model(
            R.drawable.hammer,
            "Thor's Hammer",
            R.raw.thor
        ),
        Model(
            R.drawable.hulkmain,
            "Hulk",
            R.raw.hulk
        ),
        Model(
            R.drawable.civil_war,
            "Civil War",
            R.raw.war
        )
    )

    private lateinit var videoRecorder: VideoRecorder
    private lateinit var photoSaver: PhotoSaver
    val viewNodes = mutableListOf<Node>()
    lateinit var arFragment: ArFragment
    lateinit var selectModel: Model
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photoSaver = PhotoSaver(this)
        arFragment = fragment as ArFragment
        videoRecorder = VideoRecorder(this)
            .apply {
                sceneView = arFragment.arSceneView
                setVideoQuality(CamcorderProfile.QUALITY_1080P, resources.configuration.orientation)
            }
        setUpBottomSheet()
        settingUpRecyclerView()
        setupDoubleTapArPlaneListener()
        getCurrentScene().addOnUpdateListener {
            rotateViewNodeTowardUser()
        }
        setUpPhotoButton()
    }

    private fun setUpPhotoButton() {
        fab.setOnClickListener {
            if (!isRecording) {
                photoSaver.takePhoto(arFragment.arSceneView)
            }
        }
        fab.setOnLongClickListener {
            isRecording = videoRecorder.toggleRecordingState()
            //mediaPlayerStart?.start()
            true
        }
        fab.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP && isRecording) {
                isRecording = videoRecorder.toggleRecordingState()
                // mediaPlayerStop?.start()
                Toast.makeText(this, "Video saved to your gallery", Toast.LENGTH_LONG).show()
                true
            } else false
        }
    }


    /*
    Now this function is used for loading models to our scene.
    ModelRenderable - used for 3D Models
    ViewRenderable - text appearing about the model to delete
     */
    private fun loadModels(callback: (ModelRenderable, ViewRenderable) -> Unit) {

        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectModel.modelResourceId)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()
        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                callback(modelRenderable.get(), viewRenderable.get())
            }
            .exceptionally {
                Log.d("MainActivity", "$it")
                Toast.makeText(this, "Failed to load $it", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun setupDoubleTapArPlaneListener() {
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (firstTapTime == 0L) {
                firstTapTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - firstTapTime < DOUBLE_TAP_TIME) {
                firstTapTime = 0L
                loadModels { modelRenderable, viewRenderable ->
                    addNodeToScene(hitResult.createAnchor(), modelRenderable, viewRenderable)
                }
            } else {
                firstTapTime = System.currentTimeMillis()
            }
        }
    }

    private fun rotateViewNodeTowardUser() {
        for (node in viewNodes) {
            node.renderable?.let {
                val camPos = getCurrentScene().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    /*
    After loading model we have to create a scene
     */
    fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }
        viewNodes.add(viewNode)
        modelNode.setOnTapListener { _, _ ->
            if (!modelNode.isTransforming) {
                if (viewNode.renderable == null) {
                    viewNode.renderable = viewRenderable
                } else {
                    viewNode.renderable = null
                }
            }
        }

    }

    /*
    We have to get the current scene this function helps to get Current scene.
     */
    private fun getCurrentScene() = arFragment.arSceneView.scene

    /*
    This funtion used to set up recycler_view....
     */
    private fun settingUpRecyclerView() {
        recycler_view.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler_view.adapter = ModelAdapter(
            models
        ).apply {
            selectedModel.observe(this@MainActivity, Observer {
                this@MainActivity.selectModel = it
                val newTitle = "Selected Model : ${it.title}"
                tv_model.text = newTitle
            })
        }
    }

    /*
    This function used to setup bottom sheet navigation.. In This app it will look like Yellow Color
     */
    private fun setUpBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BOTTOM_SHEET_PEEK_HEIGHT,
            resources.displayMetrics
        ).toInt()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        })
    }

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundResource(R.drawable.my_button)
            setTextColor(Color.WHITE)
        }
    }

}
