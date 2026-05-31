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

function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    
    local id = entity.entityId
    if not _G.monster_states[id] then
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
        if PlayerDamage.tryHit(player, entity, ATTACK_DAMAGE) then
            mem.attackTimer = ATTACK_COOLDOWN
        end
    end

    if distance <= 0.01 then return end

    if mem.state == STATE_IDLE then
        entity.physics.velX = 0
        entity.physics.velY = 0

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
        
        entity.physics.x = entity.physics.x + (dirX * MOVE_SPEED * deltaTime)
        entity.physics.y = entity.physics.y + (dirY * MOVE_SPEED * deltaTime)
        
        entity.physics.velX = 0
        entity.physics.velY = 0
    end

    MonsterCommon.updateSprite(entity, mem, player)
end
