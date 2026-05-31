_G.GameState = {
    bootComplete = true,
    currentScene = "boot",
    screenWidth = 1280,
    screenHeight = 720
}

_G.PlayerState = {
    hp = 100,
    maxHp = 100,
    invincibleTimer = 0,
    damageFlashTimer = 0,
    crosshairHitTimer = 0,
    weaponFireFrames = 0,
    weaponSwingTimer = 0,
    weaponSwingDuration = 0.5,
    weapon = nil,
    ammo = 0
}

local title = assert(loadfile("Data/Script/title.lua", "bt", _ENV))
title()
