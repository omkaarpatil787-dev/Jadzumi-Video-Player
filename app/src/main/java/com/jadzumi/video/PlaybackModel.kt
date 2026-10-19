package com.jadzumi.video

import kotlin.math.roundToInt

/** Unknown duration is represented as zero; do not turn it into a negative seek. */
fun boundedPosition(position: Long, duration: Long): Long = if(duration > 0) position.coerceIn(0,duration) else position.coerceAtLeast(0)
fun resumePosition(position: Long, duration: Long, completed: Boolean): Long =
    if(completed) 0 else if(duration>0) position.coerceIn(0,duration-1) else position.coerceAtLeast(0)
fun seekPosition(position: Long, delta: Long, duration: Long): Long {
    // Saturate before adding; floating-point conversion loses exact millisecond values.
    val target = when {
        delta > 0 && position > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0 && position < Long.MIN_VALUE - delta -> Long.MIN_VALUE
        else -> position + delta
    }
    return boundedPosition(target, duration)
}

data class VideoContentBounds(val left:Int,val top:Int,val right:Int,val bottom:Int)

/** Visible content rectangle relative to the player, excluding fit-mode letterboxing. */
fun videoContentBounds(width:Int,height:Int,aspect:Float,fit:Boolean):VideoContentBounds {
    val w=width.coerceAtLeast(0)
    val h=height.coerceAtLeast(0)
    if(!fit || w==0 || h==0 || !aspect.isFinite() || aspect<=0f) return VideoContentBounds(0,0,w,h)
    val ratio=aspect.toDouble()
    val contentWidth=if(w.toDouble()/h>ratio) (h*ratio).roundToInt().coerceIn(1,w) else w
    val contentHeight=if(w.toDouble()/h>ratio) h else (w/ratio).roundToInt().coerceIn(1,h)
    val left=(w-contentWidth)/2
    val top=(h-contentHeight)/2
    return VideoContentBounds(left,top,left+contentWidth,top+contentHeight)
}
