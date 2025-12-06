package com.github.maharong.growfit

/**
 * 스킨 보유 정보에 대한 Repository.
 *
 * - DAO에서 받은 데이터를 Set<Int> 형태로 가공하여 편하게 사용하도록 제공한다.
 * - 비즈니스 로직(UserStateManager 등)에서 호출한다.
 */
class OwnedSkinRepository(private val dao: OwnedSkinDao) {

    /** 모든 보유 스킨 ID를 Set으로 반환 */
    suspend fun getOwnedIds(): Set<Int> =
        dao.getAllOwnedSkinIds().toSet()

    /** 특정 스킨 보유 여부 */
    suspend fun isOwned(skinId: Int): Boolean =
        dao.isOwned(skinId)

    /** 스킨을 보유 목록에 추가 */
    suspend fun addOwned(skinId: Int) {
        dao.insert(OwnedSkinEntity(skinId = skinId))
    }
}