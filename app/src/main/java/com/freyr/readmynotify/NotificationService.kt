package com.freyr.readmynotify

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private var needSpeak = true

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 包名
        val notificationPkg = sbn.packageName
//        Log.d(TAG, "onNotificationPosted：包名：$notificationPkg")
        sbn.notification?.let { mNotify ->
            val extras: Bundle = mNotify.extras
            // 標題
            val notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE)
            // 內容
            var notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
            // EXTRA_SUB_TEXT (Line：群組)
            val groupName = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

            //2023.04.17只唸出 Line、Facebook、Instagram 訊息
            if (notificationPkg.contains("line") ||
                notificationPkg.contains("facebook") ||
                notificationPkg.contains("instagram")){

                //Line訊息 重複問題
                if (sbn.tag != null) {
                    //最後唸出的文字
                    val notifyMsg = StringBuilder("")
                    when {
                        notificationPkg.contains("line") -> {
                            notifyMsg.append("Line： ")
                            if (groupName != null) {
                                notifyMsg.append("群組： $groupName： ")
                            }
                        }
                        notificationPkg.contains("facebook") -> {
                            notifyMsg.append("facebook： ")
                        }
                        notificationPkg.contains("instagram") -> {
                            notifyMsg.append("instagram： ")

                            notificationTitle?.let { title ->
                                if (notificationText != null) {
                                    if (notificationText!!.contains(title)){
                                        notificationText =
                                            notificationText!!.substring(title.length)
                                    }
                                }
                            }
                        }
                    }
                    notifyMsg.append("${notificationTitle}說")
                    notifyMsg.append(notificationText)
                    Log.d(TAG, "文字轉語音：${notifyMsg}")

                    //文字轉語音
                    //不唸出 e-Mail
                    if (mNotify.category != Notification.CATEGORY_EMAIL) {
                        Log.d(TAG, "needSpeak：$needSpeak")
                        if (needSpeak) {
                            val speechStatus =
                                tts?.speak(
                                    notifyMsg.toString(),
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    packageName
                                )
                            if (speechStatus == TextToSpeech.ERROR) {
                                Log.e(TAG, "TextToSpeech Error：")
                            }
                        }
                    } else {
                        //mail 內容改取 BIG_TEXT(主旨+內容)，EXTRA_TEXT 為 主旨。
                        notificationText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    }
                }
            }

            //2023.03.28 暫時不接收"sys"與"service"的推播
            when (mNotify.category) {
                Notification.CATEGORY_SYSTEM, Notification.CATEGORY_SERVICE, null -> {
                    //do nothing
                }
                else -> {
                    //2023.04.17 暫時不接收"tag"為空的推播
                    //解決 Line訊息 重複問題
                    if (sbn.tag != null) {
                        val notifyMsg = StringBuilder("")
                        Log.d(TAG, "onNotificationPosted：包名：$notificationPkg")
//                        Log.d(
//                            TAG, "推播：包名：$notificationPkg\n" +
//                                    "title：${notificationTitle}\n" +
//                                    "content：${notificationText}\n" +
//                                    "category：${mNotify.category}\n" +
//                                    "number：${mNotify.number}\n" +
//                                    "publicVersion：${mNotify.publicVersion}\n" +
//                                    "tickerText：${mNotify.tickerText}\n" +
//                                    "EXTRA_SUB_TEXT：${groupName}\n" +
//                                    "EXTRA_SUMMARY_TEXT：${extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)}\n" +
//                                    "EXTRA_TEMPLATE：${extras.getCharSequence(Notification.EXTRA_TEMPLATE)}\n" +
//                                    "EXTRA_TEXT_LINES：${extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)}\n"+//多則訊息堆疊(需逐向印出)
//                                    "EXTRA_BIG_TEXT：${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}\n" +
//                        )
                    }
                }
            }

            //暫存
            if (notificationPkg.contains("line") && sbn.tag == null){
                //解決 Line訊息 重複問題
                //do nothing
            }else{
                FirebaseFirestore.getInstance().let {
                    val time = Calendar.getInstance().timeInMillis
                    val dateFormat =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())
                    val date = dateFormat.format(Date(time))
                    val dateMonth = date.substring(0, 7)
                    val dateDay = date.substring(8, 10)
                    val chatroom = if (groupName != null) {
                        "群組：$groupName"
                    } else {
                        notificationTitle
                    }

                    val hashData = hashMapOf(
                        "title" to (notificationTitle?:"").toString(),
                        "content" to (notificationText?:"").toString(),
                        "其他-StatusBarNotification" to sbn.toString(),
                        "其他-Bundle" to extras.toString(),
                        "其他-Notification" to mNotify.toString(),
                        "其他-Date" to date,
                        "其他-SUB_TEXT" to groupName,
                        "其他-包名" to notificationPkg,
                        "其他-category" to mNotify.category,
                        "其他-tag" to sbn.tag,
                    )
//                    val hashData = hashMapOf(
//                        "title" to (notificationTitle?:"null").toString(),
//                        "content" to (notificationText?:"null").toString(),
//                        "其他-StatusBarNotification" to (sbn?:"null").toString(),
//                        "其他-Bundle" to (extras?:"null").toString(),
//                        "其他-Notification" to (mNotify?:"null").toString(),
//                        "其他-Date" to (date?:"null").toString(),
//                        "其他-SUB_TEXT" to (groupName?:"null").toString(),
//                        "其他-包名" to (notificationPkg?:"null").toString(),
//                        "其他-category" to (mNotify.category?:"null").toString(),
//                        "其他-tag" to (sbn.tag?:"null").toString(),
//                    )
                    //機型(後期可改為 唯一識別)
                    it.collection(Build.MODEL)
                        //年+月/日/包名/人/時間
                        .document("$dateMonth/$dateDay/$notificationPkg/$chatroom/$date")
                        .set(hashData, SetOptions.merge())
                        .addOnSuccessListener {
//                                    Log.d(
//                                        TAG, "updateDatabase：successfully\n" +
//                                                "data：$data"
//                                    )
                        }
                        .addOnFailureListener { e: Exception ->
                            Log.e(
                                TAG, "updateDatabase：fail\n" +
                                        "$e"
                            )
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
        val isConnect = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        Log.d(TAG, "藍芽耳機是否連線：$isConnect")
        needSpeak = isConnect

        return isConnect
    }

    //藍芽裝置連線/斷線 回呼
    private fun audioOutputCallback() {
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesAdded(addedDevices)

                audioOutputAvailable()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesRemoved(removedDevices)
                if (!audioOutputAvailable()) {
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

            //文字轉語音-設置音源焦點
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val audioFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttrs)
                .setFocusGain(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            Log.d(TAG, "AudioFocus：音頻焦點丟失，永久暫停播放")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.d(TAG, "AudioFocus：暫停播放")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            Log.d(TAG, "AudioFocus：降低音量，繼續播放")
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.d(TAG, "AudioFocus：再次獲取音頻焦點")
                        }
                    }
                }
                .build()
            tts?.setAudioAttributes(audioAttrs)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                val audioManager: AudioManager =
                    getSystemService(Context.AUDIO_SERVICE) as AudioManager

                override fun onDone(utteranceId: String?) {
                    val abandon = audioManager.abandonAudioFocusRequest(audioFocus)
                    Log.d(TAG, "AudioListener：abandon：$abandon。")
                }

                override fun onError(p0: String?) {
                    val abandon = audioManager.abandonAudioFocusRequest(audioFocus)
                    Log.d(TAG, "AudioListener：abandon：$abandon。")
                }

                override fun onStart(p0: String?) {
                    val audioResult = audioManager.requestAudioFocus(audioFocus)
                    Log.d(
                        TAG,
                        "AudioListener：audioResult：$audioResult。${audioResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}"
                    )
                }
            })
        }
        //設置音量與音調
        tts?.setPitch(1F)// 語調(1 為正常；0.5 為低一倍；2 為高一倍)
        tts?.setSpeechRate(1F)// 速度(1 為正常；0.5 為慢一倍；2 為快一倍)

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
