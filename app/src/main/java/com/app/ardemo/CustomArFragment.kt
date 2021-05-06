package com.app.ardemo

import android.widget.Toast
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.io.InputStream

class CustomArFragment : ArFragment() {

    override fun getSessionConfiguration(session: Session?): Config {

        planeDiscoveryController.setInstructionView(null);
        val inputStream: InputStream? = context?.assets?.open("new_image.imgdb")
        val imageDatabase =
            AugmentedImageDatabase.deserialize(session, inputStream)
        val config = Config(session)
        config.augmentedImageDatabase = imageDatabase
        session?.configure(config)
        arSceneView?.setupSession(session)
        return config
    }
}