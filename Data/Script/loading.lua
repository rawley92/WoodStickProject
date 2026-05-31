print("LOADING LUA START") 
local MazeGenerator = dofile("Data/Script/MazeGenerator.lua") 
local ObjectSpawner = dofile("Data/Script/ObjectSpawner.lua") 
local DebugHotkey = assert(loadfile("Data/Script/debug_hotkey.lua", "bt", _ENV))()

local generator = nil
local generationDone = false
local generationProgress = 0
local enterWasDown = false 
local isInitialized = false -- 초기화 체크 변수
local GENERATION_STEPS_PER_SECOND = 900

local function setupWorld() 
    engine.assignWallTexture("Textures.Level.Wall_1") 
    engine.setFloorTexture("Textures.Level.Floor_1") 
    engine.setCeilingTexture("Textures.Level.Celling_1") 
end 

local function findExit(maze, start)
    local best = { x = start.x, y = start.y }
    local bestDistance = 0

    for y = 2, maze.height - 1 do
        for x = 2, maze.width - 1 do
            if maze.map[y][x] == maze.PATH then
                local distance = math.abs(x - start.x) + math.abs(y - start.y)

                if distance > bestDistance then
                    bestDistance = distance
                    best = { x = x, y = y }
                end
            end
        end
    end

    return best
end

local function publishMazeState(objects)
    if generator == nil then return end

    _G.GameState.maze = {
        map = generator.map,
        width = generator.width,
        height = generator.height,
        WALL = generator.WALL,
        PATH = generator.PATH,
        rooms = generator.rooms
    }

    _G.GameState.start = generator.start or { x = 2, y = 2 }
    _G.GameState.exit = generator.exit or findExit(generator, _G.GameState.start)
    _G.GameState.mazeObjects = objects or {}
end

local function finishGeneration()
    if generationDone or generator == nil then return end

    local spawner = ObjectSpawner.new()
    local objects = spawner:generate(generator)

    generator:saveToFile("Data/Level/maze/")
    publishMazeState(objects)

    generationDone = true
    generationProgress = 1.0
end

local function prepareLoading()
    _G.GameState.currentScene = "loading"
    _G.GameState.maze = nil
    _G.GameState.start = nil
    _G.GameState.exit = nil
    _G.GameState.mazeObjects = {}
    generationDone = false
    generationProgress = 0
    
    generator = MazeGenerator.new(81, 81, os.time())
    generator:startAnimatedGeneration()
    publishMazeState({})
    
    engine.initScene("Loading", "Level.loading.map") 
    setupWorld() 
    engine.setupPlayer(1.5, 1.5, -1.0, 0.0, 0.0, 0.88)
    engine.spawnEntity("Loading_Controller", "", 0.0, 0.0, "Script.loading")
end

local function drawMazePreview() 
    local maze = _G.GameState.maze 
    if maze == nil or maze.map == nil then return end 

    local previewWidth = 360 
    local previewHeight = 360 

    local tileSize = math.floor(math.min(previewWidth / maze.width, previewHeight / maze.height)) 
    tileSize = math.max(tileSize, 2) 

    local mapPixelWidth = maze.width * tileSize 
    local mapPixelHeight = maze.height * tileSize 
    local originX = math.floor((1280 - mapPixelWidth) / 2) 
    local originY = math.floor((720 - mapPixelHeight) / 2) 

    for y = 1, maze.height do 
        for x = 1, maze.width do
            local tile = maze.map[y][x] 
            local color = (tile == maze.WALL) and 0x293241 or 0xE0FBFC 
            engine.uiRect(originX + (x - 1) * tileSize, originY + (y - 1) * tileSize, tileSize - 1, tileSize - 1, color, 1.0) 
        end 
    end 

    if generator ~= nil then
        local cursor = generator:getAnimationCursor()
        if cursor ~= nil then
            engine.uiRect(originX + (cursor.x - 1) * tileSize, originY + (cursor.y - 1) * tileSize, tileSize - 1, tileSize - 1, 0xF4D35E, 1.0)
        end
    end

    local start = _G.GameState.start 
    if start ~= nil then
        engine.uiRect(originX + (start.x - 1) * tileSize, originY + (start.y - 1) * tileSize, tileSize - 1, tileSize - 1, 0xFF4D6D, 1.0) 
    end 

    local exit = _G.GameState.exit
    if generationDone and exit ~= nil then
        engine.uiRect(originX + (exit.x - 1) * tileSize, originY + (exit.y - 1) * tileSize, tileSize - 1, tileSize - 1, 0x80FF72, 1.0)
    end
end 

local function goToMaze() 
    generationProgress = 0
    generationDone = false
    enterWasDown = false
    isInitialized = false

    local mazeScript = assert(loadfile("Data/Script/maze.lua", "bt", _ENV))
    mazeScript()
end 
 
function update(entity, dt, player, control) 
    if not isInitialized then
        prepareLoading()
        isInitialized = true
        return
    end

    if DebugHotkey.consume(control) then
        return
    end

    if not generationDone and generator ~= nil then
        local steps = math.max(1, math.floor(GENERATION_STEPS_PER_SECOND * dt))
        generator:stepAnimatedGeneration(steps)
        generationProgress = generator:getAnimationProgress()

        if generator:isAnimationFinished() then
            finishGeneration()
        end
    end

    engine.uiClear() 
    engine.uiRect(0, 0, 1280, 720, 0x000000, 1.0) 
    engine.uiTextCenter("Generating Maze", 640, 108, 52, 0xFFFFFF, 1.0) 

    drawMazePreview() 
    
    local progress = math.min(1.0, generationProgress)
    engine.uiRect(360, 600, 560, 20, 0x1B263B, 1.0) 
    engine.uiRect(360, 600, math.floor(560 * progress), 20, 0xE0FBFC, 1.0) 
    
    if generationDone then
        engine.uiTextCenter("Press Enter", 640, 668, 36, 0xFFFFFF, 1.0) 
        if control ~= nil and control.s_enter and not enterWasDown then 
            goToMaze() 
            return 
        end 
    end 

    if control ~= nil then 
        enterWasDown = control.s_enter 
    end 
end

if not isInitialized then
    prepareLoading()
    isInitialized = true
end
