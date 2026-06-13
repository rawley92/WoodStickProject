-- Melee weapon pickup script.
-- 플레이어가 가까이 오면 장착 무기를 근접 무기로 바꾸고 탄약을 사용하지 않게 한다.

local NAME = "Weapon_Melee"
local TEXTURE = "Textures.UI.Weapon.club.club"
local SCALE = 0.28
local Weapons = dofile("Data/Script/weapons.lua")

-- 첫 update에서 빈 assetId를 실제 근접 무기 외형으로 바꾼다.
local function ensureVisual(entity)
    if entity.assetId == TEXTURE then return end

    entity.name = NAME
    entity.assetId = TEXTURE

    if entity.render ~= nil then
        entity.render.assetId = TEXTURE
        entity.render.scale = SCALE
    end
end

-- 플레이어 접근 거리 기반으로 근접 무기 획득을 처리한다.
function update(entity, dt, player, control)
    if not entity.isActive then return end
    ensureVisual(entity)

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y
    local distance = math.sqrt(dx * dx + dy * dy)

    if distance >= 1.0 then return end

    -- maze.lua는 이 문자열을 보고 근접 공격 범위/데미지/애니메이션을 선택한다.
    Weapons.pickup("Melee", entity)
end
