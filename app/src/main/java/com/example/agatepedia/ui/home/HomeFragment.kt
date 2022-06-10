package com.example.agatepedia.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.agatepedia.R
import com.example.agatepedia.data.Result
import com.example.agatepedia.databinding.FragmentHomeBinding
import com.example.agatepedia.ui.ViewModelFactory
import com.example.agatepedia.ui.adapter.AgateAdapter


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val agateAdapter = AgateAdapter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory: ViewModelFactory = ViewModelFactory.getInstance(requireContext())
        val viewModel: HomeViewModel by viewModels {
            factory
        }

        getDataAgate(viewModel)
        setRecycler()
        setupSearchView(viewModel)
        showSearchNotFound(viewModel)

        binding.refreshLayout.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                viewModel.isSearch = false
                getDataAgate(viewModel)
                binding.refreshLayout.isRefreshing = false
            }

        })

    }

    private fun setRecycler() {
        with(binding) {
            rvAgete.layoutManager = LinearLayoutManager(requireContext())
            rvAgete.setHasFixedSize(true)
            agateAdapter.onItemClick = { agate, view ->
                val toDetailAgateActivity =
                    HomeFragmentDirections.actionNavigationHomeToDetailAgatepediaActivity(
                        null,
                        agate.jenis
                    )
                toDetailAgateActivity.isHome = true

                view.findNavController().navigate(toDetailAgateActivity)
            }

            rvAgete.adapter = agateAdapter
        }
    }


    private fun getDataAgate(viewModel: HomeViewModel) {
        viewModel.getAgateData().observe(viewLifecycleOwner) { result ->
            if (result != null) {
                when (result) {
                    is Result.Loading -> {
                        binding.proggressBar.visibility = View.VISIBLE
                    }
                    is Result.Success -> {
                        binding.proggressBar.visibility = View.GONE
                        val agateData = result.data
                        if (!viewModel.isSearch) {
                            viewModel.searchNotFound.postValue(false)
                            agateAdapter.submitList(agateData)
                        }
                    }
                    is Result.Error -> {
                        binding.proggressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), getString(R.string.error_request), Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }
    }

    private fun setupSearchView(viewModel: HomeViewModel) {

        if (viewModel.searchViewData != null && viewModel.isSearch) {
            agateAdapter.submitList(viewModel.searchViewData)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.searchAgateData(query).observe(viewLifecycleOwner) { result ->
                        if (result != null) {
                            when (result) {
                                is Result.Loading -> {
                                    binding.proggressBar.visibility = View.VISIBLE
                                }
                                is Result.Success -> {
                                    binding.proggressBar.visibility = View.GONE
                                    val agateData = result.data


                                    viewModel.isSearch = true
                                    viewModel.searchViewData = agateData
                                    agateAdapter.submitList(agateData)

                                    if (agateData.size == 0) viewModel.searchNotFound.postValue(true) else
                                        viewModel.searchNotFound.postValue(false)
                                }
                                is Result.Error -> {
                                    binding.proggressBar.visibility = View.GONE

                                    Toast.makeText(requireContext(), getString(R.string.error_request), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })
    }

    private fun showSearchNotFound(viewModel: HomeViewModel) {

        viewModel.searchNotFound.observe(viewLifecycleOwner, {
            if (it) {
                binding.notFound.visibility =
                    View.VISIBLE
            } else {
                binding.notFound.visibility = View.GONE
            }
        })
    }

}