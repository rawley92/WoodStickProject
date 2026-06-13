-- Bull monster AI.
-- 플레이어를 감지하면 일정 시간 직선 돌진하고, 회복 시간을 가진 뒤 다시 돌진하는 돌격형 적이다.

-- 몬스터별 상태는 ScriptManager의 공유 Lua env와 분리하기 위해 entityId로 관리한다.
if not _G.monster_states then _G.monster_states = {} end
local PlayerDamage = dofile("Data/Script/player_damage.lua")
local CombatEffects = dofile("Data/Script/combat_effects.lua")
local MonsterCommon = dofile("Data/Script/monster_common.lua")
local STATE_IDLE = 1
local STATE_CHASE = 2
local STATE_RECOVER = 3

local PLAYER_SPEED = 2.5
local MOVE_SPEED = PLAYER_SPEED * 2.0
local VISION_RANGE = 4.0
local VISION_FOV = 90.0
local PROXIMITY_RANGE = 2.0
local CHASE_RANGE = 4.0
local ATTACK_RANGE = 1.0
local ATTACK_DAMAGE = 20
local ATTACK_COOLDOWN = 1.2
local MAX_HP = 24
local MOVE_DISTANCE = 10.0
-- 돌진 지속 시간은 원하는 이동 거리와 이동 속도에서 역산한다.
local MOVE_TIME = MOVE_DISTANCE / MOVE_SPEED
local RECOVER_TIME = 1.0
-- MonsterCommon.ensure가 이 설정으로 이름, 스케일, 방향별 스프라이트를 초기화한다.
local CONFIG = {
    kind = "bull",
    name = "Bull",
    scale = 0.85,
    textures = {
        front = "Char.Bulls.state_front_1",
        back = "Char.Bulls.state_back_1",
        left = "Char.Bulls.state_left_1",
        right = "Char.Bulls.state_right_1",
        hit = "Char.Bulls.state_front_1_hit"
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

-- 사망한 황소를 Java 업데이트/렌더 대상에서 제외한다.
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

-- 돌진/회복 상태에서 이동을 즉시 멈춘다.
local function stop(entity)
    entity.physics.velX = 0
    entity.physics.velY = 0
end

-- 현재 플레이어 방향을 고정 돌진 방향으로 저장한다.
local function startCharge(mem, dx, dy, distance)
    mem.chargeDirX = dx / distance
    mem.chargeDirY = dy / distance
    mem.moveTimer = MOVE_TIME
    MonsterCommon.face(mem, dx, dy, distance)
end

-- Java ScriptManager가 매 프레임 호출하는 황소 엔티티 update다.
function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    
    local id = entity.entityId
    if not _G.monster_states[id] then
        -- 돌진형 AI는 state 외에도 회복/이동 타이머와 고정 돌진 방향을 기억한다.
        _G.monster_states[id] = {
            state = STATE_IDLE,
            recoverTimer = 0,
            moveTimer = 0,
            chargeDirX = 0,
            chargeDirY = 0,
            attackTimer = 0
        }
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
        -- 충돌하듯 가까워졌을 때 피해를 주고, 쿨다운 동안 반복 피해를 막는다.
        if PlayerDamage.tryHit(player, entity, ATTACK_DAMAGE) then
            mem.attackTimer = ATTACK_COOLDOWN
        end
    end

    if distance <= 0.01 then return end

    if mem.state == STATE_IDLE then
        stop(entity)

        -- 감지 순간의 방향을 저장해 돌진 중에는 방향을 꺾지 않는다.
        if MonsterCommon.canDetectPlayer(entity, player, mem, distance, VISION_RANGE, VISION_FOV, PROXIMITY_RANGE) then
            mem.state = STATE_CHASE
            startCharge(mem, dx, dy, distance)
        end
    elseif mem.state == STATE_CHASE then
        mem.moveTimer = (mem.moveTimer or 0) - deltaTime
        if mem.moveTimer <= 0 then
            -- 돌진 시간이 끝나면 멈춘 뒤 RECOVER 상태로 들어간다.
            mem.state = STATE_RECOVER
            mem.recoverTimer = RECOVER_TIME
            stop(entity)
            MonsterCommon.updateSprite(entity, mem, player)
            return
        end

        -- startCharge에서 고정한 방향으로만 이동한다.
        entity.physics.velX = mem.chargeDirX * MOVE_SPEED * deltaTime
        entity.physics.velY = mem.chargeDirY * MOVE_SPEED * deltaTime
    elseif mem.state == STATE_RECOVER then
        stop(entity)

        if distance > CHASE_RANGE then
            mem.state = STATE_IDLE
            MonsterCommon.updateSprite(entity, mem, player)
            return
        end

        mem.recoverTimer = (mem.recoverTimer or 0) - deltaTime
        if mem.recoverTimer <= 0 then
            -- 회복이 끝나면 현재 플레이어 위치를 기준으로 새 돌진 방향을 잡는다.
            mem.state = STATE_CHASE
            startCharge(mem, dx, dy, distance)
        end
    end

    MonsterCommon.updateSprite(entity, mem, player)
end
