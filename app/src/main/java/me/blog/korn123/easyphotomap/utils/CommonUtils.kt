package me.blog.korn123.easyphotomap.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.location.Address
import android.location.Geocoder
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.imaging.jpeg.JpegProcessingException
import com.drew.metadata.exif.GpsDirectory
import me.blog.korn123.easyphotomap.constants.Constant
import me.blog.korn123.easyphotomap.models.ThumbnailItem
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by CHO HANJOONG on 2016-07-21.
 */
class CommonUtils {

    companion object {

        @JvmStatic @Volatile private var mGeoCoder: Geocoder? = null

        @JvmStatic val dateTimePattern = SimpleDateFormat("yyyy-MM-dd(EEE) HH:mm", Locale.getDefault())

        private val MAX_RETRY = 5

        private fun getGeoCoderInstance(context: Context): Geocoder = mGeoCoder?.let { it } ?: Geocoder(context, Locale.getDefault())

        fun initWorkingDirectory() {
            if (!File(Constant.WORKING_DIRECTORY).exists()) {
                File(Constant.WORKING_DIRECTORY).mkdirs()
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun getFromLocation(context: Context, latitude: Double, longitude: Double, maxResults: Int, retryCount: Int): List<Address>? {
            val lat = java.lang.Double.parseDouble(String.format("%.6f", latitude))
            val lon = java.lang.Double.parseDouble(String.format("%.7f", longitude))
            val listAddress: List<Address>?
            try {
                listAddress = getGeoCoderInstance(context).getFromLocation(lat, lon, maxResults)
            } catch (e: Exception) {
                if (retryCount < MAX_RETRY) {
                    return getFromLocation(context, lat, lon, maxResults, retryCount + 1)
                }
                throw Exception(e.message)
            }

            return listAddress
        }

        @JvmStatic
        @Throws(Exception::class)
        fun getFromLocationName(context: Context, locationName: String, maxResults: Int, retryCount: Int): List<Address>? {
            var count = retryCount
            val geoCoder = Geocoder(context, Locale.getDefault())
            val listAddress: List<Address>?
            try {
                listAddress = geoCoder.getFromLocationName(locationName, maxResults)
            } catch (e: Exception) {
                if (count < MAX_RETRY) {
                    return getFromLocationName(context, locationName, maxResults, ++count)
                }
                throw Exception(e.message)
            }

            return listAddress
        }

        fun <K, V : Comparable<V>> entriesSortedByValues(map: Map<K, V>): List<Map.Entry<K, V>> {
            val sortedEntries = ArrayList(map.entries)
            Collections.sort(sortedEntries) { e1, e2 -> e2.value.compareTo(e1.value) }
            return sortedEntries
        }

        fun <K, V : Comparable<V>> entriesSortedByKeys(map: Map<K, V>): List<Map.Entry<K, V>> {
            val sortedEntries = ArrayList(map.entries)
            Collections.sort(sortedEntries) { e1, e2 -> e2.key.toString().compareTo(e1.key.toString()) }
            return sortedEntries
        }

        fun bindButtonEffect(targetView: View) {
            val onTouchListener = View.OnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundColor(0x5fef1014)
                } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                    view.setBackgroundColor(0x00ffffff)
                }
                false
            }
            targetView.setOnTouchListener(onTouchListener)
        }

