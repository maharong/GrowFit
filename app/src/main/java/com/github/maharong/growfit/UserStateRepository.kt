package com.github.maharong.growfit

class UserStateRepository(private val dao: UserStateDao) {
    // DB에서 상태를 불러오거나 없으면 생성
    suspend fun load(): UserStateEntity {
        return dao.getState() ?: UserStateEntity().also {
            dao.insertOrUpdate(it)
        }
    }

    // 상태 저장
    suspend fun save(state: UserStateEntity) {
        dao.insertOrUpdate(state)
    }
}