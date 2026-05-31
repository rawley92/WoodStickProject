local npcStates = {}

function update(entity, dt, player)

    local id = entity.entityId

    if not npcStates[id] then
        npcStates[id] = {
            baseAsset = entity.assetId
        }
    end

    local state = npcStates[id]

    local dx = player.physics.x - entity.physics.x
    local dy = player.physics.y - entity.physics.y

    local angle = math.atan2(dy, dx)

    local dist = engine.distance(entity, player)

    local dirIndex = math.floor(((angle + math.pi) / (2 * math.pi)) * 4) % 4

    local direction
    if dirIndex == 0 then direction = "right"
    elseif dirIndex == 1 then direction = "front"
    elseif dirIndex == 2 then direction = "left"
    else direction = "back"
    end

    local prefix = string.gsub(state.baseAsset, "%.Base$", "")

    entity.assetId = prefix .. ".state_" .. direction .. "_1"

    if entity.sound ~= nil then
        if dist <= 3 then
            if not state.inRange then
                entity.sound:play("Audio.SfxTest", false)
            end
            state.inRange = true
        else
            state.inRange = false
        end
    end
end