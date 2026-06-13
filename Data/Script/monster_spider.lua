-- Spider monster AI.
-- 짧은 시야 감지 후 플레이어를 추적하고 근접 공격하는 기본 추격형 적이다.

-- 몬스터별 HP/상태는 엔티티 객체가 아니라 전역 테이블에 entityId 키로 저장한다.
if not _G.monster_states then _G.monster_states = {} end
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local CombatEffects = dofile("Data/Script/combat_effects.lua")
local MonsterCommon = dofile("Data/Script/monster_common.lua")
local STATE_IDLE = 1
local STATE_CHASE = 2

local PLAYER_SPEED = 2.5
local MOVE_SPEED = PLAYER_SPEED * 0.7
local VISION_RANGE = 5.0
local VISION_FOV = 120.0
local PROXIMITY_RANGE = 2.0
local CHASE_RANGE = 7.0
local ATTACK_RANGE = 1.3
local ATTACK_DAMAGE = 15
local ATTACK_COOLDOWN = 1.0
local MAX_HP = 14
-- MonsterCommon.ensure가 이 설정으로 이름, 스케일, 방향별 스프라이트를 초기화한다.
local CONFIG = {
    kind = "spider",
    name = "Spider",
    scale = 0.65,
    textures = {
        front = "Char.Spider.state_front_1",
        back = "Char.Spider.state_back_1",
        left = "Char.Spider.state_left_1",
        right = "Char.Spider.state_right_1",
        hit = "Char.Spider.state_front_1_hit"
    }
}

-- entityId별 memory에 전투 수치를 최초 1회 기록한다.
local function initStats(entity, mem)
    if not mem.statsInitialized then
        mem.maxHp = MAX_HP
        mem.hp = MAX_HP
        mem.statsInitialized = true
    end
end

-- HP가 0이 된 몬스터를 Java 렌더/업데이트 대상에서 제외한다.
local function destroyIfDead(entity, mem)
    if mem.hp <= 0 then
        entity.isActive = false
        entity.isDestroyed = true
        entity.physics.velX = 0
        entity.physics.velY = 0
        return true
    end

    return false
end

-- Java ScriptManager가 매 프레임 호출하는 몬스터 엔티티 update다.
function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    
    local id = entity.entityId
    if not _G.monster_states[id] then
        -- 같은 Lua env를 여러 엔티티가 공유하므로 per-entity 상태는 entityId로 분리한다.
        _G.monster_states[id] = { state = STATE_IDLE, attackTimer = 0 }
    end
    local mem = _G.monster_states[id]
    MonsterCommon.ensure(entity, mem, CONFIG)
    initStats(entity, mem)
    CombatEffects.tickEnemy(entity, mem, deltaTime)
    if destroyIfDead(entity, mem) then return end

    mem.attackTimer = math.max(0, (mem.attackTimer or 0) - deltaTime)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= ATTACK_RANGE and mem.attackTimer <= 0 then
        -- 공격 성공 여부는 PlayerDamage가 무적 시간까지 고려해 반환한다.
        if PlayerDamage.tryHit(player, entity, ATTACK_DAMAGE) then
            mem.attackTimer = ATTACK_COOLDOWN
        end
    end

    if distance <= 0.01 then return end

    if mem.state == STATE_IDLE then
        entity.physics.velX = 0
        entity.physics.velY = 0

        -- 시야/거리/벽 차단 판정이 통과하면 추격 상태로 전환한다.
        if MonsterCommon.canDetectPlayer(entity, player, mem, distance, VISION_RANGE, VISION_FOV, PROXIMITY_RANGE) then
            mem.state = STATE_CHASE
        end
    elseif mem.state == STATE_CHASE then
        if distance > CHASE_RANGE then
            mem.state = STATE_IDLE
            entity.physics.velX = 0
            entity.physics.velY = 0
            MonsterCommon.updateSprite(entity, mem, player)
            return
        end

        MonsterCommon.face(mem, dx, dy, distance)
        -- Java PhysicsCore는 velX/velY를 프레임 이동량처럼 적용하므로 deltaTime을 곱해 넣는다.
        entity.physics.velX = (dx / distance) * MOVE_SPEED * deltaTime
        entity.physics.velY = (dy / distance) * MOVE_SPEED * deltaTime
    end

    MonsterCommon.updateSprite(entity, mem, player)
end
