-- Debug hotkey module.
-- 여러 씬에서 공통으로 O 키를 감지해 debug_room.lua로 전환한다.

local DebugHotkey = {}

local oWasDown = false

-- O 키가 눌린 순간만 소비한다.
-- 키를 누르고 있는 동안 매 프레임 디버그 룸을 다시 로드하지 않도록 oWasDown을 사용한다.
function DebugHotkey.consume(control)
    if control == nil then
        return false
    end

    local pressed = control.s_o_key and not oWasDown
    oWasDown = control.s_o_key

    if not pressed then
        return false
    end

    -- 디버그 룸은 현재 Lua 환경에서 즉시 실행되어 씬을 교체한다.
    local debugRoom = assert(loadfile("Data/Script/debug_room.lua", "bt", _ENV))
    debugRoom()
    return true
end

return DebugHotkey
