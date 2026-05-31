local ObjectSpawn = {}

local SCRIPT_BY_TYPE = {
    enemy_spider = "Script.monster_spider",
    enemy_bull = "Script.monster_bull",
    enemy_ghost = "Script.monster_ghost",
    item_health = "Script.item_heal",
    item_ammo = "Script.item_ammo",
    weapon_melee = "Script.weapon_melee",
    weapon_gun = "Script.weapon_gun"
}

local MONSTER_TYPES = {
    enemy_spider = true,
    enemy_bull = true,
    enemy_ghost = true
}

function ObjectSpawn.spawn(typeName, x, y)
    local scriptId = SCRIPT_BY_TYPE[typeName]
    if scriptId == nil then
        print("[OBJECT SPAWN] Unknown object type: " .. tostring(typeName))
        return nil
    end

    return engine.spawnEntity(typeName, "", x, y, scriptId, 1.0)
end

function ObjectSpawn.isMonster(typeName)
    return MONSTER_TYPES[typeName] == true
end

return ObjectSpawn
