import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import java.util.*
import kotlin.math.floor

fun randgrid(end: Int) = Random().nextInt(end)



data class ColorTuple(val r: Float, val g: Float, val b: Float)
data class Grid(val row: Int, val col: Int)
data class Point(val x: Int, val y: Int)
data class Vertices(val br: Point, private val side_size_px: Int) {
    val tr: Point = Point(br.x, br.y - side_size_px)
    val tl: Point = Point(br.x - side_size_px, br.y - side_size_px)
    val bl: Point = Point(br.x - side_size_px, br.y)
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

class GameGrid(private val snake: Snake, private val rows: Int, private val cols: Int, private val sidesizepx: Int) {
    val maxobjectives: Int = 10
    private val activeGridElements: MutableMap<Grid, GridElement> = mutableMapOf()
    val objectiveList: MutableList<Grid> = mutableListOf()

    fun addObjective() {
        if (objectiveList.size == maxobjectives) {
            deleteGridElement((objectiveList.elementAt(0)))
            objectiveList.removeAt(0)
        }
        //Create random new grid thats not already taken
        var newobjgrid = Grid(randgrid(rows), randgrid(cols))
        while (newobjgrid in objectiveList || newobjgrid in snake.snakegrids) {
            newobjgrid = Grid(randgrid(rows), randgrid(cols))
        }
        objectiveList.add(newobjgrid)
        createGridElement(GridElementTypes.OBJECTIVE, newobjgrid)
    }

    private fun createGridElement(element_type: GridElementTypes, grid_index: Grid): Boolean{
        if (activeGridElements.containsKey(grid_index)) {
            deleteGridElement(grid_index)
        }
        val xcoord: Int = sidesizepx * grid_index.row + sidesizepx
        val ycoord: Int = sidesizepx * grid_index.col + sidesizepx
        val origincoords = Point(xcoord, ycoord)
        val newgridelement = GridElement(element_type.color, origincoords, sidesizepx)
        activeGridElements[grid_index] =  newgridelement
        return true
    }
    private fun deleteGridElement(grid_index: Grid){
        if (activeGridElements.containsKey(grid_index)) {
            activeGridElements.remove(grid_index)
        } else {
            throw Exception("Tried to delete non-existent grid at index $grid_index")
        }
    }

    fun redrawGrid() {
        glClear(GL_COLOR_BUFFER_BIT)
        for ((_, grid_element) in activeGridElements) {
            grid_element.draw()
        }
    }

    fun moveSnake(): Boolean{
        val nextmove: Grid = snake.getMove()

        //Check if we hit the wall
        if (nextmove.row !in 0..rows){
            snake.alive = false
            println("Hit a wall, we ded fam X(")
            return false
        } else if (nextmove.col !in 0..cols){
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

    fun gameOver() {
        // We died, show score and wait for user to exit
        // Also color the snake head red so we know we ded
        createGridElement(GridElementTypes.DEAD_HEAD, snake.snakegrids[snake.snakegrids.size-1])
        redrawGrid()
        println("Final Score: ${snake.length-5}") //subtract initial snake size
    }

}

class GridElement (type: ColorTuple, origin_coords: Point, size_px: Int) {
    private val color: ColorTuple = type
    private val vertices: Vertices = Vertices(origin_coords, size_px)

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
    var currentgrid: Grid = start_grid
    private var direction: Directions = start_direction
    var length: Int = 5 //initial snake size
    val snakegrids: MutableList<Grid> = mutableListOf()

    init {
        this.snakegrids.add(start_grid)
    }

    fun getMove(dir: Directions = this.direction): Grid {
        return when (dir.dir) {
            "down" ->  Grid(currentgrid.row, currentgrid.col+1)
            "up" -> Grid(currentgrid.row, currentgrid.col-1)
            "right" -> Grid(currentgrid.row+1, currentgrid.col)
            else -> Grid(currentgrid.row-1, currentgrid.col) // Using else instead of "left" cause kotlin doesn't like the ambiguity.
        }
    }

    private fun changeDirection(newdir: Directions) {
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

    //I know scancode and mods are unused, need them anyway
    //In python we could use underscores in function declaration. Kotlin only allows that in lambda expressions.
    @Suppress("UNUSED_PARAMETER")
    fun glfwKeypressCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (action == GLFW_PRESS) {
            when (key) {
                GLFW_KEY_UP -> changeDirection(Directions.UP)
                GLFW_KEY_DOWN -> changeDirection(Directions.DOWN)
                GLFW_KEY_LEFT -> changeDirection(Directions.LEFT)
                GLFW_KEY_RIGHT -> changeDirection(Directions.RIGHT)
                GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true)
            }
        }
    }
}

object SnakeGame {
    private const val WINDOW_SIZE_WIDTH: Int = 500
    private const val WINDOW_SIZE_HEIGHT: Int = 500
    private const val side_size_px: Int = 15
    private var tickno: Int = 0
    private val rows: Int = floor(WINDOW_SIZE_WIDTH.toFloat()/side_size_px.toFloat()).toInt()
    private val cols: Int = floor(WINDOW_SIZE_HEIGHT.toFloat()/side_size_px.toFloat()).toInt()
    private val snake: Snake = Snake(Grid(0,0), Directions.RIGHT)
    private val gameGrid: GameGrid = GameGrid(snake, rows, cols, side_size_px)
    private var window: Long = NULL

    fun startGame() {
        //initialize GLFW
        init(WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)

        //Add half the number of objectives at the start
        while (gameGrid.objectiveList.size < gameGrid.maxobjectives/2) {
            gameGrid.addObjective()
        }

        //Draw it!
        while (!glfwWindowShouldClose(window) && snake.alive) {
            tickno += 1

            glfwPollEvents() // Get keypresses

            if (tickno % 30 == 0){ // Add new objective every 10 ticks
                gameGrid.addObjective()
            }

            gameGrid.moveSnake()
            gameGrid.redrawGrid()
            glfwSwapBuffers(window)
            //if we are dead fall right through, no sleep
            if (snake.alive) { Thread.sleep(100L) }
        }
        // Game over man! GAME OVER!
        gameGrid.gameOver()
        // Swap buffers one last time to update the grid with our dead snake
        glfwSwapBuffers(window)
        while (!glfwWindowShouldClose(window)){
            glfwPollEvents()
            Thread.sleep(50) //Dont let this while drive the CPU usage up too much
        }

        glfwDestroyWindow(window)
        glfwTerminate()
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
        glfwSetKeyCallback(window, snake::glfwKeypressCallback)

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
