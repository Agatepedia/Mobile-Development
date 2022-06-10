package com.example.agatepedia.ui.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agatepedia.R
import com.example.agatepedia.databinding.FragmentBookmarkBinding
import com.example.agatepedia.databinding.FragmentCameraBinding
import com.example.agatepedia.ui.ViewModelFactory
import com.example.agatepedia.ui.adapter.BookmarkAdapter
import com.example.agatepedia.ui.home.HomeFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookmarkFragment : Fragment() {

    private var _binding: FragmentBookmarkBinding? = null

    private val binding get() = _binding!!
    private val agateAdapter = BookmarkAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecycler()
    }

    override fun onResume() {
        super.onResume()

        val factory: ViewModelFactory = ViewModelFactory.getInstance(requireContext())
        val viewModel: BookmarkViewModel by viewModels {
            factory
        }
        getData(viewModel)

    }

    private fun setRecycler() {
        with(binding) {
            rvAgete.layoutManager = LinearLayoutManager(requireContext())
            rvAgete.setHasFixedSize(true)
            agateAdapter.onItemClick = { agate, view ->
                val toDetailAgateActivity =
                    BookmarkFragmentDirections.actionNavigationBookmarkToDetailAgatepediaActivity(
                        null,
                        agate.type
                    )
                toDetailAgateActivity.isHome = true

                view.findNavController().navigate(toDetailAgateActivity)
            }

            rvAgete.adapter = agateAdapter
        }
    }

    private fun getData(viewModel: BookmarkViewModel) {
        lifecycleScope.launch(Dispatchers.Default) {
            val dataAgate = viewModel.getDataAgate()
            withContext(Dispatchers.Main) {
                agateAdapter.submitList(dataAgate)
            }
        }

    }
}