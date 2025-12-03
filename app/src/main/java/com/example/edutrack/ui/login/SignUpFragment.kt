package com.example.edutrack.ui.login


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView // Import TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.edutrack.R
import com.example.edutrack.databinding.FragmentSignupBinding
import com.example.edutrack.model.UserRole
import com.example.edutrack.repository.AuthRepository
import com.example.edutrack.ui.parent.ParentDashboardActivity
import com.example.edutrack.ui.teacher.TeacherDashboardActivity
import kotlinx.coroutines.launch


class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

    private val viewModel: LoginViewModel by viewModels()


    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rbTeacher: RadioButton
    private lateinit var rbParent: RadioButton
    private lateinit var btnSignUp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNavLogin: TextView  // CORRECTED TYPE: Must be TextView to match R.id.btnNavLogin


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
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
        btnNavLogin = view.findViewById(R.id.btnNavLogin)  // Assigned to TextView


        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val role = if (binding.rbTeacher.isChecked) UserRole.TEACHER else UserRole.PARENT

            if (validateInput(name, email, password)) {
                signUp(name, email, password, role)
            }
        }

        binding.btnNavLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false

            viewModel.signUp(email, password, name, role)
        }


        btnNavLogin.setOnClickListener {
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

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        return true
    }

    private fun signUp(name: String, email: String, password: String, role: UserRole) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignUp.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.signUp(email, password, name, role)

            result.fold(
                onSuccess = { user ->
                    binding.progressBar.visibility = View.GONE

                    Toast.makeText(
                        requireContext(),
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate based on role
                    val intent = when (role) {
                        UserRole.TEACHER -> Intent(requireContext(), TeacherDashboardActivity::class.java)
                        UserRole.PARENT -> Intent(requireContext(), ParentDashboardActivity::class.java)
                    }

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                },
                onFailure = { exception ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Sign up failed: ${exception.message}",
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

    private fun navigateToLogin() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}