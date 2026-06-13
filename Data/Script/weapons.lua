-- Shared weapon definitions and pickup interaction rules.

local Weapons = {}

local DEFINITIONS = {
    Melee = {
        damage = 5,
        range = 2.0,
        animation = "Melee",
        missMessage = "Swing",
        ammoOnPickup = 0
    },
    Gun = {
        damage = 7,
        range = 8.0,
        animation = "Gun",
        ammoCost = 1,
        missMessage = "Miss",
        ammoOnPickup = 12
    }
}

function Weapons.get(name)
    return DEFINITIONS[name]
end

function Weapons.pickup(name, entity)
    local weapon = DEFINITIONS[name]
    if weapon == nil then
        return
    end

    _G.PlayerState = _G.PlayerState or {
        hp = 100,
        maxHp = 100,
        weapon = nil,
        ammo = 0
    }

    _G.PlayerState.weapon = name

    if weapon.ammoOnPickup ~= nil then
        _G.PlayerState.ammo = weapon.ammoOnPickup
    end

    if entity ~= nil then
        entity.isActive = false
    end
end

return Weapons
