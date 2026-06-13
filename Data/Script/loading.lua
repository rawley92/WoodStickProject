-- Loading scene controller.
-- 미로를 한 번에 생성하지 않고 여러 프레임에 나누어 생성하면서 진행률과 미리보기를 UI로 보여준다.

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

-- 로딩 씬에서 사용할 기본 레이캐스팅 텍스처를 바인딩한다.
local function setupWorld() 
    engine.assignWallTexture("Textures.Level.Wall_1") 
    engine.setFloorTexture("Textures.Level.Floor_1") 
    engine.setCeilingTexture("Textures.Level.Celling_1") 
end 

-- 생성기가 출구를 제공하지 못했을 때 시작점에서 가장 먼 길 타일을 fallback 출구로 고른다.
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

-- Java 씬으로 넘어가기 전에 생성 결과를 전역 GameState에 게시한다.
-- maze.lua는 이 값을 읽어 플레이어 시작점, 출구, 스폰 오브젝트를 구성한다.
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

-- 미로 생성이 끝난 뒤 오브젝트 배치, map.dat 저장, 전역 상태 게시를 한 번만 수행한다.
local function finishGeneration()
    if generationDone or generator == nil then return end

    local spawner = ObjectSpawner.new()
    local objects = spawner:generate(generator)

    -- ScriptAPI.initScene은 map.dat를 읽으므로 Lua 생성 결과를 파일로 내려쓴다.
    generator:saveToFile("Data/Level/maze/")
    publishMazeState(objects)

    generationDone = true
    generationProgress = 1.0
end

-- 로딩 씬을 초기화하고 animated maze generation 상태를 시작한다.
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

-- 생성 중인 미로 배열을 2D UI 사각형으로 축소 렌더링한다.
local function drawMazePreview() 
    local maze = _G.GameState.maze 
    if maze == nil or maze.map == nil then return end 

    local previewWidth = 360 
    local previewHeight = 360 

    -- 미로 크기가 바뀌어도 preview 영역 안에 들어오도록 타일 픽셀 크기를 계산한다.
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
            -- UI 좌표는 1-based Lua 맵 인덱스를 0-based 화면 픽셀 좌표로 변환해 사용한다.
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

-- 로딩 상태를 정리하고 실제 미로 플레이 씬 스크립트를 실행한다.
local function goToMaze() 
    generationProgress = 0
    generationDone = false
    enterWasDown = false
    isInitialized = false

    local mazeScript = assert(loadfile("Data/Script/maze.lua", "bt", _ENV))
    mazeScript()
end 
 
-- Loading_Controller 엔티티가 매 프레임 호출하는 진입점이다.
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
        -- dt 기반 step 수로 생성 속도를 프레임레이트와 분리한다.
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
    -- 진행률 바는 generationProgress를 픽셀 폭으로 변환한다.
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
