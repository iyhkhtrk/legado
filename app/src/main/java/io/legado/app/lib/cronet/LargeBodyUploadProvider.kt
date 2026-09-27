package io.legado.app.lib.cronet

import androidx.annotation.Keep
import okhttp3.RequestBody
import okio.BufferedSource
import okio.Pipe
import okio.buffer
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService

/**
 * 用于上传大型文件
 *
 * @property body
 * @property executorService
 */
@Keep
class LargeBodyUploadProvider(
    private val body: RequestBody,
    private val executorService: ExecutorService
) : UploadDataProvider(), AutoCloseable {
    private var pipe = Pipe(BUFFER_SIZE.toLong())
    private var source: BufferedSource = pipe.source.buffer()

    @Volatile
    private var filled: Boolean = false
    override fun getLength(): Long {
        return body.contentLength()
    }

    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        if (!filled) {
            fillBuffer()
        }
        check(byteBuffer.hasRemaining()) { "Cronet passed a buffer with no bytes remaining" }
        var read: Int
        var bytesRead = 0
        while (bytesRead == 0) {
            read = source.read(byteBuffer)
            if (read < 0) break
            bytesRead += read
        }
        if (bytesRead == 0) {
            if (getLength() < 0) {
                uploadDataSink.onReadSucceeded(true)
            } else {
                uploadDataSink.onReadError(IOException("body ended prematurely"))
            }
            return
        }
        uploadDataSink.onReadSucceeded(false)
    }

    @Synchronized
    private fun fillBuffer() {
        filled = true
        executorService.submit {
            try {
                val writeSink = pipe.sink.buffer()
                body.writeTo(writeSink)
                writeSink.flush()
            } catch (e: Throwable) {
                e.printStackTrace()
            }

        }

    }

    override fun rewind(uploadDataSink: UploadDataSink?) {
        check(!body.isOneShot()) { "Okhttp RequestBody is OneShot" }
        pipe.cancel()
        pipe = Pipe(BUFFER_SIZE.toLong())
        source = pipe.source.buffer()
        filled = false
        fillBuffer()
        uploadDataSink?.onRewindSucceeded()
    }

    override fun close() {
        pipe.cancel()
//        source.close()
        super.close()
    }
}