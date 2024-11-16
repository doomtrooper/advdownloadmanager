package com.anand.advancedownloadmanager

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.min

@Composable
fun SegmentedCircularProgressIndicator(
    modifier: Modifier = Modifier,
    segments: List<CircularProgressIndicatorSegment> = listOf(CircularProgressIndicatorSegment()),
    startAngle: Float = 270f,
    style: DrawStyle = Fill,
    useCenter: Boolean = true
) {
    val startAnglesForSegments: List<Float> = segments
        .sortedBy { it.segmentIndex }
        .scan(startAngle) { acc: Float, circularProgressIndicatorSegment -> (acc + (circularProgressIndicatorSegment.segment * 360) / 100) % 360 }
        .subList(0, segments.size + 1)
    val sweepAnglesForSegments: List<Float> = segments
        .sortedBy { it.segmentIndex }
        .map { circularProgressIndicatorSegment ->
            val segmentSweep = (circularProgressIndicatorSegment.segment * 360) / 100
            (circularProgressIndicatorSegment.segmentProgress / 100) * segmentSweep
        }

    Canvas(
        modifier = modifier
    ) {
        println("size: $size")
        val dimension = min(size.width, size.height)
        startAnglesForSegments.zip(sweepAnglesForSegments).forEach {
            drawArc(
                color = Color.White,
                startAngle = it.first,
                sweepAngle = it.second,
                useCenter = useCenter,
                style = style,
                size = Size(dimension, dimension)
            )
        }
    }
}

data class CircularProgressIndicatorSegment(
    val segment: Float = 100f,
    val segmentProgress: Float = 100f,
    val segmentIndex: Int = 1,
    val index: Int = 0,
    val segmentColor: Color = Color.White
)