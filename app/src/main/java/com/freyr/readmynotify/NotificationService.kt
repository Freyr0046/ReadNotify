package com.freyr.readmynotify

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*


class NotificationService : NotificationListenerService() {
    private val TAG = this.javaClass.simpleName
    private var tts: TextToSpeech? = null
    //是否唸出聲音
    var needSpeak = true

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val mNotification: Notification? = sbn.notification
        // 包名
        val notificationPkg = sbn.packageName
//        Log.d(TAG, "onNotificationPosted：包名：$notificationPkg")
        if (mNotification != null) {
            val extras: Bundle = mNotification.extras
            // 標題
            val notificationTitle = extras.getString(Notification.EXTRA_TITLE)
            // 內容
            val notificationText = extras.getString(Notification.EXTRA_TEXT)

            //2023.03.28 暫時只接收CATEGORY_MESSAGE的推播
            when (mNotification.category) {
                Notification.CATEGORY_SYSTEM, Notification.CATEGORY_SERVICE, null -> {
                    //do nothing
                }
                else -> {
                    //Line訊息 重複問題
                    if (sbn.tag != null) {
                        val speakMsg = StringBuilder("")
                        Log.d(TAG, "onNotificationPosted：包名：$notificationPkg")
//                        Log.d(TAG, "推播：包名：$notificationPkg\n" +
//                                "title：${notificationTitle}\n" +
//                                "content：${notificationText}\n" +
//                                "category：${mNotification.category}\n" +
//                                "number：${mNotification.number}\n" +
//                                "publicVersion：${mNotification.publicVersion}\n" +
//                                "tickerText：${mNotification.tickerText}\n" +
//                                "EXTRA_SUB_TEXT：${extras.getString(Notification.EXTRA_SUB_TEXT)}\n"+
//                                "EXTRA_SUMMARY_TEXT：${extras.getString(Notification.EXTRA_SUMMARY_TEXT)}\n"+
//                                "EXTRA_TEMPLATE：${extras.getString(Notification.EXTRA_TEMPLATE)}\n"+
//                                "EXTRA_TEXT_LINES：${extras.getString(Notification.EXTRA_TEXT_LINES)}\n"+
//                                "EXTRA_BIG_TEXT：${extras.getString(Notification.EXTRA_BIG_TEXT)}\n"
//                        )
                        if (notificationPkg.contains("line")) {
                            speakMsg.append("Line： ")
                        }
                        speakMsg.append("${notificationTitle}說")
                        speakMsg.append(notificationText)
                        Log.d(TAG, "文字轉語音：${speakMsg}")
                        //文字轉語音
                        tts?.setPitch(1F)// 語調(1 為正常；0.5 為低一倍；2 為高一倍)
                        tts?.setSpeechRate(1F)// 速度(1 為正常；0.5 為慢一倍；2 為快一倍)

                        Log.d(TAG, "needSpeak：$needSpeak")
                        if (needSpeak){
                            val speechStatus =
                                tts?.speak(speakMsg.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
                            if (speechStatus == TextToSpeech.ERROR) {
                                Log.e(TAG, "TextToSpeech Error：")
                            }
                        }

                        FirebaseFirestore.getInstance().let {
                            val time = Calendar.getInstance().timeInMillis
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())
                            val date = dateFormat.format(Date(time))
                            val data = MessageData(
                                notificationTitle,
                                notificationText,
                                notificationPkg,
                                date
                            )

                            it.collection(Build.MODEL)
                                .document(date)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d(TAG, "updateDatabase：successfully\n" +
                                            "data：$data")
                                }
                                .addOnFailureListener { e: Exception ->
                                    Log.e(TAG, "updateDatabase：fail\n" +
                                            "$e")
                                }
                        }
                    }

                    if (mNotification.category == Notification.CATEGORY_MESSAGE) {
                        // TODO: 將內容加入 可視清單中
                    }
                }
            }
        }
    }

    //藍芽耳機是否連線
    private fun audioOutputAvailable(): Boolean {
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false
        }
        val isConnect = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        Log.d(TAG, "藍芽耳機是否連線：$isConnect")

        return isConnect
    }

    //藍芽裝置連線/斷線 回呼
    private fun audioOutputCallback() {
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesAdded(addedDevices)
                if (audioOutputAvailable()) {
                    Log.d(TAG, "藍芽耳機已連線")
                    needSpeak = true
                }
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesRemoved(removedDevices)
                if (!audioOutputAvailable()) {
                    Log.d(TAG, "藍芽耳機已斷線")
                    needSpeak = false
                    // TODO: is this correct
                    Toast.makeText(
                        applicationContext,
                        "藍芽耳機已斷線，請重新連線以繼續聆聽訊息",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, null)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationRemoved：${sbn}")
    }

    @SuppressLint("InvalidWakeLockTag", "WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate：")

        //文字轉語音
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ttsLang = tts!!.setLanguage(Locale.TRADITIONAL_CHINESE)
                if (ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The Language is not supported!")
                } else {
                    Log.d("TTS", "Language Supported.")
                }
                Log.d("TTS", "Initialization success.")
            } else {
                Toast.makeText(
                    applicationContext,
                    "TTS Initialization failed!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //不進入休眠
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyWakeLock"
        )
        wakeLock.acquire()

        //藍芽裝置連線/斷線 回呼
        audioOutputCallback()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy：")
    }

    override fun startService(service: Intent?): ComponentName? {
        Log.d(TAG, "startService：")
        return super.startService(service)
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d(TAG, "stopService：")
        return super.stopService(name)
    }
}
