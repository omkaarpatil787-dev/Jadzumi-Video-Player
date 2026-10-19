package com.jadzumi.video

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable fun PlayPauseMorph(playing:Boolean,reducedMotion:Boolean,modifier:Modifier=Modifier) {
    val fraction by animateFloatAsState(if(playing) 1f else 0f,
        animationSpec=if(reducedMotion) snap() else spring(dampingRatio=0.8f,stiffness=500f),label="Play pause morph")
    Canvas(modifier.semantics { contentDescription=if(playing) "Pause" else "Play" }) {
        val play=arrayOf(floatArrayOf(.28f,.18f,.52f,.34f,.52f,.66f,.28f,.82f),floatArrayOf(.52f,.34f,.78f,.5f,.78f,.5f,.52f,.66f))
        val pause=arrayOf(floatArrayOf(.25f,.2f,.43f,.2f,.43f,.8f,.25f,.8f),floatArrayOf(.57f,.2f,.75f,.2f,.75f,.8f,.57f,.8f))
        for(i in 0..1) {
            val path=Path()
            for(j in 0..3) {
                val x=(play[i][j*2]+(pause[i][j*2]-play[i][j*2])*fraction)*size.width
                val y=(play[i][j*2+1]+(pause[i][j*2+1]-play[i][j*2+1])*fraction)*size.height
                if(j==0) path.moveTo(x,y) else path.lineTo(x,y)
            }
            path.close();drawPath(path,Color.White)
        }
    }
}
