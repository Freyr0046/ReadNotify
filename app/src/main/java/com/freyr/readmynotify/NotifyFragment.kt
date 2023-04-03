package com.freyr.readmynotify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.freyr.readmynotify.common.BaseFragment
import com.freyr.readmynotify.databinding.FragmentNotifyBinding


class NotifyFragment : BaseFragment<FragmentNotifyBinding>(FragmentNotifyBinding::inflate) {
    private var viewModel = NotifyViewModel()

    private var mNotifyAdapter: NotifyAdapter? = null

    var notifyService: NotificationService? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        initView()

        setObserver()

        initData()
    }

    private fun setupRecyclerView() {
        viewBinding?.rvNotifyList?.also {
            it.layoutManager = LinearLayoutManager(context)
            it.isNestedScrollingEnabled = false
            mNotifyAdapter = NotifyAdapter().also { adapter ->
                it.adapter = adapter
            }
        }
    }

    private fun initView() {
        viewBinding?.apply {
            // TODO: Service 斷線時 轉圈隱藏 & 顯示 重開服務
//            pgNotifyLoading
//            tvNotifyReboot
            tvNotifyOpen.setOnClickListener {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
            }
        }
    }

    private fun setObserver() {
        viewModel.run {
//            bannerData.observe(viewLifecycleOwner) {
//                Log.d(TAG, "bannerData：$it")
//            }
        }
    }

    private fun initData() {
        Log.d(TAG, "通知權限：${checkNotifyPermission(requireContext())}")
        Log.d(TAG, "requireActivity()：${requireActivity()}")
        if (checkNotifyPermission(requireContext())) {
            val serviceIntent = Intent(requireActivity(), NotificationService::class.java)
            requireActivity().startService(serviceIntent)
        } else {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    private fun startNotifyService() {
        if (notifyService != null) {

            val serviceIntent = Intent(requireActivity(), NotificationService::class.java)
            requireActivity().startService(serviceIntent)
        }

    }

    private fun stopNotifyService() {
    }

    private fun checkNotifyPermission(c: Context): Boolean {
        val pkgName = c.packageName
        val flat: String = Settings.Secure.getString(
            c.contentResolver,
            "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mNotifyAdapter = null
        val serviceIntent = Intent(requireActivity(), NotificationService::class.java)
        requireActivity().stopService(serviceIntent)
    }
}