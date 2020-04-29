package com.dynamicdudes.realar.Fragment

import com.google.ar.sceneform.ux.ArFragment
import android.Manifest

class CustomArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String> {
        val additionPermission = super.getAdditionalPermissions()
        val permissionLength = additionPermission.size
        val permissions = Array(permissionLength + 1) {Manifest.permission.WRITE_EXTERNAL_STORAGE}
        if(permissionLength > 0){
            System.arraycopy(additionPermission,0,permissions,1,permissionLength)
        }

        return permissions
    }
}