package studio.dipdev.flutter.amazon.s3

import android.content.Context
import android.util.Log

import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client

import java.io.File
import java.io.UnsupportedEncodingException
import java.util.Locale

class AwsHelper(private val context: Context, private val onUploadCompleteListener: OnUploadCompleteListener, private val BUCKET_NAME: String, private val IDENTITY_POOL_ID: String, private val KEY: String) {

    private fun getUploadedUrl(key: String?): String {
        return String.format(Locale.getDefault(), URL_TEMPLATE, BUCKET_NAME, key)
    }

    @Throws(UnsupportedEncodingException::class)
    fun uploadImage(image: File): String {
        val credentialsProvider = CognitoCachingCredentialsProvider(context, IDENTITY_POOL_ID, Regions.EU_WEST_1)

        val amazonS3Client = AmazonS3Client(credentialsProvider)
        amazonS3Client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.EU_WEST_1))
        val transferUtility = TransferUtility(amazonS3Client, context)

        val transferObserver = transferUtility.upload(BUCKET_NAME, KEY, image)

        transferObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    onUploadCompleteListener.onUploadComplete(getUploadedUrl(KEY))
                }
                if (state == TransferState.FAILED) {
                    onUploadCompleteListener.onFailed()
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
            override fun onError(id: Int, ex: Exception) {
                Log.e(TAG, "error in upload id [ " + id + " ] : " + ex.message)
            }
        })
        return getUploadedUrl(KEY)
    }

    interface OnUploadCompleteListener {
        fun onUploadComplete(imageUrl: String)
        fun onFailed()
    }

    companion object {
        private val TAG = AwsHelper::class.java.simpleName
        private const val URL_TEMPLATE = "https://s3-eu-west-1.amazonaws.com/%s/%s"
    }
}