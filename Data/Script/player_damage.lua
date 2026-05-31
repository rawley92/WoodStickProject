local PlayerDamage = {}

local INVINCIBLE_TIME = 2.0
local KNOCKBACK_DISTANCE = 0.2
local DAMAGE_FLASH_TIME = 0.22

local function minHp()
    if _G.GameState ~= nil and _G.GameState.debugNoDeath then
        return 1
    end

    return 0
end

local function knockbackDirection(player, attacker)
    local dx = 1.0
    local dy = 0.0

    if player ~= nil and attacker ~= nil then
        dx = player.physics.x - attacker.physics.x
        dy = player.physics.y - attacker.physics.y
    end

    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        if player ~= nil and player.camera ~= nil then
            dx = -player.camera.dirX
            dy = -player.camera.dirY
        else
            dx = 1.0
            dy = 0.0
        end

        distance = math.sqrt(dx * dx + dy * dy)
    end

    return dx / distance, dy / distance
end

function PlayerDamage.tick(dt)
    if _G.PlayerState == nil then return end

    local state = _G.PlayerState

    state.invincibleTimer = math.max(0, (state.invincibleTimer or 0) - dt)
    state.damageFlashTimer = math.max(0, (state.damageFlashTimer or 0) - dt)
    state.crosshairHitTimer = math.max(0, (state.crosshairHitTimer or 0) - dt)
end

function PlayerDamage.tryHit(player, attacker, amount)
    if _G.PlayerState == nil then return false end
    if (_G.PlayerState.invincibleTimer or 0) > 0 then return false end

    _G.PlayerState.hp = math.max(minHp(), _G.PlayerState.hp - amount)
    _G.PlayerState.invincibleTimer = INVINCIBLE_TIME
    _G.PlayerState.damageFlashTimer = DAMAGE_FLASH_TIME
    _G.PlayerState.damageFlashDuration = DAMAGE_FLASH_TIME

    if player ~= nil and player.physics ~= nil then
        local dirX, dirY = knockbackDirection(player, attacker)
        player.physics.velX = player.physics.velX + dirX * KNOCKBACK_DISTANCE
        player.physics.velY = player.physics.velY + dirY * KNOCKBACK_DISTANCE
    end

    return true
end

return PlayerDamage
