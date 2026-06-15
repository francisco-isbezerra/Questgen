package com.example.questgen.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.questgen.R
import com.example.questgen.databinding.FragmentEditProfileBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.MainViewModel
import com.example.questgen.viewmodel.ProfileUpdateState
import com.example.questgen.viewmodel.ProfileViewModel

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private val profileViewModel: ProfileViewModel by lazy {
        ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // Guard: se o Fragment foi destruído antes do resultado chegar, ignorar silenciosamente
        if (_binding == null) {
            android.util.Log.w("EditProfileFragment", "Picker result arrived after Fragment view was destroyed — ignoring.")
            return@registerForActivityResult
        }

        if (uri != null) {
            selectedImageUri = uri
            try {
                // Persistir permissão de leitura da URI para sobreviver a restarts
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Nem todas as URIs suportam permissão persistente — continuar normalmente
                android.util.Log.w("EditProfileFragment", "takePersistableUriPermission falhou (não crítico): ${e.message}")
            }
            binding.imgEditProfileAvatar.imageTintList = null
            binding.imgEditProfileAvatar.colorFilter = null
            binding.imgEditProfileAvatar.setPadding(0, 0, 0, 0)
            binding.imgEditProfileAvatar.load(uri) {
                crossfade(true)
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        } else {
            android.util.Log.d("EditProfileFragment", "PhotoPicker: nenhuma imagem selecionada pelo usuário.")
            // Não chamar toast() aqui — o Fragment pode não estar completamente retomado
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val density = resources.displayMetrics.density
        val currentUser = mainViewModel.currentUser.value

        currentUser?.let { user ->
            binding.etEditProfileName.setText(user.name)
            binding.etEditProfileDesc.setText(user.description ?: "")

            // Preservar a identidade visual aplicando a moldura neon
            if (!user.moldura_neon.isNullOrEmpty()) {
                val borderColor = when (user.moldura_neon) {
                    "neon_ciano" -> "#00BFFF"
                    "neon_roxo" -> "#8A2BE2"
                    "ouro_lendario" -> "#FFD700"
                    else -> null
                }
                if (borderColor != null) {
                    binding.layoutEditAvatarContainer.setCardBackgroundColor(android.graphics.Color.parseColor(borderColor))
                    val borderPadding = (4 * density).toInt()
                    binding.imgEditProfileAvatar.setPadding(borderPadding, borderPadding, borderPadding, borderPadding)
                }
            }

            // Exibir a foto atual
            val absoluteAvatarUrl = user.getAbsoluteImageUrl()
            if (!absoluteAvatarUrl.isNullOrEmpty()) {
                binding.imgEditProfileAvatar.imageTintList = null
                binding.imgEditProfileAvatar.colorFilter = null
                if (user.moldura_neon.isNullOrEmpty()) {
                    binding.imgEditProfileAvatar.setPadding(0, 0, 0, 0)
                }
                binding.imgEditProfileAvatar.load(absoluteAvatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                    memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    diskCachePolicy(coil.request.CachePolicy.DISABLED)
                }
            } else {
                val paddingPx = if (!user.moldura_neon.isNullOrEmpty()) (10 * density).toInt() else (8 * density).toInt()
                binding.imgEditProfileAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.imgEditProfileAvatar.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.azul_neon, null))
                binding.imgEditProfileAvatar.setImageResource(R.drawable.ic_profile)
            }
        }

        // Configuração dos cliques para alterar foto
        binding.layoutEditAvatarContainer.setOnClickListener { openGallery() }
        binding.btnChangePhoto.setOnClickListener { openGallery() }

        // Voltar
        binding.btnBackEditProfile.setOnClickListener {
            findNavController().popBackStack()
        }

        // Salvar alterações
        binding.btnSaveProfile.setOnClickListener {
            val user = mainViewModel.currentUser.value
            if (user == null) {
                toast("Usuário não logado.")
                return@setOnClickListener
            }

            val newName = binding.etEditProfileName.text.toString().trim()
            if (newName.isEmpty()) {
                toast("O nome não pode ser vazio")
                return@setOnClickListener
            }

            val newDesc = binding.etEditProfileDesc.text.toString().trim()

            profileViewModel.salvarAlteracoes(user.id, newName, selectedImageUri, user.image_url, if (newDesc.isEmpty()) null else newDesc)
        }

        // Observar o estado do upload/atualização
        collectLatestFlow(profileViewModel.updateState) { state ->
            when (state) {
                is ProfileUpdateState.Idle -> {
                    binding.progressEditProfileLoad.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                }
                is ProfileUpdateState.Loading -> {
                    binding.progressEditProfileLoad.visibility = View.VISIBLE
                    binding.btnSaveProfile.isEnabled = false
                }
                is ProfileUpdateState.Success -> {
                    binding.progressEditProfileLoad.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                    toast(state.message)
                    
                    // Atualiza o estado global na memória local imediatamente
                    val userLogado = mainViewModel.currentUser.value
                    if (userLogado != null) {
                        val usuarioAtualizado = userLogado.copy(
                            name = binding.etEditProfileName.text.toString().trim(),
                            image_url = state.user.image_url,
                            description = binding.etEditProfileDesc.text.toString().trim()
                        )
                        mainViewModel.updateUser(usuarioAtualizado)
                    } else {
                        mainViewModel.updateUser(state.user)
                    }
                    
                    // Retorna à tela anterior
                    findNavController().popBackStack()
                }
                is ProfileUpdateState.Error -> {
                    binding.progressEditProfileLoad.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                    toast(state.message)
                    profileViewModel.resetState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
