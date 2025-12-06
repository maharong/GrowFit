package com.github.maharong.growfit

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 스킨 상점 화면 프래그먼트.
 *
 * - 보유 포인트를 표시하고
 * - 스킨 목록을 RecyclerView로 보여주며
 * - ViewModel의 상태 및 이벤트를 구독한다.
 */
@AndroidEntryPoint
class ShopFragment : Fragment(R.layout.fragment_shop) {

    private val viewModel: ShopViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtPoints = view.findViewById<TextView>(R.id.txtPointsShop)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSkins)

        val adapter = ShopAdapter { skinId ->
            viewModel.onSkinButtonClick(skinId)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // 닫기 버튼 → 네비게이션 스택 뒤로
        btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // StateFlow / SharedFlow 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 포인트 표시
                launch {
                    viewModel.points.collect { points ->
                        txtPoints.text = "$points P"
                    }
                }
                // 스킨 목록 표시
                launch {
                    viewModel.skins.collect { list ->
                        adapter.submitList(list)
                    }
                }
                // 토스트 메시지 이벤트 처리
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ShopUiEvent.ShowMessage -> {
                                Toast.makeText(
                                    requireContext(),
                                    event.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}