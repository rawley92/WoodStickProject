local CombatEffects = {}

local ENEMY_HIT_FLASH_TIME = 0.14
local CROSSHAIR_HIT_TIME = 0.18
local ENEMY_KNOCKBACK_DISTANCE = 0.1

local function hitDirection(player, target)
    local dx = 1.0
    local dy = 0.0

    if player ~= nil and target ~= nil then
        dx = target.physics.x - player.physics.x
        dy = target.physics.y - player.physics.y
    end

    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        if player ~= nil and player.camera ~= nil then
            dx = player.camera.dirX
            dy = player.camera.dirY
        else
            dx = 1.0
            dy = 0.0
        end

        distance = math.sqrt(dx * dx + dy * dy)
    end

    return dx / distance, dy / distance
end

function CombatEffects.markEnemyHit(player, target)
    if _G.PlayerState ~= nil then
        _G.PlayerState.crosshairHitTimer = CROSSHAIR_HIT_TIME
        _G.PlayerState.crosshairHitDuration = CROSSHAIR_HIT_TIME
    end

    if target == nil or target.physics == nil then return end

    local mem = _G.monster_states ~= nil and _G.monster_states[target.entityId] or nil

    if mem ~= nil then
        local hitAssetId = mem.hitAssetId

        if hitAssetId ~= nil and target.assetId ~= hitAssetId then
            mem.assetBeforeHit = mem.assetBeforeHit or target.assetId
            target.assetId = hitAssetId
            if target.render ~= nil then
                target.render.assetId = hitAssetId
            end
            mem.hitFlashTimer = ENEMY_HIT_FLASH_TIME
        end
    end

    local dirX, dirY = hitDirection(player, target)
    local nextX = target.physics.x + dirX * ENEMY_KNOCKBACK_DISTANCE
    local nextY = target.physics.y + dirY * ENEMY_KNOCKBACK_DISTANCE

    if engine ~= nil and engine.hasWallBetween ~= nil then
        if engine:hasWallBetween(target.physics.x, target.physics.y, nextX, nextY) then
            return
        end
    end

    target.physics.x = nextX
    target.physics.y = nextY
end

function CombatEffects.tickEnemy(entity, mem, dt)
    if entity == nil or mem == nil then return end

    if (mem.hitFlashTimer or 0) <= 0 then return end

    mem.hitFlashTimer = math.max(0, mem.hitFlashTimer - dt)

    if mem.hitFlashTimer <= 0 and mem.assetBeforeHit ~= nil then
        entity.assetId = mem.assetBeforeHit
        if entity.render ~= nil then
            entity.render.assetId = mem.assetBeforeHit
        end
        mem.assetBeforeHit = nil
    end
end

return CombatEffects
