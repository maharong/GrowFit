# GrowFit
수집 요소를 더한 운동 타이머 앱

GrowFit은 **프리셋 기반 운동 루틴 구성**, **걸음 수 감지**,  
**식물 성장 시스템**, **스킨 수집 요소**를 결합한 운동 타이머 앱입니다.  
사용자는 매일 운동을 완료할 때마다 경험치를 얻고,  
성장한 식물과 스킨을 수집하며 운동 동기부여를 얻을 수 있습니다.

---

## ✨ 주요 기능 (v1.0.0)

### 🟩 1. 프리셋 기반 운동 루틴
- 원하는 스텝을 조합하여 운동 프리셋을 구성
- TIME / COUNT / WALKING / RUNNING / REST 스텝 지원
- 스텝 이름, 시간, 횟수, 목표 걸음 수 설정 가능
- 드래그 앤 드롭으로 스텝 순서 변경 가능
- 프리셋 생성 / 수정 / 삭제 지원

### 🟦 2. Run 모드
- **TYPE_STEP_DETECTOR 센서 기반 걸음 수 측정**
- 스텝별 진행 상태 실시간 업데이트
- 목표 달성 시 자동 다음 스텝 이동
- 남은 시간·걸음 수 표시
- 운동 완료 후 보상 지급

### 🟨 3. 진동(Vibration) 피드백
- 전체 진동 활성화/비활성화 설정
- N초 전 미리 알림 진동
- 스텝 변경 진동
- 운동 완료 패턴 진동
- API 버전별 진동 모듈 최적화

### 🟧 4. 식물 성장 시스템 (Plant Skin)
- 운동 완료 시 경험치 획득 → 레벨업
- 레벨에 따라 식물 외형 변화 (1~5단계)
- 스킨 시스템을 통한 다양한 식물 외형 제공
- 기본 스킨 + 선인장 스킨 포함

### 🟥 5. 스킨 상점(Shop)
- 경험치와 별개로 지급되는 포인트로 스킨 구매
- 스킨 레벨별 애니메이션(1~5레벨 이미지 자동 순환)
- 구매 여부 저장 (Room DB)

### 🟪 6. UserState 관리
- 레벨, 경험치, 포인트, 스킨, 오늘 운동 여부 저장
- 하루 미운동 시 경험치 감소(레벨 다운 가능)
- 운동은 하루 1회만 보상 지급

---

## 🏗 기술 스택

- **Kotlin**
- **Android Jetpack**
    - ViewModel / LiveData / StateFlow
    - Room Database
    - Navigation Component
    - Hilt(Dagger) – DI
- **센서 API**
    - TYPE_STEP_DETECTOR 기반 걸음 감지
- **UI**
    - RecyclerView + ListAdapter
    - Fragment 기반 화면 구조
    - Material Components
- **DB**
    - Preset / PresetStep / UserState / OwnedSkin 엔티티 구성

---

## 📦 데이터 구조

### Preset & Steps

| Entity             | 설명                   |
|--------------------|----------------------|
| `PresetEntity`     | 프리셋 기본 정보            |
| `PresetStepEntity` | 스텝(unit) 정보          |
| `PresetWithSteps`  | Room @Relation 조합 모델 |

### UserState

| 필드    | 설명                  |
|-------|---------------------|
| 레벨    | 1~5                 |
| 경험치   | 성장 수치               |
| 포인트   | 상점 구매용 재화           |
| 선택 스킨 | PlantSkinMapper와 연결 |

### Skins

| Entity            | 설명           |
|-------------------|--------------|
| `OwnedSkinEntity` | 보유한 스킨 ID 저장 |

---

## 🎨 식물 스킨 매핑

스킨 ID + 레벨 → drawable 리소스 매핑
```kotlin
PlantSkinMapper.getPlantDrawable(skinId, level)
