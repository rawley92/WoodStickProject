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
local MOVE_TIME = MOVE_DISTANCE / MOVE_SPEED
local RECOVER_TIME = 1.0
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

local function initStats(entity, mem)
    if not mem.statsInitialized then
        mem.maxHp = MAX_HP
        mem.hp = MAX_HP
        mem.statsInitialized = true
    end
end

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

local function stop(entity)
    entity.physics.velX = 0
    entity.physics.velY = 0
end

local function startCharge(mem, dx, dy, distance)
    mem.chargeDirX = dx / distance
    mem.chargeDirY = dy / distance
    mem.moveTimer = MOVE_TIME
    MonsterCommon.face(mem, dx, dy, distance)
end

function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    
    local id = entity.entityId
    if not _G.monster_states[id] then
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
        if PlayerDamage.tryHit(player, entity, ATTACK_DAMAGE) then
            mem.attackTimer = ATTACK_COOLDOWN
        end
    end

    if distance <= 0.01 then return end

    if mem.state == STATE_IDLE then
        stop(entity)

        if MonsterCommon.canDetectPlayer(entity, player, mem, distance, VISION_RANGE, VISION_FOV, PROXIMITY_RANGE) then
            mem.state = STATE_CHASE
            startCharge(mem, dx, dy, distance)
        end
    elseif mem.state == STATE_CHASE then
        mem.moveTimer = (mem.moveTimer or 0) - deltaTime
        if mem.moveTimer <= 0 then
            mem.state = STATE_RECOVER
            mem.recoverTimer = RECOVER_TIME
            stop(entity)
            MonsterCommon.updateSprite(entity, mem, player)
            return
        end

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
            mem.state = STATE_CHASE
            startCharge(mem, dx, dy, distance)
        end
    end

    MonsterCommon.updateSprite(entity, mem, player)
end
