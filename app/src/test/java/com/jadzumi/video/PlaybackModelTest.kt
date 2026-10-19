package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class PlaybackModelTest {
    @Test fun seekClampsBothEnds() {
        assertEquals(0L,seekPosition(2000,-10000,100000))
        assertEquals(100000L,seekPosition(98000,10000,100000))
        assertEquals(30000L,seekPosition(20000,10000,100000))
    }
    @Test fun unknownDurationDoesNotBecomeNegative() {
        assertEquals(30000L,seekPosition(20000,10000,0))
        assertEquals(0L,boundedPosition(-1,0))
    }
    @Test fun resumePreservesUnfinishedEndingAndResetsCompletedVideo() {
        assertEquals(99999L,resumePosition(100000,100000,false))
        assertEquals(0L,resumePosition(100000,100000,true))
        assertEquals(0L,resumePosition(1,1,false))
        assertEquals(42000L,resumePosition(42000,0,false))
    }
    @Test fun extremeSeekDoesNotOverflowIntoNegativePosition() {
        assertEquals(Long.MAX_VALUE,seekPosition(Long.MAX_VALUE,10000,0))
        assertEquals(0L,seekPosition(0,Long.MIN_VALUE,0))
    }
    @Test fun seekPreservesExactIntegerPosition() {
        val position = 9_007_199_254_740_993L
        assertEquals(position, seekPosition(position, 0, 0))
        assertEquals(position - 1, seekPosition(position, -1, 0))
        assertEquals(Long.MAX_VALUE - 1, seekPosition(Long.MAX_VALUE, -1, 0))
        assertEquals(0L, seekPosition(Long.MIN_VALUE, -1, 0))
    }
    @Test fun landscapeVideoExcludesTabletLetterboxing() {
        assertEquals(VideoContentBounds(0,60,1920,1140),videoContentBounds(1920,1200,16f/9f,true))
        assertEquals(VideoContentBounds(0,200,1920,1000),videoContentBounds(1920,1200,2.4f,true))
    }
    @Test fun portraitVideoExcludesPillarboxing() {
        assertEquals(VideoContentBounds(622,0,1297,1200),videoContentBounds(1920,1200,9f/16f,true))
    }
    @Test fun cropAndStretchUseEntirePlayerBounds() {
        assertEquals(VideoContentBounds(0,0,1920,1200),videoContentBounds(1920,1200,2.4f,false))
    }
    @Test fun unknownOrInvalidVideoSizeHasSafeBounds() {
        assertEquals(VideoContentBounds(0,0,1920,1200),videoContentBounds(1920,1200,Float.NaN,true))
        assertEquals(VideoContentBounds(0,0,1920,1200),videoContentBounds(1920,1200,0f,true))
        assertEquals(VideoContentBounds(0,0,0,1200),videoContentBounds(-1,1200,16f/9f,true))
    }
}
