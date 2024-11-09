package com.anand.advancedownloadmanager.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill

@Composable
fun SegmentedCircularProgressIndicator(
    modifier: Modifier = Modifier,
    segments: List<CircularProgressIndicatorSegment> = listOf(CircularProgressIndicatorSegment()),
    startAngle: Float = 270f,
    style: DrawStyle = Fill,
    useCenter: Boolean = true
) {
    segments.forEach { println("[SCPI]: $it") }
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

//    println("***********")
//    startAnglesForSegments.zip(sweepAnglesForSegments).forEach { println(it) }
//    println("***********")

    Canvas(modifier = modifier) {
        startAnglesForSegments.zip(sweepAnglesForSegments).forEach {
            drawArc(
                color = Color.White,
                startAngle = it.first,
                sweepAngle = it.second,
                useCenter = useCenter,
                style = style,
                size = Size(size.minDimension, size.minDimension)
            )
        }
    }
}

data class CircularProgressIndicatorSegment(
    val segment: Float = 100f,
    val segmentProgress: Float = 100f,
    val segmentIndex: Int = 1,
    val segmentColor: Color = Color.White
)