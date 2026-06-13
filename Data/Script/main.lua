-- Engine entry script.
-- Java의 Script.main 호출로 최초 실행되며, 여러 Lua 스크립트가 공유하는 전역 상태를 만든 뒤 타이틀 씬으로 넘긴다.

-- _G.GameState는 씬 전환, 미로 생성 결과, 디버그 상태처럼 게임 전체가 공유하는 런타임 상태다.
_G.GameState = {
    bootComplete = true,
    currentScene = "boot",
    screenWidth = 1280,
    screenHeight = 720
}

-- _G.PlayerState는 체력, 무기, 탄약, 피격 효과처럼 플레이어 중심 규칙 상태를 저장한다.
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

-- 타이틀 스크립트는 파일 로드 후 즉시 실행되어 Title scene controller를 생성한다.
local title = assert(loadfile("Data/Script/title.lua", "bt", _ENV))
title()
