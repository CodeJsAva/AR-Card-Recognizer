package com.app.ardemo

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition


class MainActivity : AppCompatActivity() {
    var arFragment: ArFragment? = null
    var frameUpdated = false
    var isCreated = false

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arFragment?.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            createObject(hitResult.createAnchor())
        }
        arFragment?.arSceneView?.scene?.addOnUpdateListener { frameTime ->
            val frame = arFragment?.arSceneView?.arFrame
            val augmentedImages = frame?.getUpdatedTrackables(AugmentedImage::class.java)
            if (!frameUpdated) {
                Toast.makeText(this, "Frames are updating", Toast.LENGTH_SHORT).show()
                frameUpdated = true;
            }
            frame?.acquireCameraImage()
            augmentedImages?.forEach { image ->
                if (image.trackingState === TrackingState.TRACKING && !isCreated) {
                    // When image detected, acquire image and pass it to text recognizer
                    try {
                        val imageObject = frame.acquireCameraImage()!!
                        readTextFromImage(imageObject);
                    } catch (e: NotYetAvailableException) {

                    }
                    createObject(image.createAnchor(image.centerPose))
                    isCreated = true
                } else {
                    isCreated = false
                }
            }
        }
    }


    private fun getCameraRotation(): Int {
        val cameraId = arFragment?.arSceneView?.session?.cameraConfig?.cameraId
        val deviceRotation = display!!.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        return try {
            val sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId!!)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            // only back camera
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360

            rotationCompensation
        } catch (e: NullPointerException) {
            ORIENTATIONS[0]
        }

    }

    private fun readTextFromImage(image: Image) {
        val textRecognition = TextRecognition.getClient()
        val inputImage=InputImage.fromMediaImage(image, getCameraRotation())
        textRecognition.process(inputImage)
            .addOnSuccessListener { visionText ->
                Toast.makeText(this, visionText.text, Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error - ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createObject(anchor: Anchor) {
        ModelRenderable.builder()
            .setSource(arFragment?.context, R.raw.cat)
            .build()
            .thenAccept { t: ModelRenderable? -> addAnchorNode(anchor, t) }
            .exceptionally { t ->
                t.message?.let { Log.d("AR-VIEW", it) }
                null
            }

    }

    private fun addAnchorNode(anchor: Anchor, renderable: ModelRenderable?) {
        val anchorNode = AnchorNode(anchor)

        val node = TransformableNode(arFragment?.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        // node.localPosition = Vector3(0.3f, 0.1f, 0f)
        // node.localScale = Vector3(0.01f, 0.01f, 0f)


        ViewRenderable.builder()
            .setView(this, R.layout.text)
            .build()
            .thenAccept { t: ViewRenderable? ->
                val node = TransformableNode(arFragment?.transformationSystem)
                node.renderable = t
                node.setParent(anchorNode)
                // node.localPosition = Vector3(0.3f, 0.1f, 0f)
                // node.localScale = Vector3(1.0f, 1.0f, 0f)
                node.select()
            }
            .exceptionally { e ->
                e.message?.let { Log.d("AR-VIEW", it) }
                null
            }
        arFragment?.arSceneView?.scene?.addChild(anchorNode)
        node.select()
    }

}