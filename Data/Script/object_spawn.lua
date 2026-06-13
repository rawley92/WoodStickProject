-- Runtime object spawn adapter.
-- ObjectSpawner의 typeName 레코드를 실제 engine.spawnEntity 호출에 필요한 Script ID로 변환한다.

local ObjectSpawn = {}

-- 생성 데이터의 타입명과 Lua 동작 스크립트 ID 사이의 매핑이다.
local SCRIPT_BY_TYPE = {
    enemy_spider = "Script.monster_spider",
    enemy_bull = "Script.monster_bull",
    enemy_ghost = "Script.monster_ghost",
    item_health = "Script.item_heal",
    item_ammo = "Script.item_ammo",
    weapon_melee = "Script.weapon_melee",
    weapon_gun = "Script.weapon_gun"
}

-- 공격 대상 추적 테이블에 넣을 타입만 별도로 표시한다.
local MONSTER_TYPES = {
    enemy_spider = true,
    enemy_bull = true,
    enemy_ghost = true
}

-- 타입명을 실제 Java Entity와 Lua scriptPath로 변환해 현재 씬에 스폰한다.
function ObjectSpawn.spawn(typeName, x, y)
    local scriptId = SCRIPT_BY_TYPE[typeName]
    if scriptId == nil then
        print("[OBJECT SPAWN] Unknown object type: " .. tostring(typeName))
        return nil
    end

    -- 초기 assetId는 비워 두고, 각 아이템/몬스터 스크립트가 ensure 단계에서 외형을 확정한다.
    return engine.spawnEntity(typeName, "", x, y, scriptId, 1.0)
end

-- maze.lua가 공격 대상 후보를 별도 추적할지 판단할 때 사용한다.
function ObjectSpawn.isMonster(typeName)
    return MONSTER_TYPES[typeName] == true
end

return ObjectSpawn
