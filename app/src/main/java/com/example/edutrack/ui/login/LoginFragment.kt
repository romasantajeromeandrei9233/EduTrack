package com.example.edutrack.ui.login


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.edutrack.R
import com.example.edutrack.databinding.FragmentLoginBinding
import com.example.edutrack.repository.AuthRepository
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText


class LoginFragment : Fragment() {


    private val viewModel: LoginViewModel by viewModels()


    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar // Added declaration for ProgressBar
    private lateinit var btnNavSignUp: TextView // CORRECTED TYPE: Must be TextView to match R.id.tvSignUp


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        progressBar = view.findViewById(R.id.progressBar) // Added initialization
        btnNavSignUp = view.findViewById(R.id.tvSignUp) // Assigned to TextView


        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false

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
                    progressBar.visibility = View.GONE // Show progress bar during loading
                    btnLogin.isEnabled = true
                }
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE // Hide progress bar after complete
                    btnLogin.isEnabled = false
                }
                is AuthState.Success -> {
                    progressBar.visibility = View.GONE
                    navigateBasedOnRole(state.role)
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }

        binding.tilEmail.error = null
        binding.tilPassword.error = null
        return true
    }

    private fun login(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.signIn(email, password)

            result.fold(
                onSuccess = { user ->
                    // Get user role
                    val roleResult = authRepository.getUserRole(user.uid)

                    roleResult.fold(
                        onSuccess = { role ->
                            binding.progressBar.visibility = View.GONE

                            // Navigate based on role
                            val intent = when (role.name) {
                                "TEACHER" -> Intent(requireContext(), TeacherDashboardActivity::class.java)
                                "PARENT" -> Intent(requireContext(), ParentDashboardActivity::class.java)
                                else -> null
                            }

                            intent?.let {
                                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(it)
                                requireActivity().finish()
                            }
                        },
                        onFailure = { exception ->
                            binding.progressBar.visibility = View.GONE
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                "Failed to get user role: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                },
                onFailure = { exception ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Login failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

    private fun navigateBasedOnRole(role: UserRole) {
        val intent = when (role) {
            UserRole.TEACHER -> Intent(requireContext(), TeacherDashboardActivity::class.java)
            UserRole.PARENT -> Intent(requireContext(), ParentDashboardActivity::class.java)
        }
    }

    private fun navigateToSignUp() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SignUpFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}