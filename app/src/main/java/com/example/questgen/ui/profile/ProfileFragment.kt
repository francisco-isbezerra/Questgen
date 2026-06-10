package com.example.questgen.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.questgen.R
import com.example.questgen.databinding.FragmentProfileBinding
import com.example.questgen.viewmodel.DeleteAccountState
import com.example.questgen.viewmodel.EditProfileState
import com.example.questgen.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
        adapter = HistoryAdapter(emptyList())
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
        }

        // Setup delete account button
        binding.btnDeleteAccount.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("EXCLUIR CONTA")
                .setMessage("ATENÇÃO: Esta ação é permanente e apagará todo o seu progresso de moedas e patentes na arena. Deseja continuar?")
                .setPositiveButton("EXCLUIR") { _, _ ->
                    mainViewModel.excluirConta()
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }

        // Setup edit profile floating button
        binding.btnEditProfile.setOnClickListener {
            val context = requireContext()
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }
            
            val currentUser = mainViewModel.currentUser.value
            
            val etName = EditText(context).apply {
                hint = "Nome de Usuário"
                setText(currentUser?.name ?: "")
                maxLines = 1
            }
            
            val etImage = EditText(context).apply {
                hint = "URL da Foto de Perfil"
                setText(currentUser?.image_url ?: "")
                maxLines = 1
            }
            
            layout.addView(etName)
            layout.addView(etImage)
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("EDITAR PERFIL")
                .setView(layout)
                .setPositiveButton("SALVAR") { _, _ ->
                    val newName = etName.text.toString().trim()
                    val newImage = etImage.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        mainViewModel.editarPerfil(newName, if (newImage.isEmpty()) null else newImage)
                    } else {
                        Toast.makeText(context, "O nome não pode ser vazio", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("REMOVER FOTO") { _, _ ->
                    val newName = etName.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        mainViewModel.editarPerfil(newName, null)
                    } else {
                        Toast.makeText(context, "O nome não pode ser vazio", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }

        // Observe Shared user profile
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.currentUser.collectLatest { user ->
                user?.let {
                    binding.tvProfileName.text = it.name
                    binding.tvProfileRank.text = "${it.rank} - Top 3.2% Global"
                    
                    // Dynamic image loading via Coil
                    if (!it.image_url.isNullOrEmpty()) {
                        binding.imgProfileAvatar.imageTintList = null
                        binding.imgProfileAvatar.colorFilter = null
                        binding.imgProfileAvatar.setPadding(0, 0, 0, 0)
                        binding.imgProfileAvatar.load(it.image_url) {
                            crossfade(true)
                            placeholder(R.drawable.ic_profile)
                            error(R.drawable.ic_profile)
                        }
                    } else {
                        val paddingPx = (8 * resources.displayMetrics.density).toInt()
                        binding.imgProfileAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        binding.imgProfileAvatar.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.azul_neon, null))
                        binding.imgProfileAvatar.setImageResource(R.drawable.ic_profile)
                    }
                }
            }
        }

        // Observe Profile Edit State
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.editProfileState.collectLatest { state ->
                when (state) {
                    is EditProfileState.Loading -> {
                        // Optional progress indicators
                    }
                    is EditProfileState.Success -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        mainViewModel.resetEditProfileState()
                    }
                    is EditProfileState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        mainViewModel.resetEditProfileState()
                    }
                    else -> {}
                }
            }
        }

        // Observe Delete Account State
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.deleteAccountState.collectLatest { state ->
                when (state) {
                    is DeleteAccountState.Loading -> {
                        // Optional progress indicators
                    }
                    is DeleteAccountState.Success -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        mainViewModel.resetDeleteAccountState()
                    }
                    is DeleteAccountState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        mainViewModel.resetDeleteAccountState()
                    }
                    else -> {}
                }
            }
        }

        // Observe local challenge history log
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.challengeHistory.collectLatest { historyList ->
                adapter.updateData(historyList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
