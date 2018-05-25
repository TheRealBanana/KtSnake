import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import java.util.*
import kotlin.math.floor

fun randgrid(end: Int) = Random().nextInt(end)



data class ColorTuple(val r: Float, val g: Float, val b: Float)
data class Grid(val row: Int, val col: Int)
data class Point(val x: Int, val y: Int)
data class Vertices(val bottom_right: Point, val side_size_px: Int) {
    val br: Point
    val tr: Point
    val tl: Point
    val bl: Point

    init {
        this.br = bottom_right
        this.tr = Point(bottom_right.x, bottom_right.y - side_size_px)
        this.tl = Point(bottom_right.x - side_size_px, bottom_right.y - side_size_px)
        this.bl = Point(bottom_right.x - side_size_px, bottom_right.y)
    }
}


enum class GridElementTypes(val color: ColorTuple){
    DEFAULT_COLOR(ColorTuple(0.0f,0.0f,0.0f)),
    SNAKE_HEAD(ColorTuple(0.25882f,1.0f,0.0f)),
    SNAKE_TAIL(ColorTuple(0.15686f,0.58823f,0.0f)),
    OBJECTIVE(ColorTuple(0.0f,0.32941f,0.65098f)),
    DEAD_HEAD(ColorTuple(1.0f,0.0f,0.0f))
}

enum class Directions(val dir: String) {
    DOWN("down"),
    UP("up"),
    RIGHT("right"),
    LEFT("left")
}

class GameGrid(snake: Snake, rows: Int, cols: Int, side_size_px: Int) {
    val MAX_OBJS: Int = 10
    val snake: Snake
    private val rows: Int
    private val cols: Int
    private val sidesizepx: Int
    private val activeGridElements: MutableMap<Grid, GridElement>
    val objectiveList: MutableList<Grid> = mutableListOf()

    init {
        this.snake = snake
        this.rows = rows
        this.cols = cols
        this.sidesizepx = side_size_px
        this.activeGridElements = mutableMapOf<Grid, GridElement>()
    }

    fun addObjective() {
        if (objectiveList.size == MAX_OBJS) {
            deleteGridElement((objectiveList.elementAt(0)))
            objectiveList.removeAt(0)
        }
        //Create random new grid thats not already taken
        var newobjgrid: Grid = Grid(randgrid(rows), randgrid(cols))
        while (newobjgrid in objectiveList || newobjgrid in snake.snakegrids) {
            newobjgrid = Grid(randgrid(rows), randgrid(cols))
        }
        objectiveList.add(newobjgrid)
        createGridElement(GridElementTypes.OBJECTIVE, newobjgrid)
    }

    fun createGridElement(element_type: GridElementTypes, grid_index: Grid): Boolean{
        if (activeGridElements.containsKey(grid_index)) {
            deleteGridElement(grid_index)
        }
        val xcoord: Int = sidesizepx * grid_index.row + sidesizepx
        val ycoord: Int = sidesizepx * grid_index.col + sidesizepx
        val origin_coords = Point(xcoord, ycoord)
        val new_grid_element = GridElement(element_type.color, origin_coords, sidesizepx)
        activeGridElements[grid_index] =  new_grid_element
        return true
    }
    fun deleteGridElement(grid_index: Grid){
        if (activeGridElements.containsKey(grid_index)) {
            activeGridElements.remove(grid_index)
        } else {
            throw Exception("Tried to delete non-existent grid at index %s".format(grid_index.toString()))
        }
    }

    fun redrawGrid() {
        glClear(GL_COLOR_BUFFER_BIT)
        for ((_, grid_element) in activeGridElements) {
            grid_element.draw()
        }
    }

    fun clearGrid() {
        activeGridElements.clear()
    }

    fun moveSnake(): Boolean{
        val nextmove: Grid = snake.getMove()

        //Check if we hit the wall
        if (!(0 <= nextmove.row && nextmove.row < rows)){
            snake.alive = false
            println("Hit a wall, we ded fam X(")
            return false
        } else if (!(0 <= nextmove.col && nextmove.row < cols)){
            snake.alive = false
            println("Hit a wall, we ded fam X(")
            return false
        }
        // check if we hit ourselves
        if (nextmove in snake.snakegrids) {
            snake.alive = false
            println("Hit ourselves, we ded fam X(")
            return false
        }

        //Good move so far, lets make it happen
        createGridElement(GridElementTypes.SNAKE_HEAD, nextmove)
        createGridElement(GridElementTypes.SNAKE_TAIL, snake.currentgrid)

        //delete any grids excess grids if we are too long (truncate snake)
        snake.snakegrids.add(nextmove)
        snake.currentgrid = nextmove
        if (snake.snakegrids.size == snake.length) {
            deleteGridElement(snake.snakegrids[0])
            snake.snakegrids.removeAt(0)
        }

        //collected an objective, increase size
        if (nextmove in objectiveList) {
            objectiveList.remove(nextmove)
            snake.length += 1
        }


        return true
    }

    fun GameOver() {
        // We died, show score and wait for user to exit
        // Also color the snake head red so we know we ded
        createGridElement(GridElementTypes.DEAD_HEAD, snake.snakegrids[snake.snakegrids.size-1])
        redrawGrid()
        println("Final Score: %d".format(snake.length-5)) //subtract initial snake size
    }

}

