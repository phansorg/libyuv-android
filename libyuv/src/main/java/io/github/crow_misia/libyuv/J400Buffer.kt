package io.github.crow_misia.libyuv

import android.graphics.Rect
import java.nio.ByteBuffer

/**
 * J400 (jpeg grey) YUV Format. 4:0:0 8bpp
 */
class J400Buffer private constructor(
    buffer: ByteBuffer?,
    override val planeY: Plane,
    override val width: Int,
    override val height: Int,
    cropRect: Rect,
    releaseCallback: Runnable?,
) : AbstractBuffer(buffer, cropRect, arrayOf(planeY), releaseCallback), BufferX400<J400Buffer, J420Buffer>, BufferY<J400Buffer> {
    override fun getPlaneOffset(planeIndex: Int, rowStride: RowStride, left: Int, top: Int): Int {
        return rowStride * top + left
    }

    fun convertTo(dst: ArgbBuffer) {
        val (fixedWidth, fixedHeight) = calculateSize(dst)
        Yuv.convertJ400ToARGB(
            srcY = planeY.buffer, srcStrideY = planeY.rowStride, srcOffsetY = offset(0),
            dstARGB = dst.plane.buffer, dstStrideARGB = dst.plane.rowStride, dstOffsetARGB = dst.offset(0),
            width = fixedWidth, height = fixedHeight,
        )
    }

    companion object Factory : BufferFactoryY<J400Buffer>, CapacityCalculator<Plane1Capacities> {
        override fun calculate(width: Int, height: Int): Plane1Capacities {
            return Plane1Capacities(
                planeStride = RowStride(width),
                planeCapacity = Capacity(width * height),
            )
        }

        override fun allocate(width: Int, height: Int, cropRect: Rect): J400Buffer {
            val (capacity, stride) = calculate(width, height)
            val (buffer) = createByteBuffer(listOf(capacity))
            return J400Buffer(
                buffer = buffer,
                planeY = PlanePrimitive(stride, buffer),
                width = width,
                height = height,
                cropRect = cropRect,
            ) {
                Yuv.freeNativeBuffer(buffer)
            }
        }

        override fun wrap(buffer: ByteBuffer, width: Int, height: Int, cropRect: Rect): J400Buffer {
            check(buffer.isDirect) { "Unsupported non-direct ByteBuffer." }

            val (capacity, stride) = calculate(width, height)
            val sliceBuffer = buffer.sliceRange(0, capacity.value)
            return J400Buffer(
                buffer = sliceBuffer,
                planeY = PlanePrimitive(stride, sliceBuffer),
                width = width,
                height = height,
                cropRect = cropRect,
                releaseCallback = null,
            )
        }

        override fun wrap(planeY: Plane, width: Int, height: Int, cropRect: Rect): J400Buffer {
            return J400Buffer(
                buffer = planeY.buffer,
                planeY = planeY,
                width = width,
                height = height,
                cropRect = cropRect,
                releaseCallback = null,
            )
        }
    }
}
