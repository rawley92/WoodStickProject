-- title.lua (레벨 초기화 스크립트)

-- 1. 월드 기하구조 로드
-- 이전: engine.initScene("Title", "Data/Level/title/map.dat")
-- 변경: map.dat 파일도 Data.json에 스캔되어 "map" 또는 "title_map"이라는 ID를 가짐
engine.initScene("Level.title.map")

-- 2. 맵 데이터(int)와 텍스처(String ID) 바인딩
-- Java의 TextureCore가 문자열 ID에 해당하는 픽셀 배열을 찾아 맵 코드에 연결함
engine.assignWallTexture("Textures.Level.Wall_1")

-- [추가] 3. 바닥과 천장 텍스처 글로벌 설정 (다음 단계를 위한 준비)
-- 맵의 빈 공간(0)에 그려질 바닥과 천장을 ID로 지정
engine.setFloorTexture("Textures.Level.Floor_1")
engine.setCeilingTexture("Textures.Level.Celling_1")

-- 5. 플레이어 초기 스폰 위치 및 카메라 뷰포트 세팅
engine.setupPlayer(3.5, 3.5, -1.0, 0.0, 0.0, 0.88)

engine.playBgm("Audio.Gt3LV", true)

-- 6. 월드 내 오브젝트/NPC 스폰 
-- (타입, 에셋ID, X, Y, AI스크립트ID)
-- 엔진에 등록된 spawnEntity 함수를 호출하여 NPC 생성
-- 이름, 에셋ID, 초기위치(x,y), 스크립트 파일명을 전달합니다.
-- 6. 월드 내 오브젝트 스폰
-- NPC 스폰: "Script.NPC1" 스크립트를 사용
engine.spawnEntity("NPC_Bob", "Char.SomeNPC.Base", 5.0, 5.0, "Script.NPC1")

-- UI 애니메이션 테스트 스폰: "Script.ui_anim_test" 스크립트를 사용
-- (이름은 NPC와 겹치지 않게 고유하게 지정하세요)
engine.spawnEntity("UI_Overlay", "Textures.UI.hud1", 0.0, 0.0, "Script.ui_anim_test")