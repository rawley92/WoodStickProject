local NAME = "Ammo"
local TEXTURE = "Textures.obj.ammo"
local SCALE = 0.30

local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

function update(entity, deltaTime, player, control)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance >= 1.0 then return end

    _G.PlayerState = _G.PlayerState or { hp = 100, maxHp = 100, weapon = nil, ammo = 0 }
    _G.PlayerState.ammo = (_G.PlayerState.ammo or 0) + 8

    entity.isActive = false
end
