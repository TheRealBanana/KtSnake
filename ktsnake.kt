import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.floor


data class ColorTuple(val r: Float, val g: Float, val b: Float)
data class Grid(val row: Int, val col: Int)
data class Point(val x: Int, val y: Int)


enum class GridElementTypes(val color: ColorTuple){
    DEFAULT_COLOR(ColorTuple(0.0f,0.0f,0.0f)),
    SNAKE_HEAD(ColorTuple(0.25882f,1.0f,0.0f)),
    SNAKE_TAIL(ColorTuple(0.15686f,0.58823f,0.0f)),
    OBJECTIVE(ColorTuple(0.0f,0.32941f,0.65098f)),
    DEAD_HEAD(ColorTuple(1.0f,0.0f,0.0f))
}

class GameGrid(rows: Int, cols: Int, side_size_px: Int) {
    val rows: Int = rows
    val cols: Int = cols
    val side_size_px: Int = side_size_px
    val activeGridElements = mutableMapOf<Grid, GridElement>()


    fun getGridElement(grid_index: Grid): GridElement? {
        when (activeGridElements.containsKey(grid_index)) {
            true -> return activeGridElements.getValue(grid_index)
            false -> return null
        }

    }
    fun createGridElement(element_type: GridElementTypes, grid_index: Grid): Boolean{
        if (activeGridElements.containsKey(grid_index)) {
            return false
        } else {
            val xcoord: Int = side_size_px * grid_index.row + side_size_px
            val ycoord: Int = side_size_px * grid_index.col + side_size_px
            val origin_coords = Point(xcoord, ycoord)
            val new_grid_element: GridElement = GridElement(element_type.color, origin_coords, side_size_px)
            activeGridElements[grid_index] =  new_grid_element
            return true
        }
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

}

class GridElement (type: ColorTuple, origin_coords: Point, size_px: Int) {
    val color: ColorTuple = type
    val origin_coords: Point = origin_coords
    val size_px: Int = size_px

    fun draw() {
        // Figure out our vertices
        val br: Point = origin_coords
        val tr: Point = Point(origin_coords.x, origin_coords.y - size_px)
        val tl: Point = Point(origin_coords.x - size_px, origin_coords.y - size_px)
        val bl: Point = Point(origin_coords.x - size_px, origin_coords.y)
        glColor3f(color.r, color.g, color.b)
        glBegin(GL_QUADS)
        glVertex2i(br.x, br.y)
        glVertex2i(tr.x, tr.y)
        glVertex2i(tl.x, tl.y)
        glVertex2i(bl.x, bl.y)
        glEnd()
    }
}


object SnakeGame {
    val WINDOW_SIZE_WIDTH: Int = 500
    val WINDOW_SIZE_HEIGHT: Int = 500
    val side_size_px: Int = 25
    val rows: Int = floor(WINDOW_SIZE_WIDTH.toFloat()/side_size_px.toFloat()).toInt()
    val cols = floor(WINDOW_SIZE_HEIGHT.toFloat()/side_size_px.toFloat()).toInt()
    var gameGrid: GameGrid = GameGrid(rows, cols, side_size_px)
    var window: Long = NULL

    fun startGame() {
        //initialize GLFW
        init(WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)

        //Create a single grid element in the upper-left corner (0,0)
        gameGrid.createGridElement(GridElementTypes.DEAD_HEAD, Grid(0,0))

        //Draw it!
        while (!glfwWindowShouldClose(window)) {
            gameGrid.redrawGrid()
            glfwSwapBuffers(window)
            glfwPollEvents()
            //Thread.sleep(1000L)
        }
    }

    fun init(windowSizeW: Int, windowSizeH: Int) {
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