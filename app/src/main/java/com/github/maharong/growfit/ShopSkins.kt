package com.github.maharong.growfit

/**
 * 상점에 표시할 스킨 정보를 위한 데이터 모델.
 *
 * @param id 스킨 ID
 * @param name 스킨 이름
 * @param price 스킨 가격(포인트)
 */
data class ShopSkinItem(
    val id: Int,
    val name: String,
    val price: Int
)

/**
 * 상점에 진열되는 스킨 목록.
 *
 * - id 0: 기본 스킨 (항상 무료)
 * - id 1: 선인장 스킨
 */
val SHOP_SKINS = listOf(
    ShopSkinItem(id = 0, name = "기본", price = 0),
    ShopSkinItem(id = 1, name = "선인장", price = 100)
)
