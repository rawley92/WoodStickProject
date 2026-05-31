local NAME = "Weapon_Gun"
local TEXTURE = "Textures.UI.Weapon.Gun.gun_fps"
local SCALE = 0.28
local AMMO_ON_PICKUP = 12

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

    _G.PlayerState.weapon = "Gun"
    _G.PlayerState.ammo = AMMO_ON_PICKUP
    entity.isActive = false
end
