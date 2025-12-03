package com.example.edutrack.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.edutrack.R
import com.example.edutrack.model.UserRole
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnNavSignUp: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnNavSignUp = view.findViewById(R.id.tvSignUp)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.signIn(email, password)
        }

        btnNavSignUp.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SignUpFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> {
                    btnLogin.isEnabled = true
                }
                is AuthState.Loading -> {
                    btnLogin.isEnabled = false
                }
                is AuthState.Success -> {
                    navigateBasedOnRole(state.role)
                }
                is AuthState.Error -> {
                    btnLogin.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateBasedOnRole(role: UserRole) {
        val intent = when (role) {
            UserRole.TEACHER -> Intent(requireContext(), TeacherDashboardActivity::class.java)
            UserRole.PARENT -> Intent(requireContext(), ParentDashboardActivity::class.java)
        }
        startActivity(intent)
        requireActivity().finish()
    }
}