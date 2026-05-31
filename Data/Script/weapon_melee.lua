local NAME = "Weapon_Melee"
local TEXTURE = "Textures.UI.Weapon.club.club"
local SCALE = 0.28

local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

function update(entity, dt, player, control)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance >= 1.0 then return end

    _G.PlayerState.weapon = "Melee"
    _G.PlayerState.ammo = 0
    entity.isActive = false
end
