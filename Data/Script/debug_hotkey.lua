local DebugHotkey = {}

local oWasDown = false

function DebugHotkey.consume(control)
    if control == nil then
        return false
    end

    local pressed = control.s_o_key and not oWasDown
    oWasDown = control.s_o_key

    if not pressed then
        return false
    end

    local debugRoom = assert(loadfile("Data/Script/debug_room.lua", "bt", _ENV))
    debugRoom()
    return true
end

return DebugHotkey
