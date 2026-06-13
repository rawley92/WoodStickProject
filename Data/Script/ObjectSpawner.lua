-- Maze object placement module.
-- MazeGenerator가 만든 PATH/WALL 배열 위에 시작점, 출구, 적, 아이템, 무기 배치 레코드를 생성한다.

local ObjectSpawner = {}
ObjectSpawner.__index = ObjectSpawner
ObjectSpawner.ENEMY_TYPES = { "enemy_spider", "enemy_bull", "enemy_ghost" }
ObjectSpawner.ITEM_TYPES = { "item_health", "item_ammo" }

-- 배치 결과와 점유 상태를 보관하는 스포너 인스턴스를 만든다.
function ObjectSpawner.new()
    local self = setmetatable({}, ObjectSpawner)

    self.objects = {}
    self.occupied = {}
    self.spawnPoints = {}
    self.itemSpawnPoints = {}

    return self
end

-- 생성된 미로를 기준으로 전체 오브젝트 배치 목록을 만든다.
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

    -- 방 내부가 비어 보이지 않도록 방마다 최소 컨텐츠를 먼저 배치한다.
    self:spawnRequiredRoomContents()

    local itemPathCount = #self.itemSpawnPoints

    -- 맵 규모에 따라 수량을 늘리되, 작은 맵에서도 최소 밀도를 보장한다.
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

-- 시작점 오브젝트를 배치하고 maze.start를 확정한다.
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

-- 방 중심을 우선 시작점으로 사용하고, 없으면 첫 번째 길 타일을 사용한다.
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

-- 모든 길 타일을 아이템 후보로, 2방향 이상 열린 길 타일을 적 후보로 수집한다.
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

                -- 막다른 길보다 교차/복도형 위치에 적을 배치해 조우 가능성을 높인다.
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

-- 시작점에서 맨해튼 거리가 가장 먼 후보를 출구로 선택한다.
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

-- 후보 위치 순서를 무작위화한다.
function ObjectSpawner:shufflePoints(points)
    for i = #points, 2, -1 do
        local j = math.random(1, i)
        points[i], points[j] = points[j], points[i]
    end
end

-- 시작점과의 맨해튼 거리를 계산한다.
function ObjectSpawner:getDistanceFromStart(x, y)
    if self.maze.start == nil then
        return 9999
    end

    return math.abs(x - self.maze.start.x) + math.abs(y - self.maze.start.y)
end

-- 해당 타일이 통로이고 아직 다른 오브젝트가 점유하지 않았는지 검사한다.
function ObjectSpawner:isOpenPosition(x, y)
    local key = y .. ":" .. x
    return self.maze.map[y] ~= nil and
           self.maze.map[y][x] == self.maze.PATH and
           not self.occupied[key]
end

-- 특정 방 내부에서 아직 비어 있는 스폰 후보를 모은다.
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

-- 후보 목록에서 조건을 만족하는 한 지점을 찾아 오브젝트를 배치한다.
function ObjectSpawner:spawnFromPoints(typeName, points, minDistance, startDistance)
    if points == nil or #points == 0 then
        return false
    end

    self:shufflePoints(points)

    -- 1차: 시작점 거리와 주변 오브젝트 거리 제한을 모두 만족하는 위치를 찾는다.
    for _, point in ipairs(points) do
        if self:getDistanceFromStart(point.x, point.y) >= startDistance and
           self:isPositionValid(point.x, point.y, minDistance) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    -- 2차: 주변 거리 제한을 완화하고 시작점 거리와 점유 상태만 확인한다.
    for _, point in ipairs(points) do
        if self:getDistanceFromStart(point.x, point.y) >= startDistance and
           self:isOpenPosition(point.x, point.y) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    -- 3차: 그래도 실패하면 비어 있는 위치만 보장한다.
    for _, point in ipairs(points) do
        if self:isOpenPosition(point.x, point.y) then
            self:addObject(typeName, point.x, point.y)
            return true
        end
    end

    return false
end

-- 각 방에 적과 아이템을 최소 하나씩 배치해 방 탐색 보상을 만든다.
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

-- 적 배치용 후보군에서 랜덤 스폰을 수행한다.
function ObjectSpawner:spawnRandom(typeName, count, minDistance)
    return self:spawnRandomFrom(typeName, count, minDistance, self.spawnPoints, 10, false)
end

-- 아이템 배치용 후보군에서 랜덤 스폰을 수행한다.
function ObjectSpawner:spawnRandomItem(typeName, count, minDistance)
    return self:spawnRandomFrom(typeName, count, minDistance, self.itemSpawnPoints, 6, true)
end

-- 지정 후보군에서 여러 오브젝트를 무작위 배치한다.
function ObjectSpawner:spawnRandomFrom(typeName, count, minDistance, points, minStartDistance, allowRelaxed)
    if points == nil or #points == 0 then
        return
    end

    local spawned = 0
    local attempts = 0
    local maxAttempts = math.max(4000, count * 250)

    while spawned < count and attempts < maxAttempts do
        attempts = attempts + 1

        -- 후보를 무작위로 뽑아 거리 조건을 만족하면 즉시 배치한다.
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

    -- 아이템은 요청 수량을 최대한 채우기 위해 랜덤 실패 후 순차 후보 탐색을 추가로 수행한다.
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

-- 이미 점유된 위치이거나 주변 오브젝트와 너무 가까운 위치를 제외한다.
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

-- 오브젝트 레코드를 결과 배열에 추가하고 해당 타일을 점유 처리한다.
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
