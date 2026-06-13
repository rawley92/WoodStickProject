-- Ghost monster AI.
-- 벽 충돌용 속도 대신 위치를 직접 갱신해 느리게 접근하는 추격형 적이다.

-- 몬스터별 상태는 entityId 기반 전역 테이블에 저장한다.
if not _G.monster_states then _G.monster_states = {} end
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local CombatEffects = dofile("Data/Script/combat_effects.lua")
local MonsterCommon = dofile("Data/Script/monster_common.lua")

local STATE_IDLE = 1
local STATE_CHASE = 2

local PLAYER_SPEED = 2.5
local MOVE_SPEED = PLAYER_SPEED * 0.5
local VISION_RANGE = 4.0
local VISION_FOV = 135.0
local PROXIMITY_RANGE = 2.0
local CHASE_RANGE = 6.0
local ATTACK_RANGE = 0.9
local ATTACK_DAMAGE = 10
local ATTACK_COOLDOWN = 1.0
local MAX_HP = 16
-- MonsterCommon.ensure가 이 설정으로 이름, 스케일, 방향별 스프라이트를 초기화한다.
local CONFIG = {
    kind = "ghost",
    name = "Ghost",
    scale = 0.75,
    textures = {
        front = "Char.Ghost.state_front_1",
        back = "Char.Ghost.state_back_1",
        left = "Char.Ghost.state_left_1",
        right = "Char.Ghost.state_right_1",
        hit = "Char.Ghost.state_front_1_hit"
    }
}

-- entityId별 memory에 HP 값을 최초 1회 기록한다.
local function initStats(entity, mem)
    if not mem.statsInitialized then
        mem.maxHp = MAX_HP
        mem.hp = MAX_HP
        mem.statsInitialized = true
    end
end

-- HP가 0이 되면 렌더/업데이트 대상에서 제외한다.
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

-- Java ScriptManager가 매 프레임 호출하는 유령 엔티티 update다.
function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    
    local id = entity.entityId
    if not _G.monster_states[id] then
        -- 유령은 단순 idle/chase와 공격 쿨다운만 가진다.
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
        -- PlayerDamage가 무적 시간과 실제 HP 감소를 처리한다.
        if PlayerDamage.tryHit(player, entity, ATTACK_DAMAGE) then
            mem.attackTimer = ATTACK_COOLDOWN
        end
    end

    if distance <= 0.01 then return end

    if mem.state == STATE_IDLE then
        entity.physics.velX = 0
        entity.physics.velY = 0

        -- 시야 안에 들어오면 추격을 시작한다.
        if MonsterCommon.canDetectPlayer(entity, player, mem, distance, VISION_RANGE, VISION_FOV, PROXIMITY_RANGE) then
            mem.state = STATE_CHASE
        end

    elseif mem.state == STATE_CHASE then
        if distance > CHASE_RANGE then
            mem.state = STATE_IDLE
            MonsterCommon.updateSprite(entity, mem, player)
            return
        end
        
        local dirX = dx / distance
        local dirY = dy / distance
        MonsterCommon.face(mem, dx, dy, distance)
        
        -- 유령은 물리 속도가 아니라 위치를 직접 이동시켜 벽 충돌 제약을 약하게 받는다.
        entity.physics.x = entity.physics.x + (dirX * MOVE_SPEED * deltaTime)
        entity.physics.y = entity.physics.y + (dirY * MOVE_SPEED * deltaTime)
        
        -- 직접 이동 후 Java PhysicsCore가 잔여 속도를 추가로 적용하지 않도록 속도를 비운다.
        entity.physics.velX = 0
        entity.physics.velY = 0
    end

    MonsterCommon.updateSprite(entity, mem, player)
end
