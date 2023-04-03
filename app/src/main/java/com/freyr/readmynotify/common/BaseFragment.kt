package com.freyr.readmynotify.common

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.viewbinding.ViewBinding
import com.google.firebase.firestore.FirebaseFirestore

open class BaseFragment<VB : ViewBinding>(private val inflate: Inflate<VB>) : Fragment() {
    val TAG = this.javaClass.simpleName

    private var _viewBinding: VB? = null
    protected val viewBinding: VB?
        get() = _viewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinding = inflate.invoke(inflater, container, false)

        Log.d(TAG, "onCreateView")

        return _viewBinding?.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    //change fragment
    fun redirect(actionId: Int, bundle: Bundle? = null, navOptions: NavOptions? = null) {
        SafeNavigation.findNavController(view)?.run {
            navigate(actionId, bundle, navOptions)
        } ?: kotlin.run {
            val actionName = try {
                resources.getResourceEntryName(
                    actionId
                )
            } catch (e: Resources.NotFoundException) {
                e.toString()
            }

            Log.d(
                TAG, "view isn't initialized for the navigation action : ${actionName}."
            )
        }
    }

    private fun findHostActivity() = (activity as? BaseActivity)

    fun getFirebaseDB(): FirebaseFirestore? {
        return findHostActivity()?.db
    }
}

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T