        @JvmStatic
        fun saveStringPreference(context: Context, key: String, value: String) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val edit = preferences.edit()
            edit.putString(key, value)
            edit.apply()
        }

        @JvmStatic
        fun fetchAllThumbnail(context: Context): List<ThumbnailItem> {
            val projection = arrayOf(MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID)
            val imageCursor = context.contentResolver.query(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, // 이미지 컨텐트 테이블
                    projection, null, null,
                    MediaStore.Images.Thumbnails.DATA + " desc")
            val result = ArrayList<ThumbnailItem>()

            when (imageCursor.moveToFirst()) {
                true -> {
                    val dataColumnIndex = imageCursor.getColumnIndex(projection[0])
                    val idColumnIndex = imageCursor.getColumnIndex(projection[1])
                    do {
                        val filePath = imageCursor.getString(dataColumnIndex)
                        val imageId = imageCursor.getString(idColumnIndex)

                        //                Uri thumbnailUri = uriToThumbnail(context, imageId);
                        //                Uri imageUri = Uri.parse(filePath);
                        //                Log.i("fetchAllImages", imageUri.toString());
                        // 원본 이미지와 썸네일 이미지의 uri를 모두 담을 수 있는 클래스를 선언합니다.
                        val photo = ThumbnailItem(imageId, "", filePath)
                        result.add(photo)
                    } while (imageCursor.moveToNext())
                    imageCursor.close()
                }
                false -> {
                    // imageCursor is empty
                }
            }

            return result.filter {
                File(it.thumbnailPath).exists()
            }
        }

        @JvmStatic
        fun fetchAllImages(context: Context): List<ThumbnailItem> {
            // DATA는 이미지 파일의 스트림 데이터 경로를 나타냅니다.
            val projection = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
            val imageCursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // 이미지 컨텐트 테이블
                    projection, null, null,
                    MediaStore.Images.Media.DATA + " asc")        // DATA, _ID를 출력
            val result = ArrayList<ThumbnailItem>(imageCursor!!.count)
            when(imageCursor.moveToFirst()) {
                true -> {
                    val dataColumnIndex = imageCursor.getColumnIndex(projection[0])
                    val idColumnIndex = imageCursor.getColumnIndex(projection[1])
                    do {
                        val filePath = imageCursor.getString(dataColumnIndex)
                        val imageId = imageCursor.getString(idColumnIndex)

                        //                Uri thumbnailUri = uriToThumbnail(context, imageId);
                        //                Uri imageUri = Uri.parse(filePath);
                        //                Log.i("fetchAllImages", imageUri.toString());
                        // 원본 이미지와 썸네일 이미지의 uri를 모두 담을 수 있는 클래스를 선언합니다.
                        val photo = ThumbnailItem(imageId, filePath, "")
                        result.add(photo)
                    } while (imageCursor.moveToNext())
                    imageCursor.close()
                }
                false -> {
                    // imageCursor is empty
                }
            }
            return result
        }

        @JvmStatic
        fun getOriginImagePath(context: Context, imageId: String): String? {
            // DATA는 이미지 파일의 스트림 데이터 경로를 나타냅니다.
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val contentResolver = context.contentResolver

            // 원본 이미지의 _ID가 매개변수 imageId인 썸네일을 출력
            val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Images.Media._ID + "=?",
                    arrayOf(imageId), null)
            if (cursor == null) {
                // Error 발생
                // 적절하게 handling 해주세요
            } else if (cursor.moveToFirst()) {
                val thumbnailColumnIndex = cursor.getColumnIndex(projection[0])
                val path = cursor.getString(thumbnailColumnIndex)
                cursor.close()
                return path
            }
            return null
        }

        fun getGPSDirectory(filePath: String): GpsDirectory? {
            var gpsDirectory: GpsDirectory? = null
            try {
                val metadata = JpegMetadataReader.readMetadata(File(filePath))
                gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
            } catch (e: JpegProcessingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return gpsDirectory
        }

        @JvmStatic
        fun fullAddress(address: Address): String {
            val sb = StringBuilder()
            if (address.countryName != null) sb.append(address.countryName).append(" ")
            if (address.adminArea != null) sb.append(address.adminArea).append(" ")
            if (address.locality != null) sb.append(address.locality).append(" ")
            if (address.subLocality != null) sb.append(address.subLocality).append(" ")
            if (address.thoroughfare != null) sb.append(address.thoroughfare).append(" ")
            if (address.featureName != null) sb.append(address.featureName).append(" ")
            return sb.toString()
        }

        fun readDataFile(targetPath: String): List<String>? {
            val inputStream: InputStream?
            var listData: List<String>? = null
            try {
                inputStream = FileUtils.openInputStream(File(targetPath))
                listData = IOUtils.readLines(inputStream, "UTF-8")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return listData
        }

        @JvmStatic
        fun dpToPixel(context: Context, dp: Float, policy: Int = 0): Int {
            val px: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
            return when (policy) {
                0 -> Math.floor(px.toDouble()).toInt()
                1 -> Math.ceil(px.toDouble()).toInt()
                else -> 0
            }
        }

        @JvmStatic
        fun dpToPixel(context: Context, dp: Float): Int = dpToPixel(context, dp, 0)

        @JvmStatic
        fun getDisplayOrientation(activity: Activity): Int {
            val display = activity.windowManager.defaultDisplay
            return display.orientation
        }

        @JvmStatic
        fun getDefaultDisplay(activity: Activity): Point {
            val display = activity.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            return size
        }
    }
}
