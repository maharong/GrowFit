package com.github.maharong.growfit

/**
 * `UserStateEntity`를 읽고 저장하는 레포지토리.
 *
 * - 항상 단일 유저 상태(id = 0)만 관리한다.
 * - 상태가 없으면 기본값으로 새로 생성한다.
 */
class UserStateRepository(private val dao: UserStateDao) {

    /**
     * 유저 상태를 로드한다.
     *
     * 상태가 존재하지 않으면 기본값으로 새로 생성한 뒤 저장하고 반환한다.
     */
    suspend fun load(): UserStateEntity {
        return dao.getState() ?: UserStateEntity().also {
            dao.insertOrUpdate(it)
        }
    }

    /**
     * 변경된 유저 상태를 저장한다.
     */
    suspend fun save(state: UserStateEntity) {
        dao.insertOrUpdate(state)
    }
}