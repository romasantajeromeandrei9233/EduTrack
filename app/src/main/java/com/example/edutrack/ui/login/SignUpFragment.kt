package com.example.edutrack.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.edutrack.R
import com.example.edutrack.model.UserRole
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rbTeacher: RadioButton
    private lateinit var rbParent: RadioButton
    private lateinit var btnSignUp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        rbTeacher = view.findViewById(R.id.rbTeacher)
        rbParent = view.findViewById(R.id.rbParent)
        btnSignUp = view.findViewById(R.id.btnSignUp)
        progressBar = view.findViewById(R.id.progressBar)
        tvLogin = view.findViewById(R.id.tvLogin)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = if (rbTeacher.isChecked) UserRole.TEACHER else UserRole.PARENT

            viewModel.signUp(email, password, name, role)
        }

        tvLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                }
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSignUp.isEnabled = false
                }
                is AuthState.Success -> {
                    progressBar.visibility = View.GONE
                    navigateBasedOnRole(state.role)
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
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