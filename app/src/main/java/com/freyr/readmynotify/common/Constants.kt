package com.freyr.readmynotify.common

object Constants {
    const val ALARM_ID = "alarm_id"
    const val OPEN_TYPE = "OPEN_TYPE"
    const val REFRESH = "REFRESH"
    const val FIRE_ALARM = "FIRE_ALARM"
    const val ALARM_DATA = "ALARM_DATA"
    const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

    // 開始斷食鬧鐘
    const val TIMER_ACTION_START_FASTING = "com.freyr.mysportrecorder.TIMER_ACTION_START_FASTING"

    // 結束斷食鬧鐘
    const val TIMER_ACTION_END_FASTING = "com.freyr.mysportrecorder.TIMER_ACTION_END_FASTING"
    const val REQUEST_END_FASTING = 200

    // 設定倒數器
    const val SET_COUNTDOWN = "com.freyr.mysportrecorder.SET_COUNTDOWN"

    const val WORK_TAG_ACTIVATE_ALARMS = "com.freyr.mysportrecorder.WORK_TAG_ACTIVATE_ALARMS"

    const val NOTIFY_FOREGROUND_SERVICE_ID = 1001

    // speaker暫停或繼續
    const val REQUEST_SPEAKER_PLAY = 100
    const val REQUEST_SPEAKER_STOP = 200
    const val REQUEST_SPEAKER_CLOSE = 300

    // foregroundService extra
    const val SERVICE_EXTRA = "arg"
    const val EXTRA_SPEAKER_PLAY = "EXTRA_SPEAKER_PLAY"
    const val EXTRA_SPEAKER_STOP = "EXTRA_SPEAKER_STOP"
    const val EXTRA_SPEAKER_CLOSE = "EXTRA_SPEAKER_CLOSE"
}
