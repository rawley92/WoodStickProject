local ObjectSpawner = {}
ObjectSpawner.__index = ObjectSpawner
ObjectSpawner.ENEMY_TYPES = { "enemy_spider", "enemy_bull", "enemy_ghost" }
ObjectSpawner.ITEM_TYPES = { "item_health", "item_ammo" }

function ObjectSpawner.new()
    local self = setmetatable({}, ObjectSpawner)

    self.objects = {}
    self.occupied = {}
    self.spawnPoints = {}
    self.itemSpawnPoints = {}

    return self
end

function ObjectSpawner:generate(maze)
    self.objects = {}
    self.occupied = {}
    self.spawnPoints = {}
    self.itemSpawnPoints = {}

    self.maze = maze

    self:collectSpawnPoints()
    self:startSpawn()

    self:spawnExit()

    local pathCount = #self.spawnPoints

    self:spawnRequiredRoomContents()

    local itemPathCount = #self.itemSpawnPoints

    local spiderCount = math.max(8, math.floor(pathCount / 85))
    local bullCount = math.max(4, math.floor(pathCount / 190))
    local ghostCount = math.max(3, math.floor(pathCount / 210))

    local ammoCount = math.max(18, math.floor(itemPathCount / 60))
    local healthCount = math.max(14, math.floor(itemPathCount / 78))

    self:spawnRandom("enemy_spider", spiderCount, 5)
    self:spawnRandom("enemy_bull", bullCount, 8)
    self:spawnRandom("enemy_ghost", ghostCount, 7)

    self:spawnRandomItem("item_ammo", ammoCount, 2)
    self:spawnRandomItem("item_health", healthCount, 3)

    self:spawnRandomItem("weapon_melee", 2, 6)
    self:spawnRandomItem("weapon_gun", 2, 7)


    return self.objects
end

function ObjectSpawner:startSpawn()
    local start = self.maze.start

    if start == nil then
        start = self:findSpawnPosition()
    end

    self.maze.start = start

    self:addObject(
        "player_start",
        start.x,
        start.y
    )
end

function ObjectSpawner:findSpawnPosition()
    if self.maze.rooms ~= nil and #self.maze.rooms > 0 then
        local room = self.maze.rooms[1]

        if room ~= nil then
            return {
                x = math.floor(room.x + room.width / 2),
                y = math.floor(room.y + room.height / 2)
            }
        end
    end

    for y = 2, self.maze.height - 1 do
        for x = 2, self.maze.width - 1 do
            if self.maze.map[y][x] == self.maze.PATH then
                return {
                    x = x,
                    y = y
                }
            end
        end
    end

    return {
        x = 2,
        y = 2
    }
end

function ObjectSpawner:collectSpawnPoints()
    for y = 2, self.maze.height - 1 do
        for x = 2, self.maze.width - 1 do

            if self.maze.map[y][x] == self.maze.PATH then
                table.insert(self.itemSpawnPoints, {
                    x = x,
                    y = y
                })

                local exits = 0

                if self.maze.map[y - 1][x] == self.maze.PATH then
                    exits = exits + 1
                end

                if self.maze.map[y + 1][x] == self.maze.PATH then
                    exits = exits + 1
                end

                if self.maze.map[y][x - 1] == self.maze.PATH then
                    exits = exits + 1
                end

                if self.maze.map[y][x + 1] == self.maze.PATH then
                    exits = exits + 1
                end

                if exits >= 2 then
                    table.insert(self.spawnPoints, {
                        x = x,
                        y = y
                    })
                end

            end
        end
    end
end

function ObjectSpawner:spawnExit()
    local bestX = self.maze.start.x
    local bestY = self.maze.start.y
    local bestDistance = 0

    for _, point in ipairs(self.spawnPoints) do

        local dist =
            math.abs(point.x - self.maze.start.x) +
            math.abs(point.y - self.maze.start.y)

        if dist > bestDistance then
            bestDistance = dist
            bestX = point.x
            bestY = point.y
        end
    end

    self:addObject(
        "exit",
        bestX,
        bestY
    )

    self.maze.exit = {
        x = bestX,
        y = bestY
    }
end

function ObjectSpawner:shufflePoints(points)
    for i = #points, 2, -1 do
        local j = math.random(1, i)
        points[i], points[j] = points[j], points[i]
    end
end

function ObjectSpawner:getDistanceFromStart(x, y)
    if self.maze.start == nil then
        return 9999
    end

    return math.abs(x - self.maze.start.x) + math.abs(y - self.maze.start.y)
