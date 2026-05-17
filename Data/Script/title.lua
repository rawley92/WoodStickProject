-- level1.lua

-- 1. 월드 기하구조(벽 배치 배열) 로드
engine.initScene("Title", "Data/Level/title/map.dat")

-- 2. 맵 데이터(map.dat) 내부의 숫자 코드에 텍스처 매핑 명시 (이름 기반 할당)
-- map.dat 내부에서 '1'번으로 써진 벽은 'brick_red' 프리로드 이미지를 씁니다.
engine.assignWallTexture(1, "brick_red")
engine.assignWallTexture(2, "iron_gate")
engine.assignWallTexture(3, "wood_panel")

-- 3. 중앙에 등록되어 있는 프리로드 캐릭터 에셋 이름으로 엔티티 스폰 및 인공지능 명칭 지정
engine.spawnEntity("NPC", "npc_guard", 4.5, 6.2, "guard_patrol")
engine.spawnEntity("NPC", "npc_slime", 10.0, 3.5, "slime_idle")

-- 4. 런타임 프레임 갱신 훅
function onEntityUpdate(entity, dt, player)
    -- AI 및 트리거 조건 로직 기술...
    if entity.scriptName == "guard_patrol" then
        -- 경비병 AI 동작 코드
    end

    -- 레벨 전환 조건 판정
    if player.x > 14.0 then
        engine.log("Level 1 클리어. 다음 레벨로 전환합니다.")
        dofile("Data/Script/level2.lua") -- 레벨 2 스크립트로 완전 스위칭
    end
end

engine.log("Level 1 스크립트 빌드 성공.")