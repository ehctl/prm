package com.linhnvt.project_prm.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<out T: ViewBinding> : AppCompatActivity(){
    private var _binding: T? = null
    protected val binding: T
        get() = _binding ?: throw IllegalStateException(
            "binding is only valid between onCreateView and onDestroyView"
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = createBinding(layoutInflater)
        setUpActivity(savedInstanceState)
        setContentView(binding.root)
        initAction()
    }

    abstract fun createBinding(
        inflater: LayoutInflater
    ): T

    override fun onDestroy() {
        onActivityDestroy()
        super.onDestroy()
    }

    open fun setUpActivity(savedInstanceState: Bundle?) = Unit
    open fun onActivityDestroy() = Unit
    open fun initAction() = Unit
}
