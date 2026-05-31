local MonsterCommon = {}

local DEFAULT_FACING_X = -1.0
local DEFAULT_FACING_Y = 0.0

function MonsterCommon.ensure(entity, mem, config)
    if mem.visualInitialized then return end

    mem.monsterKind = config.kind
    mem.textures = config.textures
    mem.hitAssetId = mem.textures ~= nil and mem.textures.hit or nil
    mem.facingX = mem.facingX or DEFAULT_FACING_X
    mem.facingY = mem.facingY or DEFAULT_FACING_Y
    mem.visualInitialized = true

    if config.name ~= nil then
        entity.name = config.name
    end

    if entity.render ~= nil and config.scale ~= nil then
        entity.render.scale = config.scale
    end

    if mem.textures ~= nil and (entity.assetId == nil or entity.assetId == "") then
        entity.assetId = mem.textures.front
    end

    if entity.render ~= nil then
        entity.render.assetId = entity.assetId
    end
end

function MonsterCommon.face(mem, dx, dy, distance)
    if mem == nil then return end

    distance = distance or math.sqrt(dx * dx + dy * dy)
    if distance <= 0.001 then return end

    mem.facingX = dx / distance
    mem.facingY = dy / distance
end

function MonsterCommon.canDetectPlayer(entity, player, mem, distance, visionRange, fovDegrees, proximityRange)
    if entity == nil or player == nil or mem == nil then return false end

    if distance <= (proximityRange or 0.0) then
        return true
    end

    if distance > visionRange then return false end

    if engine ~= nil and engine.hasWallBetween ~= nil then
        if engine:hasWallBetween(entity.physics.x, entity.physics.y, player.physics.x, player.physics.y) then
            return false
        end
    end

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y

    if distance <= 0.001 then
        return true
    end

    local toPlayerX = dx / distance
    local toPlayerY = dy / distance
    local dot = toPlayerX * (mem.facingX or DEFAULT_FACING_X) + toPlayerY * (mem.facingY or DEFAULT_FACING_Y)
    local minDot = math.cos(math.rad(fovDegrees or 120.0) * 0.5)

    return dot >= minDot
end

function MonsterCommon.updateSprite(entity, mem, player)
    if entity == nil or mem == nil or player == nil then return end
    if (mem.hitFlashTimer or 0) > 0 then return end

    local textures = mem.textures
    if textures == nil then return end

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance <= 0.001 then
        entity.assetId = textures.front
        return
    end

    local toPlayerX = dx / distance
    local toPlayerY = dy / distance
    local facingX = mem.facingX or DEFAULT_FACING_X
    local facingY = mem.facingY or DEFAULT_FACING_Y
    local frontDot = toPlayerX * facingX + toPlayerY * facingY
    local nextAsset = nil

    if frontDot >= 0.5 then
        nextAsset = textures.front
    elseif frontDot <= -0.5 then
        nextAsset = textures.back
    else
        local cross = facingX * toPlayerY - facingY * toPlayerX
        nextAsset = cross >= 0 and textures.left or textures.right
    end

    if nextAsset ~= nil then
        entity.assetId = nextAsset
        if entity.render ~= nil then
            entity.render.assetId = nextAsset
        end
    end
end

return MonsterCommon