class GridElement (type: ColorTuple, origin_coords: Point, size_px: Int) {
    private val color: ColorTuple
    private val origincoords: Point
    private val sizepx: Int
    private val vertices: Vertices = Vertices(origin_coords, size_px)

    init {
        this.color = type
        this.origincoords = origin_coords
        this.sizepx = size_px
    }

    fun draw() {
        // Figure out our vertices
        glColor3f(color.r, color.g, color.b)
        glBegin(GL_QUADS)
        glVertex2i(vertices.br.x, vertices.br.y)
        glVertex2i(vertices.tr.x, vertices.tr.y)
        glVertex2i(vertices.tl.x, vertices.tl.y)
        glVertex2i(vertices.bl.x, vertices.bl.y)
        glEnd()
    }
}

class Snake(start_grid: Grid, start_direction: Directions) {
    var alive: Boolean = true
    var currentgrid: Grid
    var direction: Directions
    var length: Int = 5 //initial snake size
    val snakegrids: MutableList<Grid> = mutableListOf()

    init {
        this.currentgrid = start_grid
        this.snakegrids.add(start_grid)
        this.direction = start_direction
    }

    fun getMove(dir: Directions = this.direction): Grid {
        var nextgrid: Grid

        when (dir.dir) {
            "down" -> nextgrid = Grid(currentgrid.row, currentgrid.col+1)
            "up" -> nextgrid = Grid(currentgrid.row, currentgrid.col-1)
            "right" -> nextgrid = Grid(currentgrid.row+1, currentgrid.col)
            else -> nextgrid = Grid(currentgrid.row-1, currentgrid.col) // Using else instead of "left" cause kotlin doesn't like the ambiguity.
        }
        return nextgrid
    }

    fun changeDirection(newdir: Directions) {
        if (alive) {
            if (snakegrids.size > 1) {
                if (getMove(newdir) != snakegrids[snakegrids.size-2] ) {
                    this.direction = newdir
                }
            } else {
                this.direction = newdir
            }
        }
    }
    fun glfwKeypressCallback(key: Int) {
        when (key) {
            GLFW_KEY_UP -> changeDirection(Directions.UP)
            GLFW_KEY_DOWN -> changeDirection(Directions.DOWN)
            GLFW_KEY_LEFT -> changeDirection(Directions.LEFT)
            GLFW_KEY_RIGHT -> changeDirection(Directions.RIGHT)
        }
    }

}

object SnakeGame {
    private const val WINDOW_SIZE_WIDTH: Int = 500
    private const val WINDOW_SIZE_HEIGHT: Int = 500
    private const val side_size_px: Int = 25
    private var tickno: Int = 0
    private val rows: Int = floor(WINDOW_SIZE_WIDTH.toFloat()/side_size_px.toFloat()).toInt()
    private val cols = floor(WINDOW_SIZE_HEIGHT.toFloat()/side_size_px.toFloat()).toInt()
    private val snake: Snake = Snake(Grid(1,1), Directions.RIGHT)
    private val gameGrid: GameGrid = GameGrid(snake, rows, cols, side_size_px)
    private var window: Long = NULL
    private var keyCallback: GLFWKeyCallback? = null

    fun startGame() {
        //initialize GLFW
        init(WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)

        //Add half the number of objectives at the start
        while (gameGrid.objectiveList.size < gameGrid.MAX_OBJS/2) {
            gameGrid.addObjective()
        }

        //Draw it!
        while (!glfwWindowShouldClose(window) && snake.alive) {
            tickno += 1

            glfwPollEvents() // Get keypresses

            if (tickno % 10 == 0){ // Add new objective every 10 ticks
                gameGrid.addObjective()
            }

            gameGrid.moveSnake()
            gameGrid.redrawGrid()
            glfwSwapBuffers(window)
            //if we are dead fall right through, no sleep
            if (snake.alive) { Thread.sleep(200L) }
        }
        // Game over man! GAME OVER!
        gameGrid.GameOver()
        // Swap buffers one last time to update the grid with our dead snake
        glfwSwapBuffers(window)
        while (!glfwWindowShouldClose(window)){
            glfwPollEvents()
        }
    }

    private fun init(windowSizeW: Int, windowSizeH: Int) {
        if ( !glfwInit() ) {
            throw Exception("Failed to initialize GLFW.")
        }
        glfwDefaultWindowHints()
        //Do not allow resize
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        window = glfwCreateWindow(windowSizeW, windowSizeH, "KtSnake", 0, 0)
        if (window == NULL) {
            throw Exception("Failed to initialize window.")
        }
        glfwMakeContextCurrent(window)
        //Key callbacks
        //glfwSetKeyCallback(window, snake.glfwKeypressCallback)
        keyCallback = glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: kotlin.Long,
                                key: kotlin.Int,
                                scancode: kotlin.Int,
                                action: kotlin.Int,
                                mods: kotlin.Int) {

                snake.glfwKeypressCallback(key)
            }
        })

        // GL configuration comes AFTER we make the window our current context, otherwise errors
        GL.createCapabilities()
        glClearColor(0.0f,0.0f,0.0f,1.0f)
        glOrtho(0.0, WINDOW_SIZE_WIDTH.toDouble(), WINDOW_SIZE_HEIGHT.toDouble(), 0.0, -1.0, 1.0)
        glViewport(0, 0, WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)
        glfwShowWindow(window)
    }
}

fun main(args: Array<String>) {
    println("here we go again lol")
    SnakeGame.startGame()
}