end

function ObjectSpawner:isOpenPosition(x, y)
    local key = y .. ":" .. x
    return self.maze.map[y] ~= nil and
           self.maze.map[y][x] == self.maze.PATH and
           not self.occupied[key]
end

function ObjectSpawner:collectRoomSpawnPoints(room)
    local points = {}
    local endX = room.x + room.width - 1
    local endY = room.y + room.height - 1

    for y = room.y, endY do
        for x = room.x, endX do
            if self:isOpenPosition(x, y) then
                table.insert(points, {
                    x = x,
                    y = y
                })
            end
        end
    end

    return points
end

function ObjectSpawner:spawnFromPoints(typeName, points, minDistance, startDistance)
    if points == nil or #points == 0 then
        return false
    end

    self:shufflePoints(points)

    for _, point in ipairs(points) do
        if self:getDistanceFromStart(point.x, point.y) >= startDistance and
           self:isPositionValid(point.x, point.y, minDistance) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    for _, point in ipairs(points) do
        if self:getDistanceFromStart(point.x, point.y) >= startDistance and
           self:isOpenPosition(point.x, point.y) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    for _, point in ipairs(points) do
        if self:isOpenPosition(point.x, point.y) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    return false
end

function ObjectSpawner:spawnRequiredRoomContents()
    if self.maze.rooms == nil then
        return
    end

    for index, room in ipairs(self.maze.rooms) do
        local enemyType = self.ENEMY_TYPES[((index - 1) % #self.ENEMY_TYPES) + 1]
        local itemType = self.ITEM_TYPES[((index - 1) % #self.ITEM_TYPES) + 1]

        self:spawnFromPoints(enemyType, self:collectRoomSpawnPoints(room), 4, 8)
        self:spawnFromPoints(itemType, self:collectRoomSpawnPoints(room), 2, 4)
    end
end

function ObjectSpawner:spawnRandom(typeName, count, minDistance)
    return self:spawnRandomFrom(typeName, count, minDistance, self.spawnPoints, 10, false)
end

function ObjectSpawner:spawnRandomItem(typeName, count, minDistance)
    return self:spawnRandomFrom(typeName, count, minDistance, self.itemSpawnPoints, 6, true)
end

function ObjectSpawner:spawnRandomFrom(typeName, count, minDistance, points, minStartDistance, allowRelaxed)
    if points == nil or #points == 0 then
        return
    end

    local spawned = 0
    local attempts = 0
    local maxAttempts = math.max(4000, count * 250)

    while spawned < count and attempts < maxAttempts do
        attempts = attempts + 1

        local point =
            points[
                math.random(1, #points)
            ]

        local distanceFromStart =
            math.abs(point.x - self.maze.start.x) +
            math.abs(point.y - self.maze.start.y)

        if distanceFromStart >= minStartDistance and
           self:isPositionValid(
                point.x,
                point.y,
                minDistance
           ) then

            self:addObject(
                typeName,
                point.x,
                point.y
            )

            spawned = spawned + 1
        end
    end

    if not allowRelaxed or spawned >= count then
        return spawned
    end

    local candidates = {}
    for i, point in ipairs(points) do
        candidates[i] = point
    end

    self:shufflePoints(candidates)

    for _, point in ipairs(candidates) do
        if spawned >= count then
            break
        end

        if self:getDistanceFromStart(point.x, point.y) >= minStartDistance and
           self:isPositionValid(point.x, point.y, minDistance) then
            self:addObject(typeName, point.x, point.y)
            spawned = spawned + 1
        end
    end

    for _, point in ipairs(candidates) do
        if spawned >= count then
            break
        end

        if self:getDistanceFromStart(point.x, point.y) >= minStartDistance and
           self:isOpenPosition(point.x, point.y) then
            self:addObject(typeName, point.x, point.y)
            spawned = spawned + 1
        end
    end

    return spawned
end

function ObjectSpawner:isPositionValid(x, y, minDistance)
    local key = y .. ":" .. x

    if self.occupied[key] then
        return false
    end

    for _, object in ipairs(self.objects) do

        local dist =
            math.abs(object.x - x) +
            math.abs(object.y - y)

        if dist < minDistance then
            return false
        end
    end

    return true
end

function ObjectSpawner:addObject(typeName, x, y)
    local key = y .. ":" .. x

    self.occupied[key] = true

    table.insert(self.objects, {
        type = typeName,
        x = x,
        y = y
    })
end

return ObjectSpawner
