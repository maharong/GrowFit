package com.github.maharong.growfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 스킨 버튼 상태를 표현하는 enum.
 *
 * - BUY      : 아직 미보유 → 구매 가능
 * - SELECT   : 보유 중 → 선택 가능
 * - SELECTED : 현재 적용 중
 */
enum class SkinButtonState {
    BUY,
    SELECT,
    SELECTED
}

/**
 * 상점 리스트에 표시할 스킨 UI 모델.
 */
data class SkinUiModel(
    val id: Int,
    val name: String,
    val price: Int,
    val buttonState: SkinButtonState
)

/**
 * 상점 화면에서 발생하는 일회성 UI 이벤트.
 */
sealed class ShopUiEvent {
    data class ShowMessage(val message: String) : ShopUiEvent()
}

/**
 * 스킨 상점 비즈니스 로직을 담당하는 ViewModel.
 *
 * - 유저 포인트 / 보유 스킨 / 선택된 스킨을 조회한다.
 * - 스킨 구매 및 선택을 처리한다.
 */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userStateManager: UserStateManager
) : ViewModel() {

    private val _skins = MutableStateFlow<List<SkinUiModel>>(emptyList())
    /** 상점에 표시할 스킨 목록 */
    val skins: StateFlow<List<SkinUiModel>> get() = _skins

    private val _points = MutableStateFlow(0)
    /** 유저의 현재 포인트 */
    val points: StateFlow<Int> get() = _points

    private val _events = MutableSharedFlow<ShopUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    /** 토스트 등 일회성 UI 이벤트 스트림 */
    val events = _events
    init {
        refresh()
    }

    /**
     * 유저 상태와 보유 스킨 정보를 다시 읽어와
     * 스킨 리스트와 포인트 상태를 갱신한다.
     */
    fun refresh() {
        viewModelScope.launch {
            val state = userStateManager.getSkinShopState()
            _points.value = state.points

            _skins.value = SHOP_SKINS.map { skin ->
                val buttonState = when {
                    skin.id == state.currentSkinId -> SkinButtonState.SELECTED
                    state.ownedSkinIds.contains(skin.id) -> SkinButtonState.SELECT
                    else -> SkinButtonState.BUY
                }

                SkinUiModel(
                    id = skin.id,
                    name = skin.name,
                    price = skin.price,
                    buttonState = buttonState
                )
            }
        }
    }

    /**
     * 스킨 목록에서 버튼을 눌렀을 때의 동작을 처리한다.
     *
     * - BUY 상태: 포인트 차감 후 구매 시도
     * - SELECT 상태: 적용 스킨으로 설정
     * - SELECTED 상태: 아무 동작 없음
     */
    fun onSkinButtonClick(skinId: Int) {
        viewModelScope.launch {
            val item = _skins.value.firstOrNull { it.id == skinId } ?: return@launch

            when (item.buttonState) {
                SkinButtonState.BUY -> {
                    val success = userStateManager.buySkin(skinId, item.price)
                    if (success) {
                        _events.emit(ShopUiEvent.ShowMessage("${item.name} 스킨을 구매했어요!"))
                        refresh()
                    } else {
                        val alreadyOwned = userStateManager.isSkinOwned(skinId) // 아래 함수 추가
                        if (alreadyOwned) {
                            _events.emit(ShopUiEvent.ShowMessage("이미 보유한 스킨이에요."))
                            refresh()
                        } else {
                            _events.emit(ShopUiEvent.ShowMessage("포인트가 부족해요."))
                        }
                    }
                }
                SkinButtonState.SELECT -> {
                    val success = userStateManager.selectSkin(skinId)
                    if (success) refresh()
                }
                SkinButtonState.SELECTED -> {
                    // 이미 적용 중인 스킨 → 아무 동작도 하지 않는다.
                }
            }
        }
    }
